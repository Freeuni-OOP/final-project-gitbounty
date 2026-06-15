package org.gitbounty.gitbountybackend.controller.codebase;

// name should be one ending with .git
public record CreateCodebaseRequest(String name, String description) {
}

