package org.example.taskmanagementsystem.services;


import lombok.AllArgsConstructor;
import org.example.taskmanagementsystem.exception.UnauthorizedAccessException;
import org.example.taskmanagementsystem.models.Role;
import org.example.taskmanagementsystem.models.User;
import org.example.taskmanagementsystem.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetails loadUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Такого пользователя не существует"));
    }

    public void upgradeToAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        user.setRole(Role.ADMIN);
        userRepository.save(user);
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new UnauthorizedAccessException("Вы не авторизованы, пожалуйста войдите в аккаунт"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return loadUserByEmail(username);
    }
}