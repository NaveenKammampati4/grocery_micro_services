package com.grocery.auth_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_reset_token", columnList = "token", unique = true),
                @Index(name = "idx_reset_user", columnList = "user_id"),
                @Index(name = "idx_reset_expiry", columnList = "expiry_date")
        })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255, name = "token_hash")
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    private LocalDateTime usedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime expiryDate;


    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (expiryDate == null) {
            expiryDate = LocalDateTime.now().plusMinutes(60);
        }
    }

    public boolean isExpired() {
        return usedAt != null || LocalDateTime.now().isAfter(expiryDate);
    }

}
