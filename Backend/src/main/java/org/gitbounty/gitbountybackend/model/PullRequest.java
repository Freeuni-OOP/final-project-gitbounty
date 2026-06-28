package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@DiscriminatorValue("PULL_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequest extends Issue {

    // Allow source branch to be nullable so the branch can be deleted while keeping the PR record.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_branch_id", nullable = true)
    private Branch sourceBranch; // The branch being merged from (may be null if deleted)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_branch_id", nullable = false)
    private Branch targetBranch; // The branch being merged into

    @Column(name = "merged_at")
    private Instant mergedAt; // Null until PR is merged

    /**
     * Validates and normalizes the title.
     * @param title the title to normalize
     * @return normalized title
     * @throws IllegalArgumentException if title is null or blank
     */
    public static String normalizeTitle(String title) {
        if (title == null || title.trim().isBlank()) {
            throw new IllegalArgumentException("Pull request title is required");
        }
        return title.trim();
    }

    /**
     * Validates and normalizes the description.
     * @param description the description to normalize
     * @return normalized description (null if input is null)
     */
    public static String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }
}


