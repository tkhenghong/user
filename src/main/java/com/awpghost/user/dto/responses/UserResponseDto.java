package com.awpghost.user.dto.responses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address1;
    private String address2;
    private String city;
    private String state;
    private String zip;
    private String location;
}
