package com.awpghost.user.listeners;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.services.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class UserCreationListener {

    private final UserService userService;

    @Autowired
    public UserCreationListener(UserService userService) {
        this.userService = userService;
    }

    @KafkaListener(topics = "user.verify.email", groupId = "user")
    public void verifyEmail(UserDto userDto) {
        log.info("Verify user email: {}", userDto.getEmail());
        userService.generateVerificationEmail(userDto.getEmail());
    }

    @KafkaListener(topics = "user.verify.mobileNo", groupId = "user")
    public void verifyMobileNo(UserDto userDto) {
        log.info("User created: {}", userDto.getEmail());
        userService.generateVerificationEmail(userDto.getMobileNo());
    }
}
