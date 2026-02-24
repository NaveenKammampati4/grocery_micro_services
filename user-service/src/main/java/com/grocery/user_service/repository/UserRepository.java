package com.grocery.user_service.repository;

import com.grocery.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Page<User> findByRoleAndEnabled(User.Role role, boolean enabled, Pageable pageable);

    @Modifying
    @Query("UPDATE User u SET u.deleted = true WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);

}
