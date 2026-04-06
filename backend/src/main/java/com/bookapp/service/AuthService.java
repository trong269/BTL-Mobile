package com.bookapp.service;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Đăng ký
    public User register(RegisterRequest request) {

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // (sau này mã hóa)
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        return userRepository.save(user);
    }

    // Đăng nhập
    public User login(AuthRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu");
        }

        return user;
    }
}