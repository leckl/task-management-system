package org.example.taskmanagementsystem.services;

import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.dto.*;
import org.example.taskmanagementsystem.exception.*;
import org.example.taskmanagementsystem.models.*;
import org.example.taskmanagementsystem.repositories.CommentRepository;
import org.example.taskmanagementsystem.repositories.TaskRepository;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class TaskService {
    private TaskRepository taskRepository;
    private UserService userService;
    private UserRepository userRepository;
    private CommentRepository commentRepository;

    public List<TaskResponse> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream().map(this::mapToTaskResponse).toList();
    }

    public TaskResponse getTaskById(long taskId) {
        Task task = getTask(taskId);
        boolean hasAccess = hasTaskAccess(taskId);

        if (hasAccess) {
            return mapToTaskResponse(task);
        } else {
            throw new UnauthorizedAccessException("У вас не прав на просмотр данной задачи");
        }
    }

    public void createTask(TaskCreateRequest taskCreateRequest, User author) {
        Task task = new Task();
        task.setTitle(taskCreateRequest.getTitle());
        task.setDescription(taskCreateRequest.getDescription());
        task.setPriority(taskCreateRequest.getPriority());
        task.setStatus(TaskStatus.TODO);
        task.setAuthor(author);

        Set<User> assignees = userRepository.findAllById(taskCreateRequest.getAssigneeIds()).stream().collect(Collectors.toSet());

        if (assignees.isEmpty()) {
            throw new TaskCreationException("Не найдено ни одного пользователя для назначения задачи");
        }
        task.setAssignees(assignees);

        taskRepository.save(task);
    }

    public void editTask( Long taskId, TaskEditRequest taskEditRequest) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Такой задачи не существует"));

        if (taskEditRequest.getTitle() != null && !taskEditRequest.getTitle().isBlank()) {
            task.setTitle(taskEditRequest.getTitle());
        }

        if (taskEditRequest.getDescription() != null && !taskEditRequest.getDescription().isBlank()) {
            task.setDescription(taskEditRequest.getDescription());
        }

        if (taskEditRequest.getPriority() != null) {
            task.setPriority(taskEditRequest.getPriority());
        }

        if (taskEditRequest.getStatus() != null) {
            task.setStatus(taskEditRequest.getStatus());
        }

        if (taskEditRequest.getAssigneeIds() != null) {
            Set<User> users = taskEditRequest.getAssigneeIds().stream()
                .map(userId -> userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с айди " + userId + " не найден")))
                .collect(Collectors.toSet());

            task.setAssignees(users);
        }

        taskRepository.save(task);
    }

    public void deleteTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException("Задача с айди " + taskId + " не найдена");
        }

        taskRepository.deleteById(taskId);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByAuthor(Long authorId, TaskPriority priority, TaskStatus status, int page, int size) {
        User user = userRepository.findById(authorId)
            .orElseThrow(() -> new UserNotFoundException("Такого пользователя не существует"));

        if (!user.getRole().equals(Role.ADMIN)) {
            throw new UserIsNotAdminException("Данный пользователь не является администратором и не может создавать задачи");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> tasks;

        if (priority != null && status != null) {
            tasks = taskRepository.findByAuthorIdAndPriorityAndStatus(authorId, priority, status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByAuthorIdAndPriority(authorId, priority, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByAuthorIdAndStatus(authorId, status, pageable);
        } else {
            tasks = taskRepository.findByAuthorId(authorId, pageable);
        }

        return tasks.map(this::mapToTaskResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksByAssignee(Long assigneeId, TaskPriority priority, TaskStatus status, int page, int size) {
        if (!hasTaskListByAssigneeIdAccess(assigneeId)) {
            throw new UnauthorizedAccessException("У вас нет прав на просмотр данной информации");
        }

        if (!userRepository.existsById(assigneeId)) {
            throw new UserNotFoundException("Такого пользователя не существует");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Task> tasks;

        if (priority != null && status != null) {
            tasks = taskRepository.findByAssigneeIdAndPriorityAndStatus(assigneeId, priority, status, pageable);
        } else if (priority != null) {
            tasks = taskRepository.findByAssigneeIdAndPriority(assigneeId, priority, pageable);
        } else if (status != null) {
            tasks = taskRepository.findByAssigneeIdAndStatus(assigneeId, status, pageable);
        } else {
            tasks = taskRepository.findByAssigneeId(assigneeId, pageable);
        }

        return tasks.map(this::mapToTaskResponse);
    }

    public void updateTaskStatus(Long taskId, TaskStatus newStatus) {
        if (hasTaskAccess(taskId)) {
            Task task = getTask(taskId);

            task.setStatus(newStatus);
            taskRepository.save(task);
        } else {
            throw new UnauthorizedAccessException("У вас нет прав на изменение статуса этой задачи");
        }
    }

    public Task getTask(Long taskId) {
         return taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException("Такой задачи не существует"));
    }

    public boolean hasTaskListByAssigneeIdAccess(Long assigneeId) {
        User user = userService.getCurrentUser();

        return user.getRole().equals(Role.ADMIN) || user.getId().equals(assigneeId);
    }

    public boolean hasTaskAccess(Long taskId) {
        User currentUser = userService.getCurrentUser();
        Task task = getTask(taskId);

        boolean isUserAdmin = currentUser.getRole().equals(Role.ADMIN);
        boolean isUserAssigned = task.getAssignees().contains(currentUser);

        return isUserAssigned || isUserAdmin;
    }

    private TaskResponse mapToTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setPriority(task.getPriority());
        response.setStatus(task.getStatus());

        Set<UserResponse> assignees = task.getAssignees().stream()
            .map(user -> {
                UserResponse userResponse = new UserResponse();
                userResponse.setId(user.getId());
                userResponse.setEmail(user.getEmail());
                userResponse.setRole(user.getRole());
                return userResponse;
            }).collect(Collectors.toSet());

        response.setAssignees(assignees);
        UserResponse author = new UserResponse();
        author.setId(task.getAuthor().getId());
        author.setEmail(task.getAuthor().getEmail());
        author.setRole(task.getAuthor().getRole());

        response.setAuthor(author);

        List<CommentResponse> comments = commentRepository.findByTaskId(task.getId()).stream()
                .map(comment -> {
                    CommentResponse commentResponse = new CommentResponse();
                    commentResponse.setId(comment.getId());
                    commentResponse.setContent(comment.getContent());
                    commentResponse.setAuthorEmail(comment.getAuthor().getEmail());
                    return commentResponse;
                }).toList();

        response.setComments(comments);
        return response;
    }
}
