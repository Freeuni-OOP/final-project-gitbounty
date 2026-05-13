package org.gitbounty.gitbountybackend.dto;

// name should be one ending with .git
public record CreateCodebaseRequest(String name, String description) {
}

