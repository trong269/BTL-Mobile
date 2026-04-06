package com.bookapp.controller;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.model.User;
import com.bookapp.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Đăng ký
    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    // Đăng nhập
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody AuthRequest request) {

        User user = authService.login(request);

        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("role", user.getRole());

        return response;
    }
}