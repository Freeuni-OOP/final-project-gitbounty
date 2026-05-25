package org.gitbounty.gitbountybackend.model;

import jakarta.persistence.*;
import org.aspectj.apache.bcel.Repository;
import org.gitbounty.gitbountybackend.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private String status = "OPEN";

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    // this has to be uncommented once the Repository entity is done implementing
//    @ManyToOne
//    @JoinColumn(name = "repository_id", nullable = false)
//    private Repository repository;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;



    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    // those as well has to be uncommented after implementing the repository entity
//    public Repository getRepository() { return repository; }
//    public void setRepository(Repository repository) { this.repository = repository; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }


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