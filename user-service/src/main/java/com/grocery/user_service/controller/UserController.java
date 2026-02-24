package com.grocery.user_service.controller;

import com.grocery.user_service.config.UserPrincipal;
import com.grocery.user_service.dto.AddressDto;
import com.grocery.user_service.dto.UserDto;
import com.grocery.user_service.dto.request.UserUpdateRequest;
import com.grocery.user_service.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User profile and address operations")
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile",description = "Returns the authenticated user's profile with addresses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "400",description = "User not found")
    })
    public ResponseEntity<UserDto> getProfile(){
        Long userId = getCurrentUserId();
        log.debug("Fetching profile for userId: {}", userId);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

//    @GetMapping("/profile")
//    public ResponseEntity<UserDto> getProfile(Authentication authentication){
//        Long userId = extractUserId(authentication);
//        return ResponseEntity.ok(userService.getProfile(userId));
//    }


    @PutMapping("/profile")
    @Operation(summary = "Update current user profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    public ResponseEntity<UserDto> updateProfile(@Valid @RequestBody UserUpdateRequest request){
        Long userId = getCurrentUserId();
        log.info("Updating profile for userId: {}", userId);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }


    @GetMapping("/addresses")
    @Operation(summary = "Get all user addresses")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully")
    })
    public ResponseEntity<List<AddressDto>> getAddresses(){
        Long userId = getCurrentUserId();
        log.debug("Fetching addresses for userId: {}", userId);
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add new address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid address data")
    })
    public ResponseEntity<AddressDto> addAddress(@Valid @RequestBody AddressDto dto){
        Long userId = getCurrentUserId();
        log.info("Adding address for userId: {}", userId);
        return ResponseEntity.ok(userService.addAddress(userId, dto));
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Update existing address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDto> updateAddress(@PathVariable Long addressId, @RequestBody AddressDto dto){
        Long userId = getCurrentUserId();
        log.info("Updating address {} for userId: {}", addressId, userId);
        return ResponseEntity.ok(userService.updateAddress(userId,addressId,dto));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete user address")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<Void> deleteAddress(@PathVariable Long addressId){
        Long userId = getCurrentUserId();
        log.info("Deleting address {} for userId: {}", addressId, userId);
        userService.deleteAddress(userId,addressId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/addresses/{addressId}/default")
    @Operation(summary = "Set address as default")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Default address set successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found")
    })
    public ResponseEntity<AddressDto> setDefaultAddress(@PathVariable Long addressId) {
        Long userId = getCurrentUserId();
        log.info("Setting default address {} for userId: {}", addressId, userId);
        return ResponseEntity.ok(userService.setDefaultAddress(userId, addressId));
    }


    @PostMapping("/verify-email")
    @Operation(summary = "Verify user email", description = "Typically called after email confirmation link click")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email verified successfully")
    })
    public ResponseEntity<UserDto> verifyEmail(){
        Long userId=getCurrentUserId();
        log.info("Verifying email for userId: {}", userId);
        return ResponseEntity.ok(userService.verifyEmail(userId));
    }

    @PostMapping("/verify-phone")
    @Operation(summary = "Verify user phone number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phone verified successfully")
    })
    public ResponseEntity<UserDto> verifyPhone() {
        Long userId = getCurrentUserId();
        log.info("Verifying phone for userId: {}", userId);
        return ResponseEntity.ok(userService.verifyPhone(userId));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        if (auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.userId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
    }


    private Long extractUserId(Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.userId();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication principal");
    }

}
