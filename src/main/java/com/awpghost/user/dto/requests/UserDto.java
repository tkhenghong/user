package com.awpghost.user.dto.requests;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class UserDto {
    private String firstName;
    private String lastName;
    private String email;
    private String nationality;
    private String mobileNo;
}
