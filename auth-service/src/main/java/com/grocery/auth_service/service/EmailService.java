package com.grocery.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final Environment env;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine, Environment env) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.env = env;
    }

    @Async
    public void sendPasswordResetEmail(String email, String rawToken) {
        try {
            Context context = new Context();
            context.setVariable("token",rawToken);
            context.setVariable("expiryTime", LocalDateTime.now().plusMinutes(60).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));
            context.setVariable("appUrl", env.getProperty("app.url", "https://yourapp.com"));
            String htmlContent = templateEngine.process("email/password-reset", context);
            SimpleMailMessage message=new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Password Reset - GroceryMart");
            message.setFrom(env.getProperty("spring.mail.username", "noreply@yourapp.com"));
            message.setText(htmlContent);
            mailSender.send(message);
            log.info("Password reset email sent to: {}", email);

        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", email, e.getMessage());
        }
    }
}

