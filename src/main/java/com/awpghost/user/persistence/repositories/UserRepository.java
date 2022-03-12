package com.awpghost.user.persistence.repositories;

import com.arangodb.springframework.repository.ArangoRepository;
import com.awpghost.user.persistence.models.User;

public interface UserRepository extends ArangoRepository<User, String> {

}
