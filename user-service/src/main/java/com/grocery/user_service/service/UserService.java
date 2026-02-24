package com.grocery.user_service.service;

import com.grocery.user_service.dto.AddressDto;
import com.grocery.user_service.dto.UserDto;
import com.grocery.user_service.dto.request.UserUpdateRequest;
import com.grocery.user_service.entity.Address;
import com.grocery.user_service.entity.User;
import com.grocery.user_service.exception.AddressNotFoundException;
import com.grocery.user_service.exception.UserNotFoundException;
import com.grocery.user_service.mapper.UserMapper;
import com.grocery.user_service.repository.AddressRepository;
import com.grocery.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserMapper userMapper;

    public UserDto getProfile(Long userId){
        log.debug("Fetching profile for userId: {}", userId);
        User user = getProfileEntity(userId);
        return userMapper.toDto(user);
    }

    public UserDto updateProfile(Long userId, UserUpdateRequest request){
        log.info("Updating profile for userId: {}", userId);
        validateUpdateRequest(request);
        User user = getProfileEntity(userId);
        userMapper.updateEntity(request,user);
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email already in use");
            }
        }
        User savedUser = userRepository.save(user);
//        auditEventPublisher.publish(AuditEventType.USER_PROFILE_UPDATED, userId, request);
        log.debug("Profile updated successfully for userId: {}", userId);
        return userMapper.toDto(savedUser);
    }



    public AddressDto addAddress(Long userId, AddressDto dto){
        log.info("Adding address for userId: {}", userId);
        validateAddressDto(dto);
       User user =getProfileEntity(userId);
        Address address = userMapper.toEntity(dto);
        address.setUser(user);
        if (address.isDefault()) {
            clearDefaultAddresses(user); // Ensure only one default
        }
        Address saveAddress = addressRepository.save(address);
//        auditEventPublisher.publish(AuditEventType.ADDRESS_ADDED, userId, savedAddress.getId());
        log.debug("Address added successfully: {}", saveAddress.getId());
        return userMapper.toDto(saveAddress);
    }

    public AddressDto updateAddress(Long userId, Long addressId, AddressDto dto) {
        User user = getProfileEntity(userId);
        Address address = getUserAddress(user, addressId);

        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        address.setLabel(dto.getLabel());

        if (dto.isDefault()) {
            clearDefaultAddresses(user);
            address.setDefault(true);
        }

        return userMapper.toDto(addressRepository.save(address));
    }

    public void deleteAddress(Long userId, Long addressId){
        log.info("Deleting address {} for userId: {}", addressId, userId);
        User user = getProfileEntity(userId);
        Address address = getAddressEntity(addressId);
        boolean wasDefault = address.isDefault();
        if (!address.getUser().equals(user)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }
        addressRepository.delete(address);
        if (wasDefault) {
            assignAnotherDefault(userId);
        }
//        auditEventPublisher.publish(AuditEventType.ADDRESS_DELETED, userId, addressId);
        log.debug("Address deleted successfully: {}", addressId);
    }

    public AddressDto setDefaultAddress(Long userId, Long addressId){
        log.info("Setting default address {} for userId: {}", addressId, userId);
        User user = getProfileEntity(userId);
        Address address = getAddressEntity(addressId);
        if (!address.getUser().equals(user)) {
            throw new IllegalArgumentException("Address does not belong to user");
        }
        clearDefaultAddresses(user);
        address.setDefault(true);
        Address savedAddress = addressRepository.save(address);
//        auditEventPublisher.publish(AuditEventType.DEFAULT_ADDRESS_SET, userId, addressId);
        log.debug("Default address set successfully: {}", addressId);
        return userMapper.toDto(savedAddress);
    }



    @PreAuthorize("hasRole('ADMIN')")
    public void softDeleteUser(Long adminUserId, Long targetUserId){
        log.warn("Admin {} requested soft delete for user: {}", adminUserId, targetUserId);
        if (adminUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot delete own account");
        }
        User user = getProfileEntity(targetUserId);
        if (user.isDeleted()) {
            throw new IllegalStateException("User already deleted");
        }
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalStateException("Admin user cannot be deleted");
        }
        user.setDeleted(true);
        user.setEnabled(false);
        userRepository.save(user);
//        auditEventPublisher.publish(AuditEventType.USER_DELETED, adminUserId, targetUserId);
        log.info("User soft deleted successfully: {}", targetUserId);
    }

    public List<AddressDto> getAddresses(Long userId) {
        log.debug("Fetching addresses for userId: {}", userId);
        User user = getProfileEntity(userId);
        return user.getAddresses().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    public UserDto  verifyEmail(Long userId) {
        log.info("Verifying email for userId: {}", userId);
        User user = getProfileEntity(userId);
        user.setEmailVerified(true);
        User savedUser = userRepository.save(user);
//        auditEventPublisher.publish(AuditEventType.EMAIL_VERIFIED, userId);
        return userMapper.toDto(savedUser);
    }

    public UserDto  verifyPhone(Long userId) {
        log.info("Verifying phone for userId: {}", userId);
        User user = getProfileEntity(userId);
        user.setPhoneVerified(true);
        User savedUser = userRepository.save(user);
//        auditEventPublisher.publish(AuditEventType.PHONE_VERIFIED, userId);
        return userMapper.toDto(savedUser);
    }

    private void validateUpdateRequest(UserUpdateRequest request) {
        if (request.getName() != null && request.getName().trim().length() < 2) {
            throw new IllegalArgumentException("Name must be at least 2 characters");
        }
        if (request.getPhone() != null && !request.getPhone().matches("^\\+?[1-9]\\d{1,14}$")) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    private void validateAddressDto(AddressDto dto) {
        if (dto.getStreet() == null || dto.getStreet().trim().isEmpty()) {
            throw new IllegalArgumentException("Street is required");
        }
        if (dto.getCity() == null || dto.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
    }
    private void clearDefaultAddresses(User user) {
        user.getAddresses().forEach(address -> {
            if (address.isDefault()){
                address.setDefault(false);
                addressRepository.save(address);
            }
        });
    }

    private User getProfileEntity(Long userId) {
        return userRepository.findById(userId)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private Address getAddressEntity(Long addressId){
        return addressRepository.findById(addressId)
                .orElseThrow(()-> new AddressNotFoundException("Address not found: " + addressId));
    }

    private Address getUserAddress(User user, Long addressId) {
        return addressRepository.findById(addressId)
                .filter(address -> address.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Address not found or not owned by user"));
    }

    private void assignAnotherDefault(Long userId) {
        List<Address> addresses = addressRepository.findByUserId(userId);
        if (!addresses.isEmpty()) {
            Address first = addresses.get(0);
            first.setDefault(true);
            addressRepository.save(first);
        }
    }

}
