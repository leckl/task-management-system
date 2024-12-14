package org.example.taskmanagementsystem.services;

import org.example.taskmanagementsystem.dto.CommentCreateRequest;
import org.example.taskmanagementsystem.dto.CommentEditRequest;
import org.example.taskmanagementsystem.dto.CommentResponse;
import org.example.taskmanagementsystem.exception.CommentNotFoundException;
import org.example.taskmanagementsystem.exception.TaskNotFoundException;
import org.example.taskmanagementsystem.exception.UnauthorizedAccessException;
import org.example.taskmanagementsystem.models.*;
import org.example.taskmanagementsystem.repositories.CommentRepository;
import org.example.taskmanagementsystem.repositories.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {
    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TaskService taskService;

    @Mock
    private UserService userService;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private CommentService commentService;

    @Test
    void getCommentsForTask_shouldReturnMappedComments() {
        User author = createUser(1L, "author@example.com", Role.ADMIN);
        Task task = createTask(2L, "Test Task", "Task description", TaskPriority.HIGH, TaskStatus.TODO, author, Set.of(author));

        Comment comment1 = createComment(1L, "First comment", task, author);
        Comment comment2 = createComment(2L, "Second comment", task, author);

        when(taskRepository.existsById(task.getId())).thenReturn(true);
        when(taskService.hasTaskAccess(task.getId())).thenReturn(true);
        when(commentRepository.findByTaskId(task.getId())).thenReturn(List.of(comment1, comment2));

        List<CommentResponse> result = commentService.getCommentsForTask(task.getId());

        assertNotNull(result);
        assertTrue(result instanceof List<CommentResponse>);
        assertEquals(2, result.size());
        assertEquals("First comment", result.get(0).getContent());
        assertEquals("Second comment", result.get(1).getContent());

        verify(taskService, times(1)).hasTaskAccess(task.getId());
        verify(commentRepository, times(1)).findByTaskId(task.getId());
    }

    @Test
    void getCommentsForTask_shouldReturnTaskNotFoundException() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(false);

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            commentService.getCommentsForTask(taskId);
        });

        assertEquals("Такой задачи не существует", exception.getMessage());

        verify(taskRepository, times(1)).existsById(taskId);
        verifyNoInteractions(commentRepository);
        verifyNoInteractions(taskService);
    }

    @Test
    void  getCommentsForTask_shouldReturnUnauthorizedException() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(taskService.hasTaskAccess(taskId)).thenReturn(false);

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            commentService.getCommentsForTask(taskId);
        });

        assertEquals("У вас нет прав на просмотр комментариев этой задачи", exception.getMessage());

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskService, times(1)).hasTaskAccess(taskId);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void createComment_shouldCreateComment() {
        long taskId = 1L;
        String content = "Test comment";

        User currentUser = createUser(1L, "user@example.com", Role.USER);
        Task task = createTask(taskId, "Test Task", "Description", TaskPriority.HIGH, TaskStatus.TODO, currentUser, Set.of(currentUser));

        CommentCreateRequest request = new CommentCreateRequest();
        request.setContent(content);

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(taskService.hasTaskAccess(taskId)).thenReturn(true);
        when(taskService.getTask(taskId)).thenReturn(task);
        when(userService.getCurrentUser()).thenReturn(currentUser);

        commentService.createComment(taskId, request);

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskService, times(1)).hasTaskAccess(taskId);
        verify(taskService, times(1)).getTask(taskId);
        verify(userService, times(1)).getCurrentUser();
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void createComment_shouldReturnTaskNotFoundException() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(false);

        TaskNotFoundException exception = assertThrows(TaskNotFoundException.class, () -> {
            commentService.getCommentsForTask(taskId);
        });

        assertEquals("Такой задачи не существует", exception.getMessage());

        verify(taskRepository, times(1)).existsById(taskId);
        verifyNoInteractions(commentRepository);
        verifyNoInteractions(taskService);
    }

    @Test
    void createComment_shouldReturnUnauthorizedException() {
        long taskId = 1L;

        when(taskRepository.existsById(taskId)).thenReturn(true);
        when(taskService.hasTaskAccess(taskId)).thenReturn(false);

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            commentService.getCommentsForTask(taskId);
        });

        assertEquals("У вас нет прав на просмотр комментариев этой задачи", exception.getMessage());

        verify(taskRepository, times(1)).existsById(taskId);
        verify(taskService, times(1)).hasTaskAccess(taskId);
        verifyNoInteractions(commentRepository);
    }

    @Test
    void editComment_shouldEditComment() {
        long commentId = 1L;
        String newContent = "Updated content";

        User currentUser = createUser(1L, "user@example.com", Role.USER);
        Comment comment = createComment(commentId, "Old content", null, currentUser);

        CommentEditRequest request = new CommentEditRequest();
        request.setContent(newContent);

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userService.getCurrentUser()).thenReturn(currentUser);

        commentService.editComment(commentId, request);

        assertEquals(newContent, comment.getContent());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void editComment_shouldReturnCommentNotFoundException() {
        long commentId = 1L;
        CommentEditRequest request = new CommentEditRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.empty());

        CommentNotFoundException exception = assertThrows(CommentNotFoundException.class, () -> {
            commentService.editComment(commentId, request);
        });

        assertEquals("Такой комментарий не существует", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void editComment_shouldReturnUnauthorizedException() {
        long commentId = 1L;

        User currentUser = createUser(1L, "user@example.com", Role.USER);
        User anotherUser = createUser(2L, "author@example.com", Role.USER);

        Comment comment = createComment(commentId, "Old content", null, anotherUser);

        CommentEditRequest request = new CommentEditRequest();
        request.setContent("Updated content");

        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(userService.getCurrentUser()).thenReturn(currentUser);

        UnauthorizedAccessException exception = assertThrows(UnauthorizedAccessException.class, () -> {
            commentService.editComment(commentId, request);
        });

        assertEquals("У вас нет прав на изменение данного комментария", exception.getMessage());
        verify(commentRepository, never()).save(any());
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

    private Comment createComment(Long id, String text, Task task, User author) {
        Comment comment = new Comment();
        comment.setId(id);
        comment.setContent(text);
        comment.setAuthor(author);
        comment.setTask(task);
        return comment;
    }
}
