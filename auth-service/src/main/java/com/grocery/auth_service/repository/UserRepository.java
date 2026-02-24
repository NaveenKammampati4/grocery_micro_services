package com.grocery.auth_service.repository;

import com.grocery.auth_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.enabled = true AND u.deletedAt IS NULL")
    List<User> findAllActiveUsers();

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.enabled= true AND u.deletedAt IS NULL")
    long countActiveUsers();

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.status = 'INACTIVE', u.enabled = false, u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.role = :role AND u.deletedAt IS NULL")
    long countByStatusAndRole(@Param("status") User.UserStatus status,
                              @Param("role") User.Role role);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
            "WHERE u.id = :id AND u.role = :role AND u.status = :status AND u.deletedAt IS NULL")
    boolean isUserAdmin(@Param("id") Long id, @Param("role") User.Role role,
                        @Param("status") User.UserStatus status);

    default boolean isUserAdmin(Long id) {
        return isUserAdmin(id, User.Role.ADMIN, User.UserStatus.ACTIVE);
    }

    @Query("SELECT u FROM User u WHERE " +
            "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:status IS NULL OR u.status = :status)")
    Page<User> findAllWithFilters(@Param("search") String search,
                                  @Param("role") User.Role role,
                                  @Param("status") User.UserStatus status,
                                  Pageable pageable);

//    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.role = 'ADMIN'")
//    boolean isUserAdmin(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.status = :status, u.enabled = :enabled WHERE u.id IN :userIds")
    int updateStatusByIds(@Param("userIds") List<Long> userIds,
                          @Param("status") User.UserStatus status,
                          @Param("enabled") Boolean enabled);

}
