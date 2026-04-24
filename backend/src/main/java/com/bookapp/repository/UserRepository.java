package com.bookapp.repository;

import com.bookapp.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findFirstByUsername(String username);

    Optional<User> findFirstByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, String id);

    boolean existsByEmailAndIdNot(String email, String id);
}