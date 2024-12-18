package com.example.carsharing.service;

import com.example.carsharing.controller.WebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String text) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
    // Метод для отправки письма с ссылкой для сброса пароля
    public void sendPasswordResetEmail(String to, String resetLink) {
        String subject = "Password Reset Request";
        String text = "<p>To reset your password, click the link below:</p>" +
                "<a href=\"" + resetLink + "\">Reset Password</a>";
        sendEmail(to, subject, text);
    }
}
