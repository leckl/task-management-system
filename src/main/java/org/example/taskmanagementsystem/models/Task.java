package org.example.taskmanagementsystem.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 70, nullable = false)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private TaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private TaskStatus status;

    @ManyToMany
    @JoinTable(
            name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> assignees = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
}

