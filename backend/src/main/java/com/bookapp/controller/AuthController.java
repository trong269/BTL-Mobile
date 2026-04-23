package com.bookapp.controller;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.AuthResponse;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.model.User;
import com.bookapp.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        String token = authService.login(request);

        User user = authService.getUserByToken(token);

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUser(user);
        response.setRole(user.getRole());

        return response;
    }
}