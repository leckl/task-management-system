package org.example.taskmanagementsystem.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.dto.AuthRequest;
import org.example.taskmanagementsystem.dto.TaskResponse;
import org.example.taskmanagementsystem.models.Role;
import org.example.taskmanagementsystem.models.User;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.example.taskmanagementsystem.services.AuthService;
import org.example.taskmanagementsystem.services.UserService;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/auth")
@Tag(
        name = "Auth",
        description = "API для авторизации по почте и паролю"
)
@AllArgsConstructor
public class AuthController {

        private final AuthService authService;

        private final UserService userService;

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Этот метод позволяет зарегестрировать нового пользователя. Доступен для неавторизированных пользователей",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                            {
                                                "email": "example@mail.com",
                                                "password": "password",
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешная регистрация",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Успешная регистрация\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Пользовать уже авторизирован",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Вы уже авторизованы\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Введённая почта уже занята",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такая почта уже занята\"}")
                            )
                    )
            }
    )
    @PostMapping("/register")
    public ResponseEntity<ApiMessageResponse> register(@RequestBody @Valid AuthRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Успешная регистрация"));
    }

    @Operation(
            summary = "Авторизация существующего пользователя",
            description = "Этот метод позволяет авторизировать существующего пользователя. Доступен для неавторизированных пользователей",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для авторизации",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                            {
                                                "email": "example@mail.com",
                                                "password": "password",
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешная авторизация. В ответе возвращается JWT токен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"{{token}}\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Пользовать уже авторизирован",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Вы уже авторизованы\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Введённая почта уже занята",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такая почта уже занята\"}")
                            )
                    )
            }
    )
    @PostMapping("/login")
    public ResponseEntity<ApiMessageResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse(authService.login(request)));
    }

    @PatchMapping("/upgrade-to-admin")
    public ResponseEntity<String> upgradeToAdmin() {
        userService.upgradeToAdmin();
        return ResponseEntity.ok("Upgrade to admin successfully");
    }
}