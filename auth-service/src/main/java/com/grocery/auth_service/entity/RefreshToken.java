package com.grocery.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    private String userEmail;

    private LocalDateTime expiryDate;

    private LocalDateTime revokedAt;

    @PrePersist
    public void onPersist(){
        this.expiryDate = LocalDateTime.now().plusSeconds(604800);
    }

    public boolean isExpired(){
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isRevoked() {
        return revokedAt != null || isExpired();
    }


}
