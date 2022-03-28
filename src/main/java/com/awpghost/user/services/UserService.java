package com.awpghost.user.services;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.dto.responses.OTPResponse;
import com.awpghost.user.enums.VerificationMethod;
import com.awpghost.user.persistence.models.User;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UserService {
    Mono<User> createUser(UserDto userDto);

    Mono<Optional<User>> getUserById(String id);

    Mono<Optional<User>> getUserByEmail(String email);

    Mono<Optional<User>> getUserByMobileNo(String mobileNo);

    Mono<Boolean> generateVerificationEmail(String email, VerificationMethod verificationMethod);

    Mono<OTPResponse> generateVerificationMobileNo(String mobileNo, VerificationMethod verificationMethod);

    Mono<Boolean> verifyEmail(String token, VerificationMethod verificationMethod);

    Mono<Boolean> verifyMobileNo(String otp, VerificationMethod verificationMethod);
}
