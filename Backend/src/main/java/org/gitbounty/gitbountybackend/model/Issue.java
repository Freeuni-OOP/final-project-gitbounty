package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Setter
@Getter
@Entity
@Table(name = "issues")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("ISSUE")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer number;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    // Bounty amount offered for completing this issue
    @Column(name = "bounty_amount", nullable = false)
    private BigDecimal bountyAmount = BigDecimal.ZERO;

//    @Column(nullable = false)
//    private String status = "OPEN";

    // changed to using enums
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueStatus status = IssueStatus.OPEN;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;


    // those as well has to be uncommented after implementing the repository entity
    @ManyToOne
    @JoinColumn(name = "repository_id", nullable = false)
    private Codebase repository;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}