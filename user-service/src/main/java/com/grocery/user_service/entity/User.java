package com.grocery.user_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users", indexes = @Index(columnList = "email"))
@Data
@SQLDelete(sql = "UPDATE users SET deleted = true WHERE id = ?")
@Where(clause = "deleted = false")
public class User extends AuditEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    private String phone;
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;
    private boolean deleted = false;

    private boolean phoneVerified = false;
    private boolean emailVerified = false;

    @OneToMany(mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Address> addresses = new ArrayList<>();


    public enum Role {
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

}
