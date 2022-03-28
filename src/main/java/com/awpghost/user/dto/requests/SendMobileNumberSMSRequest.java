package com.awpghost.user.dto.requests;

import lombok.Builder;

@Builder
public class SendMobileNumberSMSRequest {
    private String mobileNo;
    private String content;
}
