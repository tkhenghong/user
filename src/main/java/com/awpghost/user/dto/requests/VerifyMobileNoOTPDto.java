package com.awpghost.user.dto.requests;

import lombok.Data;

@Data
public class VerifyMobileNoOTPDto {
    private String mobileNo;
    private String otp;
}
