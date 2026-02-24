package com.grocery.user_service.repository;

import com.grocery.user_service.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address,Long> {

    Optional<Address> findByIdAndUserId(Long id, Long userId);
    List<Address> findByUserIdAndIsDefaultTrue(Long userId);
    List<Address> findByUserId(Long userId);
}
