package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.taskmanagementsystem.models.TaskPriority;
import org.example.taskmanagementsystem.models.TaskStatus;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskEditRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Set<Long> assigneeIds;
}
