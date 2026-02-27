package com.grocery.auth_service.controller;

import com.grocery.auth_service.dto.request.RegisterRequest;
import com.grocery.auth_service.dto.request.BulkStatusUpdateRequest;
import com.grocery.auth_service.dto.request.UpdateUserStatusRequest;
import com.grocery.auth_service.dto.response.UserResponse;
import com.grocery.auth_service.entity.User;
import com.grocery.auth_service.repository.UserRepository;
import com.grocery.auth_service.service.AdminUserService;
import com.grocery.auth_service.swaggerapi.ApiStatusCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Admin Management", description = "Administrative operations for managing users, roles, and security.")
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

    @Operation(summary = "Create admin user",
            description = "Creates a new ADMIN account. Only accessible by ADMIN users.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "Admin created successfully"),
            @ApiResponse(responseCode = ApiStatusCodes.BAD_REQUEST, description = "Invalid request payload"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
    @PostMapping("/create-admin")
    public ResponseEntity<?> createAdmin(@RequestBody RegisterRequest request){
        User user = adminUserService.createAdmin(request);
        log.info("Admin created by {}: userId={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                user.getId(), user.getEmail());
        return ResponseEntity.ok(Map.of("message","Admin Created Successfully with Id: "+user.getId()));
    }

    @Operation(summary = "Create delivery partner",
            description = "Creates a new DELIVERY_PARTNER account.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "Delivery partner created successfully"),
            @ApiResponse(responseCode = ApiStatusCodes.BAD_REQUEST, description = "Invalid request payload"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
    @PostMapping("/create-delivery-partner")
    public ResponseEntity<?> createDeliveryPartner(@RequestBody RegisterRequest request){
        User user = adminUserService.createDeliveryPartner(request);
        log.info("Delivery partner created by {}: userId={}, email={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                user.getId(), user.getEmail());
        return ResponseEntity.ok(Map.of("message","Delivery partner created successfully","userId", user.getId()));
    }

    @Operation(summary = "Get paginated users",
            description = "Returns paginated list of users with optional filtering by search, role, and status.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "Users retrieved successfully"),
            @ApiResponse(responseCode = ApiStatusCodes.BAD_REQUEST, description = "Invalid query parameters"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @Parameter(description = "Page number (0-based index)",example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page Size",example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Search by name or email")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by user role",schema = @Schema(implementation = User.Role.class))
            @RequestParam(required = false) User.Role role,

            @Parameter(description = "Filter by user status",
                    schema = @Schema(implementation = User.UserStatus.class))
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


    @Operation(summary = "Get user by ID",
            description = "Returns detailed information for a specific user.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "User retrieved successfully"),
            @ApiResponse(responseCode = ApiStatusCodes.NOT_FOUND, description = "User not found"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUser(@PathVariable Long userId){
        User user = adminUserService.findByUserId(userId);
        return ResponseEntity.ok(mapToUserResponse(user));
    }

    @Operation(summary = "Bulk update user status",
            description = "Updates status and enabled flag for multiple users at once.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "Bulk update successful"),
            @ApiResponse(responseCode = ApiStatusCodes.BAD_REQUEST, description = "Invalid request payload"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
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


    @Operation(summary = "Get user security information",
            description = "Returns security-related information such as failed login attempts and token status.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "Security info retrieved"),
            @ApiResponse(responseCode = ApiStatusCodes.NOT_FOUND, description = "User not found"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
    @GetMapping("/users/{userId}/security")
    public ResponseEntity<Map<String, Object>> getUserSecurityInfo(
            @PathVariable Long userId) {
        User user = adminUserService.findByUserId(userId);
        Map<String, Object> securityInfo = adminUserService.getUserSecurityInfo(user.getEmail());
        return ResponseEntity.ok(Map.of("Security info retrieved", securityInfo));
    }

    @Operation(summary = "Update user status",
            description = "Updates status and enabled flag for a specific user.")
    @ApiResponses({
            @ApiResponse(responseCode = ApiStatusCodes.OK, description = "User status updated successfully"),
            @ApiResponse(responseCode = ApiStatusCodes.NOT_FOUND, description = "User not found"),
            @ApiResponse(responseCode = ApiStatusCodes.BAD_REQUEST, description = "Invalid request payload"),
            @ApiResponse(responseCode = ApiStatusCodes.FORBIDDEN, description = "Access denied"),
            @ApiResponse(responseCode = ApiStatusCodes.INTERNAL_SERVER_ERROR, description = "Unexpected server error")
    })
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
