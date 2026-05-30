package org.gitbounty.gitbountybackend.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Setter
@Getter
@Entity
@Table(name = "users",
indexes = @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id") )
public class User {

    // Getters and Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId; // Keycloak's unique sub claim string

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors
    public User() {
    }

    public User(String username, String email, String keycloakId) {
        this.username = username;
        this.email = email;
        this.keycloakId = keycloakId;
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

}

