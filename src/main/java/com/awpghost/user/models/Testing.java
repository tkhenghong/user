package com.awpghost.user.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class Testing {
    private String name;
    private String email;
    private String password;
}
