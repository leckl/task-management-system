package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentCreateRequest {
    @Size(min = 1, message = "Содержимое комментария не может быть пустым")
    @Size(max = 5000, message = "Максимальная длина содержимого комментария 5000 символов")
    private String content;
}
