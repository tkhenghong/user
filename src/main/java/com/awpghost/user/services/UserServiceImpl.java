package com.awpghost.user.services;

import com.awpghost.user.dto.requests.SendEmailRequest;
import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.exceptions.UserNotFoundException;
import com.awpghost.user.persistence.models.User;
import com.awpghost.user.persistence.models.VerificationToken;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
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

    @Autowired
    public UserServiceImpl(UserRepository userRepository,
                           ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
                           @Value("${token.verify.timeout}") long TOKEN_EXPIRATION_TIME,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper,
                           Environment environment) {
        this.userRepository = userRepository;
        this.reactiveValueOps = reactiveRedisTemplate.opsForValue();
        this.TOKEN_EXPIRATION_TIME = TOKEN_EXPIRATION_TIME;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new AtomicReference<>(objectMapper);
        this.environment = environment;
    }

    @Override
    public Mono<User> createUser(UserDto userDto) {

        User user = User.builder()
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .email(userDto.getEmail())
                .location(userDto.getNationality())
                .mobileNo(userDto.getMobileNo())
                .build();

        return Mono.just(userRepository.save(user));
    }

    @Override
    public Mono<Optional<User>> getUserByEmail(String email) {
        return Mono.just(userRepository.findByEmail(email));
    }

    @Override
    public Mono<Optional<User>> getUserByMobileNo(String mobileNo) {
        return Mono.just(userRepository.findByMobileNo(mobileNo));
    }

    @Override
    public Mono<Boolean> generateVerificationEmail(String email) {
        log.info("Generate verification email: {}", email);
        return Mono.fromCallable(() -> {
                    Optional<User> userOptional = userRepository.findByEmail(email);
                    if (userOptional.isPresent()) {
                        return Mono.just(userOptional.get());
                    } else {
                        return Mono.error(new UserNotFoundException("User not found"));
                    }
                })
                .cast(User.class)
                .flatMap(user -> {
                    String token = UUID.randomUUID().toString();

                    VerificationToken verificationToken = VerificationToken.builder()
                            .userId(user.getId())
                            .token(token)
                            .build();
                    return convertMonoObjectToString(verificationToken).handle((verificationTokenString, sink) -> {
                        reactiveValueOps.set(verificationToken.getUserId(), verificationTokenString,
                                Duration.ofMillis(TOKEN_EXPIRATION_TIME));

                        sink.next(token);
                    }).cast(String.class);
                })
                .flatMap(token -> {
                    String link = generateLink("email", token);
                    SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                            .receiverList(List.of(email))
                            .emailSubject("Verify your email")
                            .emailContent(link)
                            .build();

                    return convertMonoObjectToString(sendEmailRequest).handle((sendEmailRequestString, sink) -> {
                        kafkaTemplate.send("send-email", sendEmailRequestString);
                        sink.next(true);
                    }).onErrorReturn(false);
                })
                .cast(Boolean.class)
                .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> generateVerificationMobileNo(String mobileNo) {
        log.info("Generate verification mobile no: {}", mobileNo);
        return Mono.fromCallable(() -> {
                    Optional<User> userOptional = userRepository.findByMobileNo(mobileNo);

                    if (userOptional.isPresent()) {
                        return Mono.just(userOptional.get());
                    } else {
                        return Mono.error(new UserNotFoundException("User not found"));
                    }
                })
                .cast(User.class)
                .flatMap(user -> {
                    String token = UUID.randomUUID().toString();

                    generateLink("mobileNo", token);

                    VerificationToken verificationToken = VerificationToken.builder()
                            .userId(user.getId())
                            .token(token)
                            .build();
                    return convertMonoObjectToString(verificationToken).handle((verificationTokenString, sink) -> {
                        sink.next(reactiveValueOps.set(verificationToken.getUserId(), verificationTokenString,
                                Duration.ofMillis(TOKEN_EXPIRATION_TIME)));
                    }).cast(Boolean.class);
                })
                .onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> verifyMobileNoToken(String token) {
        return Mono.fromCallable(() -> true).onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> verifyMobileNoOTP(String otp) {
        return Mono.fromCallable(() -> true).onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> verifyEmailToken(String token) {
        return Mono.fromCallable(() -> true).onErrorReturn(false);
    }

    @Override
    public Mono<Boolean> verifyEmailOTP(String otp) {
        return Mono.fromCallable(() -> true).onErrorReturn(false);
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
