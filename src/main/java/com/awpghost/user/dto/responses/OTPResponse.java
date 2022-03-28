package com.awpghost.user.dto.responses;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public class OTPResponse {
    private String otp;
    private ZonedDateTime expiry;
}
