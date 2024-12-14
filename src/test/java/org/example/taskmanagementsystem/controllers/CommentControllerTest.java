package org.example.taskmanagementsystem.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.taskmanagementsystem.config.SecurityConfig;
import org.example.taskmanagementsystem.dto.CommentCreateRequest;
import org.example.taskmanagementsystem.dto.CommentEditRequest;
import org.example.taskmanagementsystem.dto.CommentResponse;
import org.example.taskmanagementsystem.exception.CommentNotFoundException;
import org.example.taskmanagementsystem.exception.TaskNotFoundException;
import org.example.taskmanagementsystem.exception.UnauthorizedAccessException;
import org.example.taskmanagementsystem.security.JwtAuthenticationEntryPoint;
import org.example.taskmanagementsystem.security.JwtAuthenticationFilter;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.example.taskmanagementsystem.services.CommentService;
import org.example.taskmanagementsystem.services.TaskService;
import org.example.taskmanagementsystem.services.UserService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Import({SecurityConfig.class, JwtUtil.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class})
@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc
@WithMockUser(username = "admin@mail.com", roles = {"ADMIN"})
public class CommentControllerTest {
    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TaskService taskService;

    @InjectMocks
    private CommentController commentController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void getCommentsForTask_shouldReturnComments() throws Exception {
        long taskId = 1L;
        CommentResponse comment1 = createCommentResponse(1L, "First comment",
                "author@mail.com");
        CommentResponse comment2 = createCommentResponse(2L, "Second comment",
                "author@mail.com");

        when(commentService.getCommentsForTask(taskId)).thenReturn(List.of(comment1, comment2));

        mockMvc.perform(get("/comment/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(commentService, times(1)).getCommentsForTask(taskId);
    }

    @Test
    void getCommentsForTask_shouldReturnTaskNotFoundException() throws Exception {
        long taskId = 1L;

        when(commentService.getCommentsForTask(taskId)).thenThrow(TaskNotFoundException.class);

        mockMvc.perform(get("/comment/{taskId}", taskId))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).getCommentsForTask(taskId);
    }

    @Test
    void getCommentsForTask_shouldReturnUnauthorizedAccessException() throws Exception {
        long taskId = 1L;

        when(commentService.getCommentsForTask(taskId)).thenThrow(UnauthorizedAccessException.class);

        mockMvc.perform(get("/comment/{taskId}", taskId))
                .andExpect(status().isUnauthorized());

        verify(commentService, times(1)).getCommentsForTask(taskId);
    }

    @Test
    void createComment_shouldCreateComment() throws Exception {
        long taskId = 1L;
        CommentCreateRequest commentCreateRequest = new CommentCreateRequest();
        commentCreateRequest.setContent("First comment");

        mockMvc.perform(post("/comment/create/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentCreateRequest)))
                .andExpect(status().isCreated());

        verify(commentService, times(1)).createComment(taskId, commentCreateRequest);
    }

    @Test
    void createComment_shouldReturnTaskNotFoundException() throws Exception {
        long taskId = 1L;
        CommentCreateRequest commentCreateRequest = new CommentCreateRequest();
        commentCreateRequest.setContent("First comment");

        doThrow(new TaskNotFoundException("Такой задачи не существует"))
                .when(commentService)
                .createComment(taskId, commentCreateRequest);

        mockMvc.perform(post("/comment/create/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentCreateRequest)))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).createComment(taskId, commentCreateRequest);
    }

    @Test
    void createComment_shouldReturnBadRequestException() throws Exception {
        long taskId = 1L;
        CommentCreateRequest commentCreateRequest = new CommentCreateRequest();
        commentCreateRequest.setContent("");

        mockMvc.perform(post("/comment/create/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentCreateRequest)))
                .andExpect(status().isBadRequest());

        verify(commentService, times(0)).createComment(taskId, commentCreateRequest);
    }

    @Test
    void createComment_shouldReturnUnauthorizedAccessException() throws Exception {
        long taskId = 1L;
        CommentCreateRequest commentCreateRequest = new CommentCreateRequest();
        commentCreateRequest.setContent("First comment");

        doThrow(new UnauthorizedAccessException("У вас нет прав на публикацию комментариев к этой задаче"))
                .when(commentService).createComment(taskId, commentCreateRequest);

        mockMvc.perform(post("/comment/create/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentCreateRequest)))
                .andExpect(status().isUnauthorized());

        verify(commentService, times(1)).createComment(taskId, commentCreateRequest);
    }

    @Test
    void editComment_shouldEditComment() throws Exception {
        long taskId = 1L;
        CommentEditRequest commentEditRequest = new CommentEditRequest();
        commentEditRequest.setContent("First edited comment");

        mockMvc.perform(patch("/comment/edit/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentEditRequest)))
                .andExpect(status().isOk());

        verify(commentService, times(1)).editComment(taskId, commentEditRequest);
    }

    @Test
    void editComment_shouldReturnCommentNotFoundException() throws Exception {
        long taskId = 1L;
        CommentEditRequest commentEditRequest = new CommentEditRequest();
        commentEditRequest.setContent("First edited comment");

        doThrow(new CommentNotFoundException("Такой комментарий не существует"))
                .when(commentService).editComment(taskId, commentEditRequest);

        mockMvc.perform(patch("/comment/edit/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentEditRequest)))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).editComment(taskId, commentEditRequest);
    }

    @Test
    void editComment_shouldReturnUnauthorizedAccessException() throws Exception {
        long taskId = 1L;

        CommentEditRequest commentEditRequest = new CommentEditRequest();
        commentEditRequest.setContent("First edited comment");

        doThrow(new UnauthorizedAccessException("У вас нет прав на изменение данного комментария"))
                .when(commentService).editComment(taskId, commentEditRequest);

        mockMvc.perform(patch("/comment/edit/{id}", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(commentEditRequest)))
                .andExpect(status().isUnauthorized());

        verify(commentService, times(1)).editComment(taskId, commentEditRequest);
    }

    @Test
    void deleteComment_shouldDeleteComment() throws Exception {
        long taskId = 1L;

        mockMvc.perform(delete("/comment/delete/{id}", taskId))
                .andExpect(status().isOk());

        verify(commentService, times(1)).deleteComment(taskId);
    }

    @Test
    void deleteComment_shouldReturnCommentNotFoundException() throws Exception {
        long taskId = 1L;

        doThrow(new CommentNotFoundException("Такой комментарий не существует"))
                .when(commentService).deleteComment(taskId);

        mockMvc.perform(delete("/comment/delete/{id}", taskId))
                .andExpect(status().isNotFound());

        verify(commentService, times(1)).deleteComment(taskId);
    }

    @Test
    void deleteComment_shouldReturnUnauthorizedAccessException() throws Exception {
        long taskId = 1L;

        doThrow(new UnauthorizedAccessException("У вас нет прав на удаление данного комментария"))
                .when(commentService).deleteComment(taskId);

        mockMvc.perform(delete("/comment/delete/{id}", taskId))
                .andExpect(status().isUnauthorized());

        verify(commentService, times(1)).deleteComment(taskId);
    }

    private CommentResponse createCommentResponse(long id, String content, String authorEmail) {
        CommentResponse commentResponse = new CommentResponse();
        commentResponse.setId(id);
        commentResponse.setContent(content);
        commentResponse.setAuthorEmail(authorEmail);
        return commentResponse;
    }
}
