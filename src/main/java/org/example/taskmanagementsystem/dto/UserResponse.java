package org.example.taskmanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.taskmanagementsystem.models.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private long id;
    private String email;
    private Role role;
}
