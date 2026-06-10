package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "pull_requests", uniqueConstraints = {
    // Ensures a PR number is unique within a single codebase
    @UniqueConstraint(columnNames = {"codebase_id", "pr_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    private Codebase codebase;

    @Column(name = "pr_number", nullable = false)
    private Integer prNumber; // Sequential PR number within a codebase

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PRStatus status = PRStatus.OPEN; // OPEN, MERGED, CLOSED, DRAFT

    // Allow source branch to be nullable so the branch can be deleted while keeping the PR record.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_branch_id", nullable = true)
    private Branch sourceBranch; // The branch being merged from (may be null if deleted)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_branch_id", nullable = false)
    private Branch targetBranch; // The branch being merged into

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author; // User who created the pull request

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "merged_at")
    private Instant mergedAt; // Null until PR is merged

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}


