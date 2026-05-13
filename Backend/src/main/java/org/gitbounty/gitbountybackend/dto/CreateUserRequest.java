package org.gitbounty.gitbountybackend.dto;

public record CreateUserRequest(String username, String email, String password) {
}

