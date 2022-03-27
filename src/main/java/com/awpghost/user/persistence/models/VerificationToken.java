package com.awpghost.user.persistence.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VerificationToken {
    private String userId;
    private String token;
}
