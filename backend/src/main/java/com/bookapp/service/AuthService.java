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
    private final EmailService emailService;

    public AuthService(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
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
        user.setPassword(request.getPassword()); // (sau này mã hóa)
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Gửi email chào mừng
        if (savedUser.getEmail() != null && !savedUser.getEmail().isEmpty()) {
            String subject = "Chào mừng bạn đến với Book App! 🎉";
            String text = "Xin chào " + savedUser.getUsername() + ",\n\n" +
                          "Cảm ơn bạn đã đăng ký tài khoản tại Book App. Chúng tôi rất vui mừng được đồng hành cùng bạn trên hành trình khám phá tri thức.\n\n" +
                          "Hãy bắt đầu đọc cuốn sách đầu tiên của bạn ngay hôm nay nhé!\n\n" +
                          "Trân trọng,\nĐội ngũ Book App";
            // Gửi bất đồng bộ để không làm chậm luồng đăng ký
            new Thread(() -> emailService.sendEmail(savedUser.getEmail(), subject, text)).start();
        }

        return savedUser;
    }

    // Đăng nhập
    public User login(AuthRequest request) {

        User user = userRepository.findFirstByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu");
        }

        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy email này"));

        // Generate 6-digit OTP
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

        user.setPassword(newPassword);
        user.setResetPasswordOtp(null);
        user.setResetPasswordOtpExpiry(null);
        userRepository.save(user);
    }
}