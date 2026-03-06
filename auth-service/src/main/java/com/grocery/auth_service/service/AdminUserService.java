package com.grocery.auth_service.service;

import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.exception.authenticationException.DuplicateEmailException;
import com.grocery.auth_service.exception.authenticationException.UserNotFoundException;
import com.grocery.auth_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createAdmin(RegisterRequest request){
        return createUser(request, User.Role.ADMIN);
    }

    public User createDeliveryPartner(RegisterRequest request){
        validateDeliveryPartnerCreation(request);
        return createUser(request, User.Role.DELIVERY_PARTNER);
    }

    private User createUser(RegisterRequest request, User.Role role) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)){
            log.warn("Admin attempted to create existing email: {}", email);
            throw new DuplicateEmailException("Email already exists");
        }
        User user=User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status(User.UserStatus.ACTIVE)
                .enabled(true)
                .build();
        User savedUser = userRepository.save(user);

        log.info("{} created successfully: {}", role, email);

        return savedUser;
    }

    public User findByUserId(Long userId){
        return userRepository.findById(userId).orElseThrow(()-> new UserNotFoundException("User not found: " + userId));
    }

    public Map<String, Object> getUserSecurityInfo(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(()->new UsernameNotFoundException("User not found: " + email));
        return Map.of(
                "email", user.getEmail(),
                "role", user.getRole(),
                "enabled", user.isEnabled(),
                "status", user.getStatus(),
                "lastLogin", "Not tracked", // Add if needed
                "failedAttempts", user.getFailedLoginAttempts(),
                "isLocked", isAccountLocked(email)
        );
    }

    public boolean isAccountLocked(String email){
        return userRepository.findByEmail(email)
                .filter(user -> user.getLockTime()!=null && user.getLockTime().isAfter(LocalDateTime.now())).isPresent();
    }

    @Transactional
    public User updateUserStatus(Long userId,User.UserStatus status, boolean enabled){
        User user = findByUserId(userId);
        user.setStatus(status);
        user.setEnabled(enabled);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private void validateDeliveryPartnerCreation(RegisterRequest request) {
        // Additional delivery partner validations
        if (!request.getEmail().endsWith("partner.com") &&
                !request.getEmail().endsWith("@delivery")) {
            log.warn("Suspicious delivery partner email: {}", request.getEmail());
        }
    }
}
