package com.grocery.auth_service.controller;

import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.dto.request.BulkStatusUpdateRequest;
import com.grocery.auth_service.dto.request.UpdateUserStatusRequest;
import com.grocery.auth_service.dto.response.UserResponse;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminUserService adminUserService;
    private final UserRepository userRepository;

    public AdminController(AdminUserService adminUserService, UserRepository userRepository) {
        this.adminUserService = adminUserService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody RegisterRequest request){
        User user = adminUserService.createAdmin(request);
        log.info("Admin created by {}: userId={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                user.getId(), user.getEmail());
        return ResponseEntity.ok(Map.of("message","Admin Created Successfully with Id: "+user.getId()));
    }

    @PostMapping("/create-delivery-partner")
    public ResponseEntity<?> createDeliveryPartner(@RequestBody RegisterRequest request){
        User user = adminUserService.createDeliveryPartner(request);
        log.info("Delivery partner created by {}: userId={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                user.getId(), user.getEmail());
        return ResponseEntity.ok(Map.of("message","Delivery partner created successfully","userId", user.getId()));
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) User.Role role,
            @RequestParam(required = false)User.UserStatus status){
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAllWithFilters(search, role, status, pageable);
        List<UserResponse> userResponses = users.getContent().stream().map(this::mapToUserResponse).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "content", userResponses,
                "page", users.getNumber(),
                "size", users.getSize(),
                "totalElements", users.getTotalElements(),
                "totalPages", users.getTotalPages()
        ));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId){
        User user = adminUserService.findByUserId(userId);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    // Bulk user status update
    @PatchMapping("/users/bulk-status")
    public ResponseEntity<?> bulkUpdateStatus(@Valid @RequestBody BulkStatusUpdateRequest request) {
        int updatedCount = userRepository.updateStatusByIds(
                request.getUserIds(), request.getStatus(), request.getEnabled());

        log.info("Bulk status update: {} users updated by {}",
                updatedCount, SecurityContextHolder.getContext().getAuthentication().getName());

        return ResponseEntity.ok(Map.of(
                "message", "Bulk update successful",
                "updatedCount", updatedCount
        ));
    }

    @GetMapping("/users/{userId}/security")
    public ResponseEntity<Map<String, Object>> getUserSecurityInfo(
            @PathVariable Long userId) {
        User user = adminUserService.findByUserId(userId);
        Map<String, Object> securityInfo = adminUserService.getUserSecurityInfo(user.getEmail());
        return ResponseEntity.ok(Map.of("Security info retrieved", securityInfo));
    }

    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        User updatedUser = adminUserService.updateUserStatus(userId,request.getStatus(),request.isEnabled());
        return ResponseEntity.ok(mapToUserResponse(updatedUser));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .status(user.getStatus())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .isActive(user.isActive())
                .build();
    }
}
