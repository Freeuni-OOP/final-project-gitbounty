package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "codebase_members", uniqueConstraints = {
    // Ensures a user can only be added to a specific repository once
    @UniqueConstraint(columnNames = {"repo_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodebaseMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repo_id", nullable = false)
    private Codebase codebase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CodebaseRole role;
}