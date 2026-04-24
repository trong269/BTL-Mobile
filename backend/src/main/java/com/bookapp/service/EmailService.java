package com.bookapp.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String text) {
        if (to == null || to.isEmpty()) {
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            message.setFrom("no-reply@bookapp.com");
            
            mailSender.send(message);
            System.out.println("Email reminder sent to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
        }
    }

    public void sendOtpEmail(String to, String otp) {
        if (to == null || to.isEmpty()) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Mã xác nhận khôi phục mật khẩu - Book App");
            message.setText("Xin chào,\n\n" +
                    "Bạn đã yêu cầu khôi phục mật khẩu. Mã OTP của bạn là: " + otp + "\n\n" +
                    "Mã này sẽ hết hạn trong 10 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n" +
                    "Trân trọng,\nĐội ngũ Book App");
            message.setFrom("no-reply@bookapp.com");

            mailSender.send(message);
            System.out.println("OTP email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email to " + to + ": " + e.getMessage());
        }
    }
}
