package com.todo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Subtask {

    @Column(nullable = false)
    private String id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(nullable = false)
    private Boolean completed = false;

    // Constructors
    public Subtask() {}

    public Subtask(String id, String title, Boolean completed) {
        this.id = id;
        this.title = title;
        this.completed = completed;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
}
