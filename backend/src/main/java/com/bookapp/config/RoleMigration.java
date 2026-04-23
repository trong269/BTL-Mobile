package com.bookapp.config;

import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class RoleMigration implements CommandLineRunner {

    private final UserRepository userRepository;

    public RoleMigration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        List<User> users = userRepository.findAll();
        boolean hasChanges = false;

        for (User user : users) {
            String role = user.getRole();
            if (role != null && !role.equals(role.toUpperCase())) {
                user.setRole(role.toUpperCase());
                hasChanges = true;
            }
        }

        if (hasChanges) {
            userRepository.saveAll(users);
            System.out.println("✓ Role migration completed: normalized all roles to uppercase");
        }
    }
}
