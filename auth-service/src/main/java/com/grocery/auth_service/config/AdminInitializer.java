package com.grocery.auth_service.config;

import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initAdmin(){
        String adminEmail = "admin@grocery.com";
        if (!userRepository.existsByEmail(adminEmail)){
            User user=User.builder()
                    .name("Super Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(User.Role.ADMIN)
                    .status(User.UserStatus.ACTIVE)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("Initial admin created: {}", adminEmail);
        }
    }
}
