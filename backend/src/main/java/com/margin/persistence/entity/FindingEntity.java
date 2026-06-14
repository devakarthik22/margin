package com.margin.persistence.entity;

import jakarta.persistence.*;

/** Persistent finding belonging to a {@link ReviewEntity}. */
@Entity
@Table(name = "findings")
public class FindingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    private ReviewEntity review;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String severity;

    @Column(name = "line_number")
    private Integer line;

    @Column(name = "file_path")
    private String filePath;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String explanation;

    @Column(length = 2000)
    private String suggestion;

    public FindingEntity() { }

    public void setReview(ReviewEntity review) { this.review = review; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public Integer getLine() { return line; }
    public void setLine(Integer line) { this.line = line; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
}
