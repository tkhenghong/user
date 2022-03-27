package com.awpghost.user.controllers;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.dto.responses.UserResponseDto;
import com.awpghost.user.exceptions.TokenVerificationException;
import com.awpghost.user.exceptions.UserNotFoundException;
import com.awpghost.user.persistence.models.User;
import com.awpghost.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/user")
@Log4j2
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
    })
    @PostMapping("/register")
    public Mono<UserResponseDto> registerUser(@Valid UserDto userDto) {
        return userService.createUser(userDto).map(this::mapUserToUserResponseDto);
    }

    @Operation(summary = "Verify mobile number with token", description = "Verify mobile number with token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful mobile number verification"),
    })
    @GetMapping("/verify-mobileNo")
    public Mono<Boolean> mobileNumberVerification(@RequestParam(name = "token") final String token, @RequestParam(name = "otp") final String otp) {
        if (StringUtils.hasText(token)) {
            log.info("Verify user account mobile number with token: {}", token);
            return userService.verifyMobileNoToken(token);
        } else if (StringUtils.hasText(otp)) {
            log.info("Verify user account mobile number with otp: {}", otp);
            return userService.verifyMobileNoOTP(otp);
        } else {
            return Mono.error(new TokenVerificationException("No token or otp provided"));
        }
    }

    @Operation(summary = "Verify email address with token/OTP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful email address verification"),
    })
    @GetMapping("/verify-email")
    public Mono<Boolean> emailVerification(@RequestParam(name = "token") final String token, @RequestParam(name = "otp") final String otp) {
        if (StringUtils.hasText(token)) {
            log.info("Verify user account email address with token: {}", token);
            return userService.verifyEmailToken(token);
        } else if (StringUtils.hasText(otp)) {
            log.info("Verify user account email address with otp: {}", otp);
            return userService.verifyEmailOTP(otp);
        } else {
            return Mono.error(new TokenVerificationException("No token or otp provided"));
        }
    }

    @Operation(summary = "Get a user using email address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/email}")
    public Mono<UserResponseDto> getUserByEmail(@RequestParam("email") String email) {
        return userService.getUserByEmail(email).flatMap(user ->
                user.isEmpty() ? Mono.error(new UserNotFoundException("User not found")) : Mono.just(mapUserToUserResponseDto(user.get()))
        );
    }

    @Operation(summary = "Get a user using mobile number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/mobileNo")
    public Mono<UserResponseDto> getUserByMobileNo(@RequestParam("mobileNo") String mobileNo) {
        return userService.getUserByMobileNo(mobileNo).flatMap(user ->
                user.isEmpty() ? Mono.error(new UserNotFoundException("User not found")) : Mono.just(mapUserToUserResponseDto(user.get()))
        );
    }

    private UserResponseDto mapUserToUserResponseDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .address1(user.getAddress1())
                .address2(user.getAddress2())
                .city(user.getCity())
                .state(user.getState())
                .zip(user.getZip())
                .location(user.getLocation())
                .build();
    }
}
