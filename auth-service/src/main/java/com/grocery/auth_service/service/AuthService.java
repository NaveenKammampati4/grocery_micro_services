package com.grocery.auth_service.service;

import com.grocery.auth_service.client.UserServiceClient;
import com.grocery.auth_service.dto.request.InitProfileRequest;
import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.PasswordMismatchException;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.security.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    public final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Logger log=LoggerFactory.getLogger(AuthService.class);

    @Value("${app.security.default.registration.role:CUSTOMER}")
    private String defaultRegistrationRole;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserServiceClient userServiceClient, KafkaTemplate<String, Object> kafkaTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.kafkaTemplate = kafkaTemplate;
    }

    public User register(RegisterRequest request){
        log.info("Register request received for email: {}", request.getEmail());
        validateRegistrationRequest(request);
        validatePassword(request.getPassword());
        if (userRepository.existsByEmail(request.getEmail())){
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            throw new RuntimeException("Email already exists");
        }
        User user=User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
//                .role(resolveUserRole(request))
                .role(User.Role.CUSTOMER)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        userServiceClient.initProfile(savedUser.getId(), new InitProfileRequest(savedUser.getEmail()));

//        sendUserEventAsync(savedUser.getId(),savedUser.getEmail());
//        // Send welcome email asynchronously
//        CompletableFuture.runAsync(() ->
//                emailService.sendWelcomeEmail(savedUser));
        return savedUser;
    }

//    private void sendUserEventAsync(Long userId, String email){
//        CompletableFuture.runAsync(()->{
//            try {
//                kafkaTemplate.send("user-events", new UserCreatedEvent(userId, email));
//                log.debug("UserCreatedEvent sent for userId: {}", userId);
//            } catch (Exception e) {
//                log.warn("Failed to send UserCreatedEvent for userId {} (profile already created via sync): {}",
//                        userId, e.getMessage());
//            }
//        }, Executors.newSingleThreadExecutor());
//
//    }

//    private User.Role resolveUserRole(RegisterRequest request) {
//        return getDefaultRegistrationRole();
//    }
//
//    private User.Role getDefaultRegistrationRole() {
//        try{
////            return User.Role.valueOf(defaultRegistrationRole);
//            return User.Role.CUSTOMER;
//        } catch (IllegalArgumentException e) {
//            log.warn("Invalid default role '{}', falling back to CUSTOMER", defaultRegistrationRole);
//            return User.Role.CUSTOMER;
//        }
//    }

    private void validateRegistrationRequest(RegisterRequest request) {
        if (request.getName() == null || request.getName().trim().length() < 2) {
            throw new PasswordMismatchException("Name must be at least 2 characters");
        }
        if (request.getEmail() == null) {
            throw new PasswordMismatchException("Valid email required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 8) {
            throw new PasswordMismatchException("Password must be at least 8 characters");
        }
    }

//    private boolean isValidEmail(String email) {
//        return email != null && EMAIL_PATTERN.matcher(email).matches();
//    }

    public Authentication authenticate(String email, String password){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordMismatchException("Invalid email or password");
        }
        UserDetails userDetails = new UserDetailsImpl(user);
        return new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public void deleteUser(Long userId, String operator){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        if (!user.isActive()){
            log.warn("Attempt to delete already inactive user: {}", userId);
            return;
        }
        validateDeleteOperation(user, operator);
        user.softDelete();
        userRepository.save(user);
        log.info("User soft-deleted: userId={}", userId);
    }

    @Transactional
    public void incrementFailedLogin(String email){
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException("User not found"));

        // Only increment for active & enabled users
        if (user.getStatus() != User.UserStatus.ACTIVE || !user.isEnabled()) {
            log.info("User {} is non-active or disabled. Skipping failed login increment.", email);
            // Non-active users should not be locked or have failed attempts incremented
            return;
        }

        if (user.isActive()){
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts>=5){
                user.setLockTime(LocalDateTime.now().plusMinutes(30));
                log.warn("User {} locked due to {} failed attempts", email, attempts);
            }
            userRepository.save(user);
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new PasswordMismatchException("Password must be at least 8 characters");
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$")) {
            throw new PasswordMismatchException("Password must contain uppercase, lowercase, and number");
        }
    }

    private void validateDeleteOperation(User user, String operator) {
        if (isLastAdmin(user.getId())) {
            throw new RuntimeException("Cannot delete the last admin user");
        }
    }

    private boolean isLastAdmin(Long userId) {
        long activeAdminCount = userRepository.countByStatusAndRole(User.UserStatus.ACTIVE, User.Role.ADMIN);
        boolean isTargetAdmin = userRepository.isUserAdmin(userId);
        return activeAdminCount==1 && isTargetAdmin;
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        user.activate();
        userRepository.save(user);
        log.info("User activated: userId={}", userId);
    }
}
