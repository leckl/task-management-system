package org.example.taskmanagementsystem.services;

import org.example.taskmanagementsystem.dto.AuthRequest;
import org.example.taskmanagementsystem.dto.CommentCreateRequest;
import org.example.taskmanagementsystem.dto.CommentEditRequest;
import org.example.taskmanagementsystem.dto.CommentResponse;
import org.example.taskmanagementsystem.exception.*;
import org.example.taskmanagementsystem.models.*;
import org.example.taskmanagementsystem.repositories.CommentRepository;
import org.example.taskmanagementsystem.repositories.TaskRepository;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    @Spy
    private AuthService authService;

    @Test
    void register_shouldReturnAlreadyAuthenticatedException() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        doReturn(true).when(authService).isUserAuthenticated();

        AlreadyAuthenticatedException exception = assertThrows(
                AlreadyAuthenticatedException.class,
                () -> authService.register(request)
        );
        assertEquals("Вы уже авторизованы", exception.getMessage());
        verify(userRepository, times(0)).findByEmail(anyString());
    }

    @Test
    void register_shouldReturnEmailAlreadyTakenException() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        doReturn(false).when(authService).isUserAuthenticated();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(new User()));

        EmailAlreadyTakenException exception = assertThrows(
                EmailAlreadyTakenException.class,
                () -> authService.register(request)
        );
        assertEquals("Такая почта уже занята", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
    }

    @Test
    void register_shouldRegisterUser() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);

        doReturn(false).when(authService).isUserAuthenticated();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");

        authService.register(request);

        verify(userRepository, times(1)).save(argThat(savedUser ->
                savedUser.getEmail().equals(user.getEmail()) &&
                        savedUser.getPassword().equals("encodedPassword") &&
                        savedUser.getRole() == Role.USER
        ));
    }

    @Test
    void login_shouldReturnToken() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword("hashedPassword");
        String expectedToken = "generated.jwt.token";

        doReturn(false).when(authService).isUserAuthenticated();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail())).thenReturn(expectedToken);

        String actualToken = authService.login(request);

        assertEquals(expectedToken, actualToken);
        verify(jwtUtil, times(1)).generateToken(user.getEmail());
    }

    @Test
    void login_shouldReturnAlreadyAuthenticatedException() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        when(authService.isUserAuthenticated()).thenReturn(true);

        AlreadyAuthenticatedException exception = assertThrows(
                AlreadyAuthenticatedException.class,
                () -> authService.login(request)
        );
        assertEquals("Вы уже авторизованы", exception.getMessage());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_shouldReturnInvalidCredentialsExceptionWhenUserNotFound() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        doReturn(false).when(authService).isUserAuthenticated();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Неправильная почта или пароль", exception.getMessage());
        verify(userRepository, times(1)).findByEmail(request.getEmail());
    }

    @Test
    void login_shouldReturnInvalidCredentialsExceptionWhenPasswordDoesNotMatch() {
        AuthRequest request = new AuthRequest("test@example.com", "password");
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword("hashedPassword");

        doReturn(false).when(authService).isUserAuthenticated();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );
        assertEquals("Неправильная почта или пароль", exception.getMessage());
        verify(passwordEncoder, times(1)).matches(request.getPassword(), user.getPassword());
    }
}