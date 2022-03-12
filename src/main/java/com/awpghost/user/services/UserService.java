package com.awpghost.user.services;

import com.awpghost.user.dto.requests.UserDto;
import com.awpghost.user.persistence.models.User;

public interface UserService {
    User createUser(UserDto userDto);
}
