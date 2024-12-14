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
import org.example.taskmanagementsystem.dto.*;
import org.example.taskmanagementsystem.services.CommentService;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "Comments", description = "API для управления комментариями задач")
@AllArgsConstructor
@RequestMapping("/comment")
public class CommentController {
    private final CommentService commentService;

    @Operation(
            summary = "Получить все комментарии к существующей задаче",
            description = "Этот метод позволяет получить список всех комментариев к существующей задаче. Доступен только для пользователей с ролью ADMIN и исполнителей задачи",
            parameters = {
                    @Parameter(name = "taskId", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешное получение задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = CommentResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN или не является исполнителем задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на просмотр комментариев этой задачи\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Задача не найдена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такой задачи не существует\"}")
                            )
                    )
            }
    )
    @GetMapping("/{taskId}")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable("taskId") long taskId) {
        return ResponseEntity.status(HttpStatus.OK).body(commentService.getCommentsForTask(taskId));
    }

    @Operation(
            summary = "Создать комментарий к существующей задаче",
            description = "Этот метод позволяет создать новый комментарий для существующей задачи. Доступен только для пользователей с ролью ADMIN и исполнителей задачи",
            parameters = {
                    @Parameter(name = "taskId", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания комментария",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                    {
                                        "content": "Содержимое комментария"
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешное получение задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Комментарий успешно создан\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN или не является исполнителем задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на публикацию комментариев к этой задаче\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Задача не найдена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такой задачи не существует\"}")
                            )
                    )
            }
    )
    @PostMapping("/create/{taskId}")
    public ResponseEntity<ApiMessageResponse> createComment(@PathVariable Long taskId, @Valid @RequestBody CommentCreateRequest commentCreateRequest) {
        commentService.createComment(taskId, commentCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse("Комментарий успешно создан"));
    }

    @Operation(
            summary = "Изменение существующего комментария",
            description = "Этот метод позволяет изменить существующий комментарий. Доступен только для автора комментария",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор комментария",
                            required = true, example = "1")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для изменения комментария",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentEditRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                    {
                                        "content": "Новое содержимое комментария"
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Комментарий успешно изменён",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Комментарий успешно изменён\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён. Пользователь не является автором комментария",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на изменение данного комментария\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Комментарий не найден",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такой комментарий не существует\"}")
                            )
                    )
            }
    )
    @PatchMapping("/edit/{id}")
    public ResponseEntity<ApiMessageResponse> editComment(@PathVariable Long id, @Valid @RequestBody CommentEditRequest commentEditRequest) {
        commentService.editComment(id, commentEditRequest);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Комментарий успешно изменён"));
    }

    @Operation(
            summary = "Удалить существующий комментарий",
            description = "Этот метод позволяет удалить существующий комментарий. Доступно только для автора комментария",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор комментария",
                            required = true, example = "1")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Комментарий успешно удалён",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Комментарий успешно удалён\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён. Пользователь не является автором комментария",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на выполнение данной операции\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Задача не найдена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такой комментарий не существует\"}")
                            )
                    )
            }
    )
    @DeleteMapping("/delete/{id}")
    private ResponseEntity<ApiMessageResponse> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Комментарий успешно удалён"));
    }
}
