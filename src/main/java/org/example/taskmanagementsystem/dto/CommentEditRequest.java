package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CommentEditRequest {
    @Size(min = 1, message = "Содержимое комментария не может быть пустым")
    @Size(max = 5000, message = "Максимальная длина содержимого комментария 5000 символов")
    private String content;
}
