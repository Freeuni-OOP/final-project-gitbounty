package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import java.time.Instant;

@Entity
@Table(name = "commits", uniqueConstraints = {
    // A single commit hash should only be logged once per codebase scope
    @UniqueConstraint(columnNames = {"codebase_id", "commit_hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codebase_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Codebase codebase;

    @Column(name = "commit_hash", nullable = false, length = 40, updatable = false)
    private String commitHash; // The full 40-character SHA-1 Git hash string

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "author_email", nullable = false, length = 255)
    private String authorEmail;

    @Lob // Maps to a LONGTEXT/CLOB type in the DB to support massive multi-line Git messages
    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "committed_at", nullable = false, updatable = false)
    private Instant committedAt; // Converted from JGit's integer epoch timestamp
}