package org.example.taskmanagementsystem.controllers;

import org.example.taskmanagementsystem.config.SecurityConfig;
import org.example.taskmanagementsystem.dto.AuthRequest;
import org.example.taskmanagementsystem.exception.InvalidCredentialsException;
import org.example.taskmanagementsystem.security.JwtAuthenticationEntryPoint;
import org.example.taskmanagementsystem.security.JwtAuthenticationFilter;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.example.taskmanagementsystem.services.AuthService;
import org.example.taskmanagementsystem.services.UserService;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Import({SecurityConfig.class, JwtUtil.class, JwtAuthenticationEntryPoint.class, JwtAuthenticationFilter.class})
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @Test
    void register_shouldRegisterUser() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        ApiMessageResponse response = new ApiMessageResponse("Успешная регистрация");

        doNothing().when(authService).register(any(AuthRequest.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(response.getMessage()));

        verify(authService, times(1)).register(any(AuthRequest.class));
    }

    @Test
    void register_shouldReturnBadRequest() throws Exception {
        AuthRequest request = new AuthRequest("", "password");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "",
                                    "password": "password"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void login_shouldLoginUser() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        String token = "generated.jwt.token";
        ApiMessageResponse response = new ApiMessageResponse(token);

        when(authService.login(any(AuthRequest.class))).thenReturn(token);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(response.getMessage()));

        verify(authService, times(1)).login(any(AuthRequest.class));
    }

    @Test
    void login_shouldReturnBadRequest() throws Exception {
        AuthRequest request = new AuthRequest("test@example.com", "wrongPassword");

        when(authService.login(any(AuthRequest.class)))
                .thenThrow(new InvalidCredentialsException("Неправильная почта или пароль"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com",
                                    "password": "wrongPassword"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Неправильная почта или пароль"));

        verify(authService, times(1)).login(any(AuthRequest.class));
    }
}
