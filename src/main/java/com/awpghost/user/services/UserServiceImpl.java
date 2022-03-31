package com.awpghost.user.services;

import com.awpghost.user.dto.requests.SendEmailRequest;
import com.awpghost.user.dto.requests.SendMobileNumberSMSRequest;
import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.dto.responses.OTPResponse;
import com.awpghost.user.enums.VerificationMethod;
import com.awpghost.user.enums.VerificationType;
import com.awpghost.user.exceptions.UserNotFoundException;
import com.awpghost.user.persistence.models.User;
import com.awpghost.user.persistence.repositories.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Transactional
@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    private final ReactiveValueOperations<String, String> reactiveValueOps;

    private final long TOKEN_EXPIRATION_TIME;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final AtomicReference<ObjectMapper> objectMapper;

    private final Environment environment;

    private final Integer TOKEN_LENGTH;

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                           @Value("${otp.length}") Integer TOKEN_LENGTH,
                           @Value("${token.verify.timeout}") long TOKEN_EXPIRATION_TIME,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           Environment environment) {
        this.userRepository = userRepository;
        this.reactiveValueOps = reactiveRedisTemplate.opsForValue();
        this.TOKEN_LENGTH = TOKEN_LENGTH;
        this.TOKEN_EXPIRATION_TIME = TOKEN_EXPIRATION_TIME;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new AtomicReference<>(objectMapper);
        this.environment = environment;
    }

    @Override
    public Mono<User> createUser(UserDto userDto) {
        return Mono.fromCallable(() -> {
            User user = User.builder()
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName())
                    .email(userDto.getEmail())
                    .location(userDto.getNationality())
                    .mobileNo(userDto.getMobileNo())
                    .build();

            return userRepository.save(user);
        });
    }

    @Override
    public Mono<Optional<User>> getUserById(String id) {
        return Mono.fromCallable(() -> userRepository.findById(id));
    }

    @Override
    public Mono<Optional<User>> getUserByEmail(String email) {
        return Mono.fromCallable(() -> userRepository.findByEmail(email));
    }

    @Override
    public Mono<Optional<User>> getUserByMobileNo(String mobileNo) {
        return Mono.fromCallable(() -> userRepository.findByMobileNo(mobileNo));
    }

    @Override
    public Mono<Boolean> generateVerificationEmail(String email, VerificationMethod verificationMethod) {
        return Mono.fromCallable(() -> {
                    log.info("Generate verification email: {}", email);

                    Optional<User> userOptional = userRepository.findByEmail(email);

                    if (userOptional.isPresent()) {
                        return Mono.just(userOptional.get());
                    } else {
                        return Mono.error(new UserNotFoundException("User not found"));
                    }
                })
                .cast(User.class)
                .flatMap(user -> {
                    if (verificationMethod.equals(VerificationMethod.TOKEN)) {
                        String token = UUID.randomUUID().toString();

                        return reactiveValueOps.set(user.getId(), token,
                                Duration.ofMillis(TOKEN_EXPIRATION_TIME)).flatMap(response -> Mono.just(token));
                    }
                    return Mono.error(new UnsupportedOperationException("Verification method not supported"));
                })
                .flatMap(token -> {
                    // Generate verification link
                    String link = generateLink("email", token);
                    SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                            .receiverList(List.of(email))
                            .emailSubject("Verify your email")
                            .emailContent("Click the link to verify your email: " + link)
                            .build();

                    // Send to Email microservice
                    return convertMonoObjectToString(sendEmailRequest).handle((sendEmailRequestString, sink) -> {
                        kafkaTemplate.send("email.send", sendEmailRequestString);
                        sink.next(true);
                    }).onErrorReturn(false);
                })
                .cast(Boolean.class)
                .onErrorReturn(false);
    }

    @Override
    public Mono<OTPResponse> generateVerificationMobileNo(String mobileNo, VerificationMethod verificationMethod) {
        return Mono.fromCallable(() -> {
                    log.info("Generate verification mobile no: {}", mobileNo);
                    Optional<User> userOptional = userRepository.findByMobileNo(mobileNo);
                    if (userOptional.isPresent()) {
                        return Mono.just(userOptional.get());
                    } else {
                        return Mono.error(new UserNotFoundException("User not found"));
                    }
                })
                .cast(User.class)
                .flatMap(user -> {
                    // Generate verification code (OTP)
                    Random random = new Random();
                    int otpNumber = random.nextInt((int) (Math.pow(10, TOKEN_LENGTH)));

                    long otpExpiryDuration = Duration.ofMillis(TOKEN_EXPIRATION_TIME).toMinutes();
                    ZonedDateTime expiryTime = ZonedDateTime.now().plusMinutes(otpExpiryDuration);
                    String content = "Your OTP is " + otpNumber + ". Please use this within " + otpExpiryDuration + " minutes. ";

                    SendMobileNumberSMSRequest sendMobileNumberSMSRequest = SendMobileNumberSMSRequest.builder()
                            .mobileNo(user.getMobileNo())
                            .content(content)
                            .build();

                    return convertMonoObjectToString(sendMobileNumberSMSRequest)
                            .handle((sendMobileNumberSMSRequestString, sink) -> {
                                reactiveValueOps.set(user.getId(), String.valueOf(otpNumber), Duration.ofMillis(TOKEN_EXPIRATION_TIME));
                                kafkaTemplate.send("mobile.send", sendMobileNumberSMSRequestString);

                                sink.next(OTPResponse.builder().otp(String.valueOf(otpNumber)).expiry(expiryTime).build());
                            });
                }).cast(OTPResponse.class);
    }

    @Override
    public Mono<Boolean> verifyEmail(String token, VerificationMethod verificationMethod) {
        return verify(token, VerificationType.EMAIL, verificationMethod);
    }

    @Override
    public Mono<Boolean> verifyMobileNo(String token, VerificationMethod verificationMethod) {
        return verify(token, VerificationType.MOBILE_NUMBER, verificationMethod);
    }

    private Mono<Boolean> verify(String token, VerificationType verificationType, VerificationMethod verificationMethod) {
        return Mono.fromCallable(() -> {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
                    UserDetails userDetails = (UserDetails) usernamePasswordAuthenticationToken.getPrincipal();

                    Optional<User> userOptional = userRepository.findById(userDetails.getUsername());
                    if (userOptional.isPresent()) {
                        return userOptional.get();
                    } else {
                        throw new UserNotFoundException("User not found");
                    }
                })
                .cast(User.class)
                .flatMap(user -> reactiveValueOps.get(user.getId())).cast(String.class)
                .flatMap(existingOtp -> Mono.just(token.equals(existingOtp)))
                .onErrorReturn(false);
    }

    private String generateLink(String api, String token) {
        String link = String.format("https://%s:%s/auth/verify-%s?token=%s",
                environment.getProperty("java.rmi.server.hostname"),
                environment.getProperty("local.server.port"),
                api,
                token);
        log.info("Link: {}", link);
        return link;
    }

    private Mono<String> convertMonoObjectToString(Object object) {
        return Mono.create((monoSink) -> {
            try {
                monoSink.success(objectMapper.get().writeValueAsString(object));
            } catch (JsonProcessingException e) {
                monoSink.error(e);
            }
        }).cast(String.class);
    }
}
