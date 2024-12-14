package org.example.taskmanagementsystem.services;

import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.dto.CommentCreateRequest;
import org.example.taskmanagementsystem.dto.CommentEditRequest;
import org.example.taskmanagementsystem.dto.CommentResponse;
import org.example.taskmanagementsystem.exception.CommentNotFoundException;
import org.example.taskmanagementsystem.exception.TaskNotFoundException;
import org.example.taskmanagementsystem.exception.UnauthorizedAccessException;
import org.example.taskmanagementsystem.models.Comment;
import org.example.taskmanagementsystem.models.Role;
import org.example.taskmanagementsystem.models.Task;
import org.example.taskmanagementsystem.models.User;
import org.example.taskmanagementsystem.repositories.CommentRepository;
import org.example.taskmanagementsystem.repositories.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    public List<CommentResponse> getCommentsForTask(long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException("Такой задачи не существует");
        }

        boolean hasAccess = taskService.hasTaskAccess(taskId);

        if (hasAccess) {
            List<Comment> comments = commentRepository.findByTaskId(taskId);
            return comments.stream().map(this::mapToResponse).collect(Collectors.toList());
        } else {
            throw new UnauthorizedAccessException("У вас нет прав на просмотр комментариев этой задачи");
        }
    }

    public void createComment(long taskId, CommentCreateRequest commentCreateRequest) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException("Такой задачи не существует");
        }

        boolean hasAccess = taskService.hasTaskAccess(taskId);

        if (hasAccess) {
            User currentUser = userService.getCurrentUser();
            Task task = taskService.getTask(taskId);
            Comment comment = new Comment();
            comment.setContent(commentCreateRequest.getContent());
            comment.setTask(task);
            comment.setAuthor(currentUser);

            commentRepository.save(comment);
        } else {
            throw new UnauthorizedAccessException("У вас нет прав на публикацию комментариев к этой задаче");
        }
    }

    public void editComment(long commentId, CommentEditRequest commentEditRequest) {
        User currentUser = userService.getCurrentUser();
        Comment comment = getComment(commentId);

        if (isAuthor(comment, currentUser)) {
            comment.setContent(commentEditRequest.getContent());
            commentRepository.save(comment);
        } else {
            throw new UnauthorizedAccessException("У вас нет прав на изменение данного комментария");
        }
    }

    public void deleteComment(long commentId) {
        User currentUser = userService.getCurrentUser();
        Comment comment = getComment(commentId);

        if (isAuthor(comment, currentUser)) {
            commentRepository.delete(comment);
        } else {
            throw new UnauthorizedAccessException("У вас нет прав на удаление данного комментария");
        }
    }

    private Comment getComment(long commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new CommentNotFoundException("Такой комментарий не существует"));
    }

    private boolean isAuthor(Comment comment, User user) {
        return comment.getAuthor() != null && comment.getAuthor().getId().equals(user.getId());
    }

    private CommentResponse mapToResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setAuthorEmail(comment.getAuthor().getEmail());
        return response;
    }
}
