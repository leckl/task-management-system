package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.taskmanagementsystem.models.TaskStatus;

@Data
public class TaskStatusUpdateRequest {
    @NotNull(message = "Необходимо выбрать статус задачи")
    public TaskStatus status;
}
