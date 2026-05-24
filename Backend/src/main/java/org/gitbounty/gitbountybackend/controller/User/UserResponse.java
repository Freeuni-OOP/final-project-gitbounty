package org.gitbounty.gitbountybackend.controller.User;

import java.time.LocalDateTime;

import org.gitbounty.gitbountybackend.model.User;

public record UserResponse(Long id, String username, String email, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }
}

