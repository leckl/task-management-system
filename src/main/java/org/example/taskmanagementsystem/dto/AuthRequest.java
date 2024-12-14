package org.example.taskmanagementsystem.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthRequest {
    @Size(min = 1, message = "Почта не может быть пустой")
    @Size(max = 70, message = "Максимальная длина почты 70 символов")
    private String email;
    @Size(min = 1, message = "Пароль не может быть пустым")
    private String password;
}
