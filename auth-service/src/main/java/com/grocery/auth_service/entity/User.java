package com.grocery.auth_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(columnNames = "email"),
        indexes = {@Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_status", columnList = "status")})
@Data
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private UserStatus status= UserStatus.ACTIVE;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int failedLoginAttempts=0;

    private LocalDateTime lockTime;

    private LocalDateTime deletedAt;

    public User() {
    }

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }


    public enum Role{
        CUSTOMER("ROLE_CUSTOMER"),
        ADMIN("ROLE_ADMIN"),
        DELIVERY_PARTNER("ROLE_DELIVERY_PARTNER");

        private final String authority;

        Role(String authority) {
            this.authority = authority;
        }
        public String getAuthority() {
            return authority;
        }
    }

    public enum UserStatus {
        ACTIVE, PENDING, SUSPENDED, INACTIVE
    }

    @PrePersist
    protected void onCreate(){
        if (createdAt==null){
            createdAt = LocalDateTime.now();
        }
    }

    public boolean isActive(){
        return status==UserStatus.ACTIVE && enabled && deletedAt == null;
    }

    public void softDelete(){
        this.status=UserStatus.INACTIVE;
        this.enabled = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.enabled = true;
        this.deletedAt = null;
    }

}
