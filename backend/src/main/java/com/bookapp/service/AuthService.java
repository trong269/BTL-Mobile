package com.bookapp.service;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import com.bookapp.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, EmailService emailService, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Đăng ký
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên tài khoản đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Gửi email chào mừng
        if (savedUser.getEmail() != null && !savedUser.getEmail().isEmpty()) {
            String subject = "Chào mừng bạn đến với Book App! 🎉";
            String text = "Xin chào " + savedUser.getUsername() + ",\n\n" +
                    "Cảm ơn bạn đã đăng ký tài khoản tại Book App.\n\n" +
                    "Trân trọng,\nĐội ngũ Book App";

            new Thread(() -> emailService.sendEmail(savedUser.getEmail(), subject, text)).start();
        }

        return savedUser;
    }

    // Đăng nhập -> trả JWT
    public String login(AuthRequest request) {
        User user = userRepository.findFirstByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu");
        }

        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public User getUserByToken(String token) {
        String userId = jwtUtil.extractUserId(token);
        User user = getUserById(userId);

        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy email này"));

        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetPasswordOtp(otp);
        user.setResetPasswordOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy email này"));

        if (user.getResetPasswordOtp() == null || !user.getResetPasswordOtp().equals(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ");
        }

        if (user.getResetPasswordOtpExpiry() == null || user.getResetPasswordOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận đã hết hạn");
        }

        // 🔥 QUAN TRỌNG: encode password
        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetPasswordOtp(null);
        user.setResetPasswordOtpExpiry(null);
        userRepository.save(user);
    }
}