package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;

@Entity
@Table(name = "branches", uniqueConstraints = {
    // Ensures a branch name is unique within a single codebase
    @UniqueConstraint(columnNames = {"codebase_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Codebase codebase;

    @Column(nullable = false, length = 255)
    private String name; // e.g., "refs/heads/main" or "refs/heads/feature-xyz"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_commit_id")
    private Commit latestCommit; // Pointer to the HEAD commit of this branch

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}