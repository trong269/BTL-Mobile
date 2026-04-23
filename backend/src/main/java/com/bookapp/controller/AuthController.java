package com.bookapp.controller;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.AuthResponse;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.model.User;
import com.bookapp.service.AuthService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

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

    @PostMapping("/forgot-password")
    public org.springframework.http.ResponseEntity<?> forgotPassword(@RequestBody com.bookapp.dto.ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Mã xác nhận đã được gửi đến email của bạn");
        return org.springframework.http.ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public org.springframework.http.ResponseEntity<?> resetPassword(@RequestBody com.bookapp.dto.ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        Map<String, String> response = new HashMap<>();
        response.put("message", "Đổi mật khẩu thành công");
        return org.springframework.http.ResponseEntity.ok(response);
    }
}