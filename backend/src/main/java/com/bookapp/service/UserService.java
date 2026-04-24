package com.bookapp.service;

import com.bookapp.dto.ChangePasswordRequest;
import com.bookapp.dto.CreateUserRequest;
import com.bookapp.dto.UpdateProfileRequest;
import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User getById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User create(CreateUserRequest request) {
        String username = valueOrEmpty(request.getUsername());
        String email = valueOrEmpty(request.getEmail());
        String password = valueOrEmpty(request.getPassword());

        validateRequiredFields(username, email);

        if (password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }

        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(valueOrEmpty(request.getFullName()));
        user.setAvatar(valueOrEmpty(request.getAvatar()));
        user.setRole(valueOrDefault(request.getRole(), "USER"));
        user.setPlan(valueOrDefault(request.getPlan(), "Cơ bản"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    public User updateProfile(String userId, UpdateProfileRequest request) {
        User user = getById(userId);

        String username = valueOrEmpty(request.getUsername());
        String email = valueOrEmpty(request.getEmail());

        validateRequiredFields(username, email);

        if (userRepository.existsByUsernameAndIdNot(username, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        if (userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        String password = valueOrEmpty(request.getPassword());
        if (!password.isEmpty() && password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        }

        user.setUsername(username);
        user.setEmail(email);
        if (!password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }
        user.setFullName(valueOrEmpty(request.getFullName()));
        user.setAvatar(valueOrEmpty(request.getAvatar()));
        user.setRole(valueOrDefault(request.getRole(), user.getRole() == null ? "USER" : user.getRole()));
        user.setPlan(valueOrDefault(request.getPlan(), user.getPlan() == null ? "Cơ bản" : user.getPlan()));
        user.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    private void validateRequiredFields(String username, String email) {
        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        if (email.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
    }

    public void delete(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(userId);
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = getById(userId);

        String currentPassword = request.getCurrentPassword();
        String newPassword = request.getNewPassword();

        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is required");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password is required");
        }

        if (newPassword.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be at least 6 characters");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void updateFcmToken(String userId, String fcmToken) {
        User user = getById(userId);
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    public void updateLastActiveAt(String userId) {
        User user = getById(userId);
        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String valueOrDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value);
        return normalized.isEmpty() ? fallback : normalized;
    }
}
