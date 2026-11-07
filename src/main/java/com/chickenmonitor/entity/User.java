package com.chickenmonitor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;



@Entity
@Table(name = "`user`") // Dùng dấu backtick vì 'user' là từ khóa hệ thống trong một số DB
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "fullname", nullable = false, length = 100)
    private String fullname;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "role", nullable = false, length = 50)
    private String role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Farm> farms;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications;

    @PrePersist
    @PreUpdate
    public void normalizeBeforeSave() {
        this.role = normalizeRole(this.role);
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "USER";
        }

        String normalized = role.trim().toUpperCase();
        return "ADMIN".equals(normalized) ? "ADMIN" : "USER";
    }

    public String getNormalizedRole() {
        return normalizeRole(this.role);
    }
}