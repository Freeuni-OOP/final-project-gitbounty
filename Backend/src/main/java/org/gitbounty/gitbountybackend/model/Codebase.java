package org.gitbounty.gitbountybackend.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Getter
@Entity
@Table(
    name = "codebases",
    uniqueConstraints = @UniqueConstraint(name = "uk_codebases_name", columnNames = "name")
)
public class Codebase {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Setter
    @Column(length = 1000)
    private String description;

    @Setter
    @Column(name = "git_url", nullable = false, length = 2048)
    private String gitUrl;

    @Setter
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
        name = "owner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_codebases_owner")
    )
    private User owner;

    @Setter
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public Codebase() {
    }

    public Codebase(String name, String description, String gitUrl, User owner) {
        this.name = name;
        this.description = description;
        this.gitUrl = gitUrl;
        this.owner = owner;
    }


    @Override
    public String toString() {
        return "Codebase{" +
            "id=" + id +
            ", name='" + name + '\'' +
            ", description='" + description + '\'' +
            ", gitUrl='" + gitUrl + '\'' +
            ", owner=" + (owner != null ? owner.getUsername() : null) +
            ", createdAt=" + createdAt +
            '}';
    }
}
