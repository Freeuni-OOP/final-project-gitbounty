package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import lombok.*; // Import Lombok annotations
import java.time.LocalDateTime;

@Entity
@Table(name = "bounties")
@Data //generate all getters, setters, equals, hashCode, and toString
@NoArgsConstructor //generates the empty constructor
@AllArgsConstructor //generates a constructor with all fields
@Builder //alows usage of Bounty.builder().title("...").build()
public class Bounty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    private BountyStatus status;

    private LocalDateTime createdAt;

    //one-to-one with Issue
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "issue_id", referencedColumnName = "id")
    private Issue issue;

    public Bounty(String title, String description, Double amount, BountyStatus status) {
        this.title = title;
        this.description = description;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}