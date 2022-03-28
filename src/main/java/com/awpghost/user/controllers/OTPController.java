package com.awpghost.user.controllers;

import com.awpghost.user.dto.responses.OTPResponse;
import com.awpghost.user.enums.VerificationMethod;
import com.awpghost.user.exceptions.TokenVerificationException;
import com.awpghost.user.exceptions.UserNotFoundException;
import com.awpghost.user.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/api/v1/otp")
@Log4j2
@RestController
public class OTPController {

    private final UserService userService;

    @Autowired
    public OTPController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Verify mobile number with OTP", description = "Verify mobile number with OTP")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful mobile number verification"),
    })
    @PostMapping("/mobileNo/otp")
    public Mono<OTPResponse> generateMobileNumberOTP(@RequestParam(name = "id") final String id,
                                                     @RequestParam(name = "mobileNo") final String mobileNo) {
        if (StringUtils.hasText(id)) {
            log.info("Generate OTP for User to mobile number with id: {}", id);
            return userService.getUserById(id).flatMap(user -> {
                if (user.isPresent()) {
                    return userService.generateVerificationMobileNo(mobileNo, VerificationMethod.OTP);
                } else {
                    return Mono.error(new UserNotFoundException("User not found"));
                }
            });
        } else if (StringUtils.hasText(mobileNo)) {
            log.info("Generate OTP for User with mobile number: {}", mobileNo);
            return userService.generateVerificationMobileNo(mobileNo, VerificationMethod.OTP);
        } else {
            return Mono.error(new TokenVerificationException("No token or otp provided"));
        }
    }

    @Operation(summary = "Verify email address with token", description = "Verify email address with token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "View email with verification link."),
    })
    @PostMapping("/email/token")
    public Mono<Boolean> generateEmailToken(@RequestParam(name = "id") final String id,
                                            @RequestParam(name = "email") final String email) {

        if (StringUtils.hasText(id)) {
            log.info("Generate OTP for User to mobile number with id: {}", id);
            return userService.getUserById(id).flatMap(user -> {
                if (user.isPresent()) {
                    return userService.generateVerificationEmail(user.get().getEmail(), VerificationMethod.TOKEN);
                } else {
                    return Mono.error(new UserNotFoundException("User not found"));
                }
            });
        } else if (StringUtils.hasText(email)) {
            log.info("Generate token for User with email address: {}", email);
            return userService.generateVerificationEmail(email, VerificationMethod.TOKEN);
        } else {
            return Mono.error(new TokenVerificationException("No token or otp provided"));
        }
    }
}
