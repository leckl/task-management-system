package org.example.taskmanagementsystem.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.dto.TaskCreateRequest;
import org.example.taskmanagementsystem.dto.TaskEditRequest;
import org.example.taskmanagementsystem.dto.TaskResponse;
import org.example.taskmanagementsystem.dto.TaskStatusUpdateRequest;
import org.example.taskmanagementsystem.models.TaskPriority;
import org.example.taskmanagementsystem.models.TaskStatus;
import org.example.taskmanagementsystem.models.User;
import org.example.taskmanagementsystem.services.TaskService;
import org.example.taskmanagementsystem.services.UserService;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
@Tag(name = "Tasks", description = "API для управления задачами")
@AllArgsConstructor
public class TaskController {
    private TaskService taskService;
    private UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить все задачи",
            description = "Этот метод позволяет получить список всех задач. Доступен только для пользователей с ролью ADMIN",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список задач успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на выполнение данной операции\"}")
                            )
                    )
            }
    )
    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.getAllTasks();
    }

    @Operation(
            summary = "Получить задачу по ID",
            description = "Этот метод позволяет получить задачу по её идентификатору. Доступен только для пользователей с ролью ADMIN и исполнителей задачи",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешное получение задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN или не является исполнителем задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на просмотр данной задачи\"}")
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
    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskService.getTaskById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Создать новую задачу",
            description = "Этот метод позволяет создать новую задачу. Доступен только для пользователей с ролью ADMIN",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для создания задачи",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskCreateRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                    {
                                        "title": "Название задачи",
                                        "description": "Описание задачи",
                                        "priority": "HIGH",
                                        "assigneeIds": [1, 2, 3]
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Задача успешно создана",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Задача успешно создана\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на выполнение данной операции\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректные данные в запросе",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"field\": \"error\"}")
                            )
                    )
            }
    )
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskCreateRequest taskCreateRequest) {
        User author = userService.getCurrentUser();
        taskService.createTask(taskCreateRequest, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiMessageResponse("Задача успешно создана"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Изменить существующую задачу",
            description = "Этот метод позволяет изменить существующую задачу. Доступен только для пользователей с ролью ADMIN",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для изменения задачи",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TaskEditRequest.class),
                            examples = @ExampleObject(
                                    name = "Пример запроса",
                                    value = """
                                    {
                                        "title": "Обновленное название",
                                        "description": "Обновленное описание",
                                        "priority": "HIGH",
                                        "status": "IN_PROGRESS",
                                        "assigneeIds": [1, 2, 3]
                                    }
                                    """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Задача успешно изменена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Задача успешно изменена\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN",
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
                                    schema = @Schema(example = "{\"message\": \"Такой задачи не существует\"}")
                            )
                    )
            }
    )
    @PatchMapping("/edit/{id}")
    public ResponseEntity<?> editTask(@PathVariable Long id, @Valid @RequestBody TaskEditRequest taskEditRequest) {
        taskService.editTask(id, taskEditRequest);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Задача успешно изменена"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Удалить существующую задачу",
            description = "Этот метод позволяет удалить существующую задачу. Доступен только для пользователей с ролью ADMIN",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Задача успешно удалена",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Задача успешно удалена\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN",
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
                                    schema = @Schema(example = "{\"message\": \"Такой задачи не существует\"}")
                            )
                    )
            }
    )
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Задача успешно удалена"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Получить задачи определённого автора",
            description = "Этот метод позволяет получить список задач существующего автора. Доступен только для пользователей с ролью ADMIN",
            parameters = {
                    @Parameter(name = "authorId", description = "Идентификатор автора",
                            required = true, example = "1"),
                    @Parameter(name = "page", description = "Номер страницы",
                            required = true, example = "0"),
                    @Parameter(name = "size", description = "Количество задач на страницу",
                            required = true, example = "10"),
                    @Parameter(name = "priority", description = "Критерий для фильтрации задач",
                            required = false, example = "HIGH"),
                    @Parameter(name = "status", description = "Критерий для фильтрации задач",
                            required = false, example = "TODO")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список задач успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на выполнение данной операции\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Указанный пользователь не найден",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такого пользователя не существует\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Пользователь по которому идёт поиск не является админом",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Данный пользователь не является администратором и не может создавать задачи\"}")
                            )
                    )
            }
    )
    @GetMapping("/author")
    public ResponseEntity<Page<TaskResponse>> getTaskByAuthor(
            @RequestParam long authorId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskStatus status) {
        Page<TaskResponse> tasks = taskService.getTasksByAuthor(authorId, priority, status, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }

    @Operation(
            summary = "Получить задачи определённого исполнителя",
            description = "Этот метод позволяет получить список задач существующего исполнителя. Доступен только для пользователей с ролью ADMIN и для просмотра собственных задач исполнителем.",
            parameters = {
                    @Parameter(name = "assigneeId", description = "Идентификатор исполнителя",
                            required = true, example = "1"),
                    @Parameter(name = "page", description = "Номер страницы",
                            required = true, example = "0"),
                    @Parameter(name = "size", description = "Количество задач на страницу",
                            required = true, example = "10"),
                    @Parameter(name = "priority", description = "Критерий для фильтрации задач",
                            required = false, example = "HIGH"),
                    @Parameter(name = "status", description = "Критерий для фильтрации задач",
                            required = false, example = "TODO")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список задач успешно получен",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN или исполнитель указал чужой айди",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на просмотр данной информации\"}")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Указанный пользователь не найден",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"Такого пользователя не существует\"}")
                            )
                    )
            }
    )
    @GetMapping("/assignee")
    public ResponseEntity<Page<TaskResponse>> getTasksByAssignee(
            @RequestParam long assigneeId,
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskStatus status
    ) {
        Page<TaskResponse> tasks = taskService.getTasksByAssignee(assigneeId, priority, status, page, size);
        return ResponseEntity.status(HttpStatus.OK).body(tasks);
    }


    @Operation(
            summary = "Изменить статус задачи",
            description = "Этот метод позволяет изменить статус задачи по идентификатору. Доступен для пользователей с ролью ADMIN и исполнителей задачи",
            parameters = {
                    @Parameter(name = "id", description = "Идентификатор задачи",
                            required = true, example = "1")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешное получение задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = TaskResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Доступ запрещён, пользователь не имеет прав ADMIN или не является исполнителем задачи",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(example = "{\"message\": \"У вас нет прав на изменение статуса этой задачи\"}")
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
    @PatchMapping("/status/{id}")
    public ResponseEntity<ApiMessageResponse> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody TaskStatusUpdateRequest taskStatusUpdateRequest
    ) {
        taskService.updateTaskStatus(id, taskStatusUpdateRequest.getStatus());
        return ResponseEntity.status(HttpStatus.OK).body(new ApiMessageResponse("Статус задачи успешно изменён"));
    }
}
