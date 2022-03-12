package com.awpghost.user.services;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.persistence.models.User;
import com.awpghost.user.persistence.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User createUser(UserDto userDto) {

        User user = User.builder()
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .email(userDto.getEmail())
                .nationality(userDto.getNationality())
                .mobileNo(userDto.getMobileNo())
                .build();

        return userRepository.save(user);
    }
}
