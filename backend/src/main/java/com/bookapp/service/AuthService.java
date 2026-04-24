package com.bookapp.service;

import com.bookapp.dto.AuthRequest;
import com.bookapp.dto.RegisterRequest;
import com.bookapp.dto.AuthResponse;
import com.bookapp.model.User;
import com.bookapp.repository.UserRepository;
import com.bookapp.repository.NotificationRepository;
import com.bookapp.model.Notification;
import com.bookapp.util.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final NotificationRepository notificationRepository;

    public AuthService(UserRepository userRepository, EmailService emailService, JwtUtil jwtUtil, NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
        this.notificationRepository = notificationRepository;
    }

    // Đăng ký
    public User register(RegisterRequest request) {
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());
        String password = normalize(request.getPassword());

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin đăng ký");
        }

        if (password.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 6 ký tự");
        }

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("USER");
        user.setPlan("Cơ bản");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Gửi email
        if (savedUser.getEmail() != null && !savedUser.getEmail().isEmpty()) {
            String subject = "Chào mừng bạn đến với Book App! 🎉";
            String text = "Xin chào " + savedUser.getUsername() + ",\n\n" +
                    "Cảm ơn bạn đã đăng ký tài khoản tại Book App.\n\n" +
                    "Trân trọng,\nĐội ngũ Book App";

            new Thread(() -> emailService.sendEmail(savedUser.getEmail(), subject, text)).start();
        }

        // Notification
        Notification welcomeNotification = new Notification(
                savedUser.getId(),
                "Chào mừng đến với BookApp 🎉",
                "Khám phá hàng ngàn cuốn sách hay và bắt đầu hành trình đọc sách của bạn ngay hôm nay!"
        );
        notificationRepository.save(welcomeNotification);

        return savedUser;
    }

    // Login thường
    public String login(AuthRequest request) {

        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu tài khoản hoặc mật khẩu");
        }

        List<User> candidates = userRepository.findAllByUsernameIgnoreCase(username);
        if (candidates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu");
        }

        List<User> matchedUsers = candidates.stream()
                .filter(u -> matchesPassword(u, password))
                .toList();

        if (matchedUsers.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai tài khoản hoặc mật khẩu");
        }

        User user = matchedUsers.stream()
                .max(
                        Comparator
                                .comparing((User u) -> u.getUpdatedAt() != null ? u.getUpdatedAt() : LocalDateTime.MIN)
                                .thenComparing(u -> u.getCreatedAt() != null ? u.getCreatedAt() : LocalDateTime.MIN)
                )
                .orElse(matchedUsers.get(0));

        user.setLastActiveAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getRole());
    }

    // Login Google
    public AuthResponse googleLogin(String idToken) {
        try {
            com.google.firebase.auth.FirebaseToken decodedToken =
                    com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);

            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            if (email == null || email.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản Google không có email");
            }

            User user = userRepository.findFirstByEmail(email).orElse(null);

            if (user == null) {
                user = new User();
                user.setUsername(name != null ? name : email.split("@")[0]);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                user.setRole("USER");
                user.setCreatedAt(LocalDateTime.now());
                user.setLastActiveAt(LocalDateTime.now());
                user = userRepository.save(user);

                Notification welcomeNotification = new Notification(
                        user.getId(),
                        "Chào mừng đến với BookApp 🎉",
                        "Khám phá hàng ngàn cuốn sách hay và bắt đầu hành trình đọc sách của bạn ngay hôm nay!"
                );
                notificationRepository.save(welcomeNotification);
            } else {
                user.setLastActiveAt(LocalDateTime.now());
                userRepository.save(user);
            }

            String token = jwtUtil.generateToken(user.getId(), user.getRole());

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setUser(user);
            response.setRole(user.getRole());

            return response;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Xác thực Google thất bại: " + e.getMessage());
        }
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
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy email này"));

        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        user.setResetPasswordOtp(otp);
        user.setResetPasswordOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        new Thread(() -> emailService.sendOtpEmail(user.getEmail(), otp)).start();
    }

    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findFirstByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy email này"));

        if (user.getResetPasswordOtp() == null || !user.getResetPasswordOtp().equals(otp)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận không hợp lệ");
        }

        if (user.getResetPasswordOtpExpiry() == null || user.getResetPasswordOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác nhận đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));

        user.setResetPasswordOtp(null);
        user.setResetPasswordOtpExpiry(null);
        userRepository.save(user);
    }

    private boolean matchesPassword(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcryptHash(storedPassword)) {
            try {
                return passwordEncoder.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        return storedPassword.equals(rawPassword);
    }

    private boolean looksLikeBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}