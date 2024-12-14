package org.example.taskmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.taskmanagementsystem.models.TaskPriority;
import org.example.taskmanagementsystem.models.TaskStatus;
import org.example.taskmanagementsystem.models.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private List<CommentResponse> comments;
    private Set<UserResponse> assignees;
    private UserResponse author;
}
