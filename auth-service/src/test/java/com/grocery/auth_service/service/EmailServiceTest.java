package com.grocery.auth_service.service;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private SpringTemplateEngine templateEngine;

    @Mock
    private Environment environment;

    @InjectMocks
    private EmailService emailService;


    private static final String EMAIL="user@example.com";
    private static final String TOKEN="reset-token";


    @BeforeEach
    void setUp(){
        when(environment.getProperty("app.url", "https://yourapp.com"))
                .thenReturn("https://grocerymart.com");
        when(environment.getProperty("spring.mail.username", "noreply@yourapp.com"))
                .thenReturn("noreply@grocerymart.com");

        when(templateEngine.process(eq("email/password-reset"), any(Context.class)))
                .thenReturn("<html>Password reset</html>");
    }

    @Test
    @DisplayName("sendPasswordResetEmail: should send password reset email successfully")
    void sendPasswordResetEmail_success(){
        emailService.sendPasswordResetEmail(EMAIL,TOKEN);
        ArgumentCaptor<SimpleMailMessage> captor=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender, times(1)).send(captor.capture());
        SimpleMailMessage sentMessage=captor.getValue();
        assertThat(sentMessage.getTo()).containsExactly(EMAIL);
        assertThat(sentMessage.getSubject()).isEqualTo("Password Reset - GroceryMart");
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@grocerymart.com");
        assertThat(sentMessage.getText()).isEqualTo("<html>Password reset</html>");
        verify(templateEngine).process(eq("email/password-reset"),any(Context.class));
    }

    @Test
    @DisplayName("sendPasswordResetEmail: should not throw when mail sending fails")
    void sendPasswordResetEmail_whenMailSenderThrows_shouldHandleGracefully(){
        doThrow(new RuntimeException("Mail server error"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));
        emailService.sendPasswordResetEmail(EMAIL, TOKEN);

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
        verify(templateEngine).process(eq("email/password-reset"), any(Context.class));
    }
}
