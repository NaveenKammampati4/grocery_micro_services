package com.grocery.user_service.repository;

import com.grocery.user_service.entity.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserProfile,Long> {

    Optional<UserProfile> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<UserProfile> findByRoleAndEnabled(UserProfile.Role role, boolean enabled, Pageable pageable);

    @Modifying
    @Query("UPDATE UserProfile u SET u.deleted = true WHERE u.userId = :id")
    void softDeleteById(@Param("id") Long id);

}
