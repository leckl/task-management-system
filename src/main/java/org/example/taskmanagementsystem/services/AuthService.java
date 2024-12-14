package org.example.taskmanagementsystem.services;

import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.dto.AuthRequest;
import org.example.taskmanagementsystem.exception.AlreadyAuthenticatedException;
import org.example.taskmanagementsystem.exception.EmailAlreadyTakenException;
import org.example.taskmanagementsystem.exception.InvalidCredentialsException;
import org.example.taskmanagementsystem.models.Role;
import org.example.taskmanagementsystem.models.User;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.example.taskmanagementsystem.security.JwtUtil;
import org.example.taskmanagementsystem.util.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtils;

    public void register(AuthRequest request) {
        if (isUserAuthenticated()) {
            throw new AlreadyAuthenticatedException("Вы уже авторизованы");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyTakenException("Такая почта уже занята");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        userRepository.save(user);
    }

    public String login(AuthRequest request) {
        if (isUserAuthenticated()) {
            throw new AlreadyAuthenticatedException("Вы уже авторизованы");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Неправильная почта или пароль"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Неправильная почта или пароль");
        }

        return jwtUtils.generateToken(user.getEmail());
    }

    public boolean isUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
