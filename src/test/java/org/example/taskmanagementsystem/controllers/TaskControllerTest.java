package org.example.taskmanagementsystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.taskmanagementsystem.config.SecurityConfig;
import org.example.taskmanagementsystem.dto.TaskCreateRequest;
import org.example.taskmanagementsystem.dto.TaskEditRequest;
import org.example.taskmanagementsystem.dto.TaskResponse;
import org.example.taskmanagementsystem.dto.UserResponse;
import org.example.taskmanagementsystem.exception.TaskNotFoundException;
import org.example.taskmanagementsystem.exception.UnauthorizedAccessException;
import org.example.taskmanagementsystem.exception.UserIsNotAdminException;
import org.example.taskmanagementsystem.exception.UserNotFoundException;
import org.example.taskmanagementsystem.models.*;
import org.example.taskmanagementsystem.security.JwtAuthenticationEntryPoint;
import org.example.taskmanagementsystem.security.JwtAuthenticationFilter;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.example.taskmanagementsystem.services.TaskService;
import org.example.taskmanagementsystem.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Import({SecurityConfig.class, JwtUtil.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class})
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc
public class TaskControllerTest {
    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private UserService userService;

    @InjectMocks
    private TaskController taskController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    @WithMockUser(username = "admin@mail.com", roles = { "ADMIN" })
    void getAllTasks_shouldReturnAllTasks() throws Exception {
        UserResponse author = createUser(1L, "author@example.com", Role.ADMIN);

        UserResponse assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        UserResponse assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        TaskResponse taskResponse = new TaskResponse(1L, "Task 1", "Description 1",
                TaskPriority.HIGH, TaskStatus.TODO, Collections.emptyList(), Set.of(assignee1, assignee2), author);
        List<TaskResponse> tasks = List.of(taskResponse);

        when(taskService.getAllTasks()).thenReturn(tasks);

        mockMvc.perform(get("/task"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Task 1"))
                .andExpect(jsonPath("$[0].description").value("Description 1"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].assignees", hasSize(2)))
                .andExpect(jsonPath("$[0].assignees[*].email", containsInAnyOrder("assignee1@example.com", "assignee2@example.com")))
                .andExpect(jsonPath("$[0].author.email").value("author@example.com"))
                .andExpect(jsonPath("$[0].author.role").value("ADMIN"));

        verify(taskService, times(1)).getAllTasks();
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void getAllTasks_shouldReturnAccessDeniedException() throws Exception {
        mockMvc.perform(get("/task"))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(0)).getAllTasks();
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTaskById_shouldReturnTask() throws Exception {
        UserResponse author = createUser(1L, "author@example.com", Role.ADMIN);
        UserResponse assignee = createUser(2L, "assignee@example.com", Role.USER);

        TaskResponse taskResponse = new TaskResponse(1L, "Task 1", "Description 1",
                TaskPriority.HIGH, TaskStatus.TODO, Collections.emptyList(), Set.of(assignee), author);

        when(taskService.getTaskById(1L)).thenReturn(taskResponse);

        mockMvc.perform(get("/task/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Task 1"))
                .andExpect(jsonPath("$.description").value("Description 1"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.assignees", hasSize(1)))
                .andExpect(jsonPath("$.assignees[0].email").value("assignee@example.com"))
                .andExpect(jsonPath("$.author.email").value("author@example.com"))
                .andExpect(jsonPath("$.author.role").value("ADMIN"));

        verify(taskService, times(1)).getTaskById(1L);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTaskById_shouldReturnNotFoundException() throws Exception {
        long taskId = 1;
        when(taskService.getTaskById(taskId)).thenThrow(new TaskNotFoundException("Такой задачи не существует"));

        mockMvc.perform(get("/task/{id}", taskId))
                .andExpect(status().isNotFound());

        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void getTaskById_shouldReturnAccessDeniedException() throws Exception {
        long taskId = 1L;

        when(taskService.getTaskById(taskId)).thenThrow(new UnauthorizedAccessException("У вас нет прав на просмотр данной задачи"));

        mockMvc.perform(get("/task/{id}", taskId))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(1)).getTaskById(taskId);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void createTask_shouldCreateTask() throws Exception {
        TaskCreateRequest taskCreateRequest = new TaskCreateRequest("Title", "Description",
                TaskPriority.MEDIUM, Set.of(1L, 2L));

        User author = new User();
        author.setId(1L);
        author.setEmail("author@example.com");
        author.setRole(Role.ADMIN);

        when(userService.getCurrentUser()).thenReturn(author);

        doNothing().when(taskService).createTask(taskCreateRequest, author);

        mockMvc.perform(post("/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Задача успешно создана"));

        verify(userService, times(1)).getCurrentUser();
        verify(taskService, times(1)).createTask(taskCreateRequest, author);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void createTask_shouldReturnBadRequestException() throws Exception {
        TaskCreateRequest taskCreateRequest = new TaskCreateRequest("", "Description",
                TaskPriority.MEDIUM, Set.of(1L, 2L));

        mockMvc.perform(post("/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskCreateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Название задачи не может быть пустым"));
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void createTask_shouldReturnAccessDeniedException() throws Exception {
        TaskCreateRequest taskCreateRequest = new TaskCreateRequest("Title", "Description",
                TaskPriority.MEDIUM, Set.of(1L, 2L));

        User author = new User();
        author.setId(1L);
        author.setEmail("author@example.com");
        author.setRole(Role.ADMIN);

        mockMvc.perform(post("/task/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskCreateRequest)))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(0)).createTask(taskCreateRequest, author);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void editTask_shouldEditTask() throws Exception {
        User author = new User();
        author.setId(1L);
        author.setEmail("author@example.com");
        author.setRole(Role.ADMIN);

        User assignee = new User();
        assignee.setId(2L);
        assignee.setEmail("assignee@example.com");
        assignee.setRole(Role.USER);

        Task task = createTask(1L, "Title", "Description",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee));

        TaskEditRequest taskEditRequest = new TaskEditRequest();
        taskEditRequest.setStatus(TaskStatus.COMPLETED);

        doNothing().when(taskService).editTask(task.getId(), taskEditRequest);

        mockMvc.perform(patch("/task/edit/{id}", task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskEditRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Задача успешно изменена"));

        verify(taskService, times(1)).editTask(task.getId(), taskEditRequest);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void editTask_shouldReturnNotFoundException() throws Exception {
        long taskId = 1;
        TaskEditRequest taskEditRequest = new TaskEditRequest();
        taskEditRequest.setStatus(TaskStatus.COMPLETED);

        doThrow(new TaskNotFoundException("Такой задачи не существует"))
                .when(taskService).editTask(taskId, taskEditRequest);

        mockMvc.perform(patch("/task/edit/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskEditRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Такой задачи не существует"));

        verify(taskService, times(1)).editTask(taskId, taskEditRequest);
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void editTask_shouldReturnAccessDeniedException() throws Exception {
        long taskId = 1;
        TaskEditRequest taskEditRequest = new TaskEditRequest();
        taskEditRequest.setStatus(TaskStatus.COMPLETED);

        mockMvc.perform(patch("/task/edit/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(taskEditRequest)))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(0)).editTask(taskId, taskEditRequest);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void deleteTask_shouldDeleteTask() throws Exception {
        long taskId = 1;

        mockMvc.perform(delete("/task/delete/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Задача успешно удалена"));

        verify(taskService, times(1)).deleteTask(taskId);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void deleteTask_shouldReturnNotFoundException() throws Exception {
        long taskId = 1;

        doThrow(new TaskNotFoundException("Такой задачи не сцществует")).when(taskService).deleteTask(taskId);

        mockMvc.perform(delete("/task/delete/{id}", taskId))
                .andExpect(status().isNotFound());

        verify(taskService, times(1)).deleteTask(taskId);
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void deleteTask_shouldReturnAccessDeniedException() throws Exception {
        long taskId = 1;

        mockMvc.perform(delete("/task/delete/{id}", taskId))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(0)).deleteTask(taskId);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTasksByAuthor_shouldReturnTasks() throws Exception {
        long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        UserResponse author = createUser(1L, "author@example.com", Role.ADMIN);

        UserResponse assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        UserResponse assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        List<TaskResponse> tasks = List.of(
                new TaskResponse(1L, "Task 1", "Description 1", TaskPriority.MEDIUM,
                        TaskStatus.IN_PROGRESS, Collections.emptyList(), Set.of(assignee1, assignee2), author),
                new TaskResponse(2L, "Task 2", "Description 2", TaskPriority.MEDIUM,
                        TaskStatus.IN_PROGRESS, Collections.emptyList(), Set.of(assignee1), author)
        );

        Page<TaskResponse> taskPage = new PageImpl<>(tasks);

        when(taskService.getTasksByAuthor(authorId, priority, status, page, size))
                .thenReturn(taskPage);

        mockMvc.perform(get("/task/author")
                .param("authorId", String.valueOf(authorId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.content[0].description").value("Description 1"))
                .andExpect(jsonPath("$.content[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].author.email").value("author@example.com"))
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].title").value("Task 2"))
                .andExpect(jsonPath("$.content[1].description").value("Description 2"))
                .andExpect(jsonPath("$.content[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.content[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[1].author.email").value("author@example.com"));

        verify(taskService, times(1))
                .getTasksByAuthor(authorId, priority, status, page, size);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTasksByAuthor_shouldReturnUserNotFoundException() throws Exception {
        long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        when(taskService.getTasksByAuthor(authorId, priority, status, page, size))
                .thenThrow(new UserNotFoundException("Такого пользователя не существует"));

        mockMvc.perform(get("/task/author")
                .param("authorId", String.valueOf(authorId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTasksByAuthor_shouldReturnUserIsNotAdmin() throws Exception {
        long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        when(taskService.getTasksByAuthor(authorId, priority, status, page, size))
                .thenThrow(new UserIsNotAdminException("Данный пользователь не является администратором и не может создавать задачи"));

        mockMvc.perform(get("/task/author")
                .param("authorId", String.valueOf(authorId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void getTasksByAuthor_shouldReturnAccessDeniedException() throws Exception {
        long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        UserResponse author = createUser(1L, "author@example.com", Role.ADMIN);

        mockMvc.perform(get("/task/author")
                .param("authorId", String.valueOf(authorId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTasksByAssignee_shouldReturnTasks() throws Exception {
        long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        UserResponse author = createUser(1L, "author@example.com", Role.ADMIN);

        UserResponse assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        UserResponse assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        List<TaskResponse> tasks = List.of(
                new TaskResponse(1L, "Task 1", "Description 1", TaskPriority.MEDIUM,
                        TaskStatus.IN_PROGRESS, Collections.emptyList(), Set.of(assignee1, assignee2), author),
                new TaskResponse(2L, "Task 2", "Description 2", TaskPriority.MEDIUM,
                        TaskStatus.IN_PROGRESS, Collections.emptyList(), Set.of(assignee1), author)
        );

        Page<TaskResponse> taskPage = new PageImpl<>(tasks);

        when(taskService.getTasksByAssignee(assigneeId, priority, status, page, size))
                .thenReturn(taskPage);

        mockMvc.perform(get("/task/assignee")
                .param("assigneeId", String.valueOf(assigneeId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].title").value("Task 1"))
                .andExpect(jsonPath("$.content[0].description").value("Description 1"))
                .andExpect(jsonPath("$.content[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[0].author.email").value("author@example.com"))
                .andExpect(jsonPath("$.content[1].id").value(2L))
                .andExpect(jsonPath("$.content[1].title").value("Task 2"))
                .andExpect(jsonPath("$.content[1].description").value("Description 2"))
                .andExpect(jsonPath("$.content[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$.content[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.content[1].author.email").value("author@example.com"));

        verify(taskService, times(1))
                .getTasksByAssignee(assigneeId, priority, status, page, size);
    }

    @Test
    @WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
    void getTasksByAssignee_shouldReturnUserNotFoundException() throws Exception {
        long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        when(taskService.getTasksByAssignee(assigneeId, priority, status, page, size))
                .thenThrow(new UserNotFoundException("Такого пользователя не существует"));

        mockMvc.perform(get("/task/assignee")
                .param("assigneeId", String.valueOf(assigneeId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isNotFound());

        verify(taskService, times(1))
                .getTasksByAssignee(assigneeId, priority, status, page, size);
    }

    @Test
    @WithMockUser(username = "user@mail.com", roles = {"USER"})
    void getTasksByAssignee_shouldReturnAccessDeniedException() throws Exception {
        long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.MEDIUM;
        TaskStatus status = TaskStatus.IN_PROGRESS;

        when(taskService.getTasksByAssignee(assigneeId, priority, status, page, size))
                .thenThrow(new UnauthorizedAccessException("У вас нет прав на просмотр данной информации"));

        mockMvc.perform(get("/task/assignee")
                .param("assigneeId", String.valueOf(assigneeId))
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size))
                .param("priority", priority.toString())
                .param("status", status.toString()))
                .andExpect(status().isUnauthorized());

        verify(taskService, times(1))
                .getTasksByAssignee(assigneeId, priority, status, page, size);
    }

    private UserResponse createUser(Long id, String email, Role role) {
        UserResponse user = new UserResponse();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private Task createTask(Long id, String title, String description, TaskPriority priority, TaskStatus status, User author, Set<User> assignees) {
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        task.setAuthor(author);
        task.setAssignees(assignees);
        return task;
    }
}
