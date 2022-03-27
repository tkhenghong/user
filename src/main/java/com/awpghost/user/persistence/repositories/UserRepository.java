package com.awpghost.user.persistence.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.awpghost.user.persistence.models.User;

import java.util.Optional;

public interface UserRepository extends ArangoRepository<User, String> {
    Optional<User> findByEmail(String email);

    Optional<User> findByMobileNo(String mobileNo);
}
