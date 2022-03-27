package com.awpghost.user.services;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.persistence.models.User;
import reactor.core.publisher.Mono;

import java.util.Optional;

public interface UserService {
    Mono<User> createUser(UserDto userDto);

    Mono<Optional<User>> getUserByEmail(String email);

    Mono<Optional<User>> getUserByMobileNo(String mobileNo);

    Mono<Boolean> generateVerificationEmail(String email);

    Mono<Boolean> generateVerificationMobileNo(String mobileNo);

    Mono<Boolean> verifyMobileNoToken(String token);

    Mono<Boolean> verifyMobileNoOTP(String otp);

    Mono<Boolean> verifyEmailToken(String token);

    Mono<Boolean> verifyEmailOTP(String otp);

}
