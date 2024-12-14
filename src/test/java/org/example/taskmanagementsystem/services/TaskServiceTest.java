package org.example.taskmanagementsystem.services;

import org.example.taskmanagementsystem.dto.TaskCreateRequest;
import org.example.taskmanagementsystem.dto.TaskEditRequest;
import org.example.taskmanagementsystem.dto.TaskResponse;
import org.example.taskmanagementsystem.dto.UserResponse;
import org.example.taskmanagementsystem.exception.*;
import org.example.taskmanagementsystem.models.*;
import org.example.taskmanagementsystem.repositories.CommentRepository;
import org.example.taskmanagementsystem.repositories.TaskRepository;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    @Spy
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllTasks_shouldReturnMappedTaskResponses() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);

        User assignee = createUser(2L, "assignee@example.com", Role.USER);

        Task task1 = createTask(1L, "Task 1", "Description 1",
                TaskPriority.HIGH, TaskStatus.TODO, author, Set.of(assignee));
        Task task2 = createTask(2L, "Task 2", "Description 2",
                TaskPriority.LOW, TaskStatus.COMPLETED, author, Set.of());

        List<Task> mockTasks = List.of(task1, task2);

        when(taskRepository.findAll()).thenReturn(mockTasks);

        List<TaskResponse> result = taskService.getAllTasks();

        assertThat(result).hasSize(2);

        TaskResponse response1 = result.getFirst();
        assertThat(response1.getId()).isEqualTo(1L);
        assertThat(response1.getTitle()).isEqualTo("Task 1");
        assertThat(response1.getDescription()).isEqualTo("Description 1");
        assertThat(response1.getPriority()).isEqualTo(TaskPriority.HIGH);
        assertThat(response1.getStatus()).isEqualTo(TaskStatus.TODO);

        UserResponse authorResponse = response1.getAuthor();
        assertThat(authorResponse.getId()).isEqualTo(1L);
        assertThat(authorResponse.getEmail()).isEqualTo("author@example.com");
        assertThat(authorResponse.getRole()).isEqualTo(Role.ADMIN);

        assertThat(response1.getAssignees()).hasSize(1);
        UserResponse assigneeResponse = response1.getAssignees().iterator().next();
        assertThat(assigneeResponse.getId()).isEqualTo(2L);
        assertThat(assigneeResponse.getEmail()).isEqualTo("assignee@example.com");
        assertThat(assigneeResponse.getRole()).isEqualTo(Role.USER);

        TaskResponse response2 = result.get(1);
        assertThat(response2.getId()).isEqualTo(2L);
        assertThat(response2.getTitle()).isEqualTo("Task 2");
        assertThat(response2.getDescription()).isEqualTo("Description 2");
        assertThat(response2.getPriority()).isEqualTo(TaskPriority.LOW);
        assertThat(response2.getStatus()).isEqualTo(TaskStatus.COMPLETED);

        UserResponse authorResponse2 = response2.getAuthor();
        assertThat(authorResponse2.getId()).isEqualTo(1L);
        assertThat(authorResponse2.getEmail()).isEqualTo("author@example.com");
        assertThat(authorResponse2.getRole()).isEqualTo(Role.ADMIN);

        assertThat(response2.getAssignees()).isEmpty();

        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getAllTasks_shouldReturnEmptyTaskResponses() {
        when(taskRepository.findAll()).thenReturn(new ArrayList<>());

        List<TaskResponse> result = taskService.getAllTasks();
        assertThat(result).isEmpty();
        verify(taskRepository, times(1)).findAll();
    }

    @Test
    void getTaskById_shouldReturnMappedTaskResponse() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);

        User assignee = createUser(2L, "assignee@example.com", Role.USER);

        Task task = createTask(3L, "Task 3", "Description 3",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee));

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        doReturn(true).when(taskService).hasTaskAccess(task.getId());

        TaskResponse result = taskService.getTaskById(3L);

        assertNotNull(result);
        assertTrue(result instanceof TaskResponse);

        verify(taskRepository, times(1)).findById(3L);
        verify(taskService, times(1)).hasTaskAccess(task.getId());
    }

    @Test
    void getTaskById_shouldReturnTaskNotFoundException() {
        long id = 3L;
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.getTaskById(id);
        });

        assertEquals("Такой задачи не существует", exception.getMessage());
        verify(taskRepository, times(1)).findById(id);
    }

    @Test
    void getTaskById_shouldReturnUnauthorizedException() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);

        User assignee = createUser(2L, "assignee@example.com", Role.USER);

        Task task = createTask(3L, "Task 3", "Description 3",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee));

        when(taskRepository.findById(3L)).thenReturn(Optional.of(task));
        doReturn(false).when(taskService).hasTaskAccess(task.getId());

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.getTaskById(3L);
        });

        assertEquals("У вас не прав на просмотр данной задачи", exception.getMessage());

        verify(taskRepository, times(1)).findById(3L);
        verify(taskService, times(1)).hasTaskAccess(task.getId());
    }

    @Test
    void createTask_shouldCreateTask() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        User assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        User assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        Set<Long> assigneeIds = Set.of(assignee1.getId(), assignee2.getId());

        TaskCreateRequest taskCreateRequest = new TaskCreateRequest("Task", "Description",
                TaskPriority.HIGH, assigneeIds);

        when(userRepository.findAllById(assigneeIds)).thenReturn(List.of(assignee1, assignee2));

        taskService.createTask(taskCreateRequest, author);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        assertNotNull(savedTask);
        assertEquals("Task", savedTask.getTitle());
        assertEquals("Description", savedTask.getDescription());
        assertEquals(TaskPriority.HIGH, savedTask.getPriority());
        assertEquals(TaskStatus.TODO, savedTask.getStatus());
        assertEquals(author, savedTask.getAuthor());
        assertTrue(savedTask.getAssignees().contains(assignee1));
        assertTrue(savedTask.getAssignees().contains(assignee2));
    }

    @Test
    void createTask_shouldReturnTaskCreationException() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        Set<Long> assigneeIds = Set.of(2L, 3L);

        TaskCreateRequest taskCreateRequest = new TaskCreateRequest("Task", "Description",
                TaskPriority.HIGH, assigneeIds);

        when(userRepository.findAllById(assigneeIds)).thenReturn(Collections.emptyList());

        TaskCreationException exception = assertThrows(TaskCreationException.class, () -> {
            taskService.createTask(taskCreateRequest, author);
        });

        assertEquals("Не найдено ни одного пользователя для назначения задачи", exception.getMessage());

        verify(taskRepository, times(0)).save(any(Task.class));
    }

    @Test
    void editTask_shouldFullEditTask() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        User assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        User assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        Task task = createTask(1L, "Task 3", "Description 3",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee1, assignee2));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        Set<Long> assigneeIds = Set.of(assignee1.getId());

        TaskEditRequest taskEditRequest = new TaskEditRequest("Task", "Description",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, assigneeIds);

        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee2));

        taskService.editTask(1L, taskEditRequest);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();

        assertNotNull(savedTask);
        assertEquals("Task", savedTask.getTitle());
        assertEquals("Description", savedTask.getDescription());
        assertEquals(TaskPriority.HIGH, savedTask.getPriority());
        assertEquals(TaskStatus.IN_PROGRESS, savedTask.getStatus());
        assertEquals(author, savedTask.getAuthor());
        assertTrue(savedTask.getAssignees().contains(assignee2));
    }

    @Test
    void editTask_shouldPartialEditTask() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        User assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        User assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        Task task = createTask(1L, "Task 3", "Description 3",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee1, assignee2));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        TaskEditRequest taskEditRequest = new TaskEditRequest();
        taskEditRequest.setTitle("Task");

        taskService.editTask(task.getId(), taskEditRequest);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository, times(1)).save(taskCaptor.capture());
        Task savedTask = taskCaptor.getValue();
        assertNotNull(savedTask);
        assertEquals("Task", savedTask.getTitle());
    }

    @Test
    void editTask_shouldReturnTaskNotFoundException() {
        long taskId = 1L;
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        User assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        User assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        Set<Long> assigneeIds = Set.of(assignee1.getId(), assignee2.getId());

        TaskEditRequest taskEditRequest = new TaskEditRequest("Task", "Description",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, assigneeIds);

        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.editTask(taskId, taskEditRequest);
        });

        assertEquals("Такой задачи не существует", exception.getMessage());

        verify(taskRepository, times(0)).save(any(Task.class));
    }

    @Test
    void editTask_shouldReturnUserNotFoundException() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        User assignee1 = createUser(2L, "assignee1@example.com", Role.USER);
        User assignee2 = createUser(3L, "assignee2@example.com", Role.USER);

        Task task = createTask(1L, "Task 3", "Description 3",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of(assignee1, assignee2));

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        Set<Long> assigneeIds = Set.of(assignee1.getId());

        TaskEditRequest taskEditRequest = new TaskEditRequest("Task", "Description",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS, assigneeIds);

        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            taskService.editTask(task.getId(), taskEditRequest);
        });

        assertEquals("Пользователь с айди " + assignee1.getId() + " не найден", exception.getMessage());

        verify(taskRepository, times(0)).save(any(Task.class));
    }

    @Test
    void deleteTask_shouldDeleteTask() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(true);

        taskService.deleteTask(taskId);

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(1)).deleteById(taskId);
    }

    @Test
    void deleteTask_shouldReturnTaskNotFoundException() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(false);

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.deleteTask(taskId);
        });

        assertEquals("Задача с айди " + taskId + " не найдена", exception.getMessage());

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskRepository, times(0)).deleteById(taskId);
    }

    @Test
    void getTasksByAuthor_shouldReturnTasksWhenValidAuthorAndAdmin() {
        Long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.HIGH;
        TaskStatus status = TaskStatus.TODO;

        User author = createUser(authorId, "admin@example.com", Role.ADMIN);

        Task task1 = createTask(1L, "Task 1", "Description 1", priority, status, author, Set.of());
        Task task2 = createTask(2L, "Task 2", "Description 2", priority, status, author, Set.of());

        Page<Task> tasksPage = new PageImpl<>(List.of(task1, task2));
        Pageable pageable = PageRequest.of(page, size);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(taskRepository.findByAuthorIdAndPriorityAndStatus(authorId, priority, status, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAuthor(authorId, priority, status, page, size);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(userRepository, times(1)).findById(authorId);
        verify(taskRepository, times(1)).findByAuthorIdAndPriorityAndStatus(authorId, priority, status, pageable);
    }

    @Test
    void getTasksByAuthor_shouldReturnUserNotFoundException() {
        Long authorId = 1L;

        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            taskService.getTasksByAuthor(authorId, null, null, 0, 10);
        });

        assertEquals("Такого пользователя не существует", exception.getMessage());
        verify(userRepository, times(1)).findById(authorId);
        verify(taskRepository, never()).findByAuthorId(anyLong(), any());
    }

    @Test
    void getTasksByAuthor_shouldReturnUserIsNotAdminException() {
        Long authorId = 1L;
        User user = createUser(authorId, "user@example.com", Role.USER);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(user));

        UserIsNotAdminException exception = assertThrows(UserIsNotAdminException.class, () -> {
            taskService.getTasksByAuthor(authorId, null, null, 0, 10);
        });

        assertEquals("Данный пользователь не является администратором и не может создавать задачи", exception.getMessage());
        verify(userRepository, times(1)).findById(authorId);
        verify(taskRepository, never()).findByAuthorId(anyLong(), any());
    }

    @Test
    void getTasksByAuthor_shouldReturnTasksWhenPriorityIsNullAndStatusIsNotNull() {
        Long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskStatus status = TaskStatus.TODO;

        User author = createUser(authorId, "admin@example.com", Role.ADMIN);

        Task task = createTask(1L, "Task 1", "Description 1", TaskPriority.MEDIUM, status, author, Set.of());
        Page<Task> tasksPage = new PageImpl<>(List.of(task));
        Pageable pageable = PageRequest.of(page, size);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(taskRepository.findByAuthorIdAndStatus(authorId, status, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAuthor(authorId, null, status, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1)).findById(authorId);
        verify(taskRepository, times(1)).findByAuthorIdAndStatus(authorId, status, pageable);
    }

    @Test
    void getTasksByAuthor_shouldReturnTasksWhenPriorityIsNotNullAndStatusIsNull() {
        Long authorId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.LOW;

        User author = createUser(authorId, "admin@example.com", Role.ADMIN);

        Task task = createTask(1L, "Task 1", "Description 1", priority, TaskStatus.TODO, author, Set.of());
        Page<Task> tasksPage = new PageImpl<>(List.of(task));
        Pageable pageable = PageRequest.of(page, size);

        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(taskRepository.findByAuthorIdAndPriority(authorId, priority, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAuthor(authorId, priority, null, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(userRepository, times(1)).findById(authorId);
        verify(taskRepository, times(1)).findByAuthorIdAndPriority(authorId, priority, pageable);
    }

    @Test
    void getTasksByAssignee_shouldReturnTasksWhenValidAssigneeAndAdmin() {
        Long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.HIGH;
        TaskStatus status = TaskStatus.TODO;

        User assignee = createUser(assigneeId, "assignee@example.com", Role.USER);

        when(userRepository.existsById(assigneeId)).thenReturn(true);

        doReturn(true).when(taskService).hasTaskListByAssigneeIdAccess(assigneeId);

        Task task1 = createTask(1L, "Task 1", "Description 1", priority, status, assignee, Set.of());
        Task task2 = createTask(2L, "Task 2", "Description 2", priority, status, assignee, Set.of());

        Page<Task> tasksPage = new PageImpl<>(List.of(task1, task2));
        Pageable pageable = PageRequest.of(page, size);

        when(taskRepository.findByAssigneeIdAndPriorityAndStatus(assigneeId, priority, status, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAssignee(assigneeId, priority, status, page, size);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Task 1", result.getContent().get(0).getTitle());
        assertEquals("Task 2", result.getContent().get(1).getTitle());

        verify(taskRepository, times(1)).findByAssigneeIdAndPriorityAndStatus(assigneeId, priority, status, pageable);
    }

    @Test
    void getTasksByAssignee_shouldReturnUserNotFoundException() {
        Long assigneeId = 1L;

        when(userRepository.findById(assigneeId)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            taskService.getTasksByAuthor(assigneeId, null, null, 0, 10);
        });

        assertEquals("Такого пользователя не существует", exception.getMessage());
        verify(userRepository, times(1)).findById(assigneeId);
    }

    @Test
    void getTasksByAssignee_shouldReturnUnauthorizedException() {
        Long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.HIGH;
        TaskStatus status = TaskStatus.TODO;

        User currentUser = createUser(2L, "user@example.com", Role.USER);

        when(userService.getCurrentUser()).thenReturn(currentUser);

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.getTasksByAssignee(assigneeId, priority, status, page, size);
        });

        assertEquals("У вас нет прав на просмотр данной информации", exception.getMessage());
    }


    @Test
    void getTasksByAssignee_shouldReturnTasksWhenPriorityIsNullAndStatusIsNotNull() {
        Long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskStatus status = TaskStatus.TODO;

        User author = createUser(assigneeId, "admin@example.com", Role.ADMIN);

        when(userRepository.existsById(assigneeId)).thenReturn(true);
        doReturn(true).when(taskService).hasTaskListByAssigneeIdAccess(assigneeId);

        Task task = createTask(1L, "Task 1", "Description 1", TaskPriority.MEDIUM, status, author, Set.of());
        Page<Task> tasksPage = new PageImpl<>(List.of(task));
        Pageable pageable = PageRequest.of(page, size);

        when(taskRepository.findByAssigneeIdAndStatus(assigneeId, status, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAssignee(assigneeId, null, status, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Task 1", result.getContent().get(0).getTitle());

        verify(taskRepository, times(1)).findByAssigneeIdAndStatus(assigneeId, status, pageable);
    }

    @Test
    void getTasksByAssignee_shouldReturnTasksWhenPriorityIsNotNullAndStatusIsNull() {
        Long assigneeId = 1L;
        int page = 0;
        int size = 10;
        TaskPriority priority = TaskPriority.LOW;

        User author = createUser(assigneeId, "admin@example.com", Role.ADMIN);

        when(userRepository.existsById(assigneeId)).thenReturn(true);
        doReturn(true).when(taskService).hasTaskListByAssigneeIdAccess(assigneeId);

        Task task = createTask(1L, "Task 1", "Description 1", priority, TaskStatus.TODO, author, Set.of());
        Page<Task> tasksPage = new PageImpl<>(List.of(task));
        Pageable pageable = PageRequest.of(page, size);

        when(taskRepository.findByAssigneeIdAndPriority(assigneeId, priority, pageable)).thenReturn(tasksPage);

        Page<TaskResponse> result = taskService.getTasksByAssignee(assigneeId, priority, null, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        verify(taskRepository, times(1)).findByAssigneeIdAndPriority(assigneeId, priority, pageable);
    }

    @Test
    void updateTaskStatus_shouldUpdateStatus() {
        long taskId = 1L;
        TaskStatus newStatus = TaskStatus.COMPLETED;

        User author = createUser(1L, "admin@example.com", Role.ADMIN);
        Task task = createTask(taskId, "Task", "Description",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, author, Set.of());

        doReturn(true).when(taskService).hasTaskAccess(taskId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.updateTaskStatus(taskId, newStatus);

        assertEquals(newStatus, task.getStatus());

        verify(taskService, times(1)).hasTaskAccess(taskId);
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    void updateTaskStatus_shouldReturnUnauthorizedAccessException() {
        long taskId = 1L;
        TaskStatus newStatus = TaskStatus.COMPLETED;

        doReturn(false).when(taskService).hasTaskAccess(taskId);

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            taskService.updateTaskStatus(taskId, newStatus);
        });

        assertEquals("У вас нет прав на изменение статуса этой задачи", exception.getMessage());

        verify(taskService, times(1)).hasTaskAccess(taskId);
        verify(taskRepository, times(0)).findById(taskId);
        verify(taskRepository, times(0)).save(any());
    }

    @Test
    void updateTaskStatus_shouldReturnTaskNotfoundException() {
        long taskId = 1L;
        TaskStatus newStatus = TaskStatus.COMPLETED;

        doReturn(true).when(taskService).hasTaskAccess(taskId);
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            taskService.updateTaskStatus(taskId, newStatus);
        });

        assertEquals("Такой задачи не существует", exception.getMessage());

        verify(taskService, times(1)).hasTaskAccess(taskId);
        verify(taskRepository, times(1)).findById(taskId);
        verify(taskRepository, times(0)).save(any());
    }

    private User createUser(Long id, String email, Role role) {
        User user = new User();
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