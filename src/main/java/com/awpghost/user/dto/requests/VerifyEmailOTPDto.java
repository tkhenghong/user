package com.awpghost.user.dto.requests;

import lombok.Data;

@Data
public class VerifyEmailOTPDto {
    private String email;
    private String otp;
}
