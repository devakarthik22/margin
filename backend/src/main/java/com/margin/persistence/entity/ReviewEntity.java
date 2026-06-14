package com.margin.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Persistent record of one completed review, keyed for idempotency by headSha. */
@Entity
@Table(name = "reviews", indexes = @Index(name = "ix_reviews_head_sha", columnList = "head_sha"))
public class ReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String repo;

    @Column(name = "pr_number", nullable = false)
    private int prNumber;

    @Column(name = "head_sha", nullable = false, unique = true)
    private String headSha;

    @Column(nullable = false)
    private String verdict;

    @Column(length = 1000)
    private String summary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FindingEntity> findings = new ArrayList<>();

    public ReviewEntity() { }

    public void addFinding(FindingEntity finding) {
        finding.setReview(this);
        findings.add(finding);
    }

    // getters / setters
    public Long getId() { return id; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getRepo() { return repo; }
    public void setRepo(String repo) { this.repo = repo; }
    public int getPrNumber() { return prNumber; }
    public void setPrNumber(int prNumber) { this.prNumber = prNumber; }
    public String getHeadSha() { return headSha; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }
    public String getVerdict() { return verdict; }
    public void setVerdict(String verdict) { this.verdict = verdict; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<FindingEntity> getFindings() { return findings; }
    public java.time.Instant getCreatedAt() { return createdAt; }
}
