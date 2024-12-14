package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.taskmanagementsystem.models.TaskPriority;
import org.example.taskmanagementsystem.models.TaskStatus;

import java.util.Set;

@Data
@AllArgsConstructor
public class TaskCreateRequest {
    @Size(min =1, message = "Название задачи не может быть пустым")
    @Size(max = 70, message = "Максимальная длина названия названия 70 символов")
    private String title;

    @Size(min =1, message = "Описание задачи не может быть пустым")
    @Size(max = 70, message = "Максимальная длина поля 70 символов")
    private String description;

    @NotNull(message = "Необходимо выбрать приоритет задачи")
    private TaskPriority priority;

    @NotEmpty(message = "Необходим как мимнимум оин исполнитель")
    private Set<Long> assigneeIds;
}