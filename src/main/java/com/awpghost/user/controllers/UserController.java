package com.awpghost.user.controllers;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.persistence.models.User;
import com.awpghost.user.services.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PostMapping("/register")
    public String registerUser(@Valid UserDto userDto) {
        User user = userService.createUser(userDto);
        return "User created with id: " + user.getId();
    }
}
