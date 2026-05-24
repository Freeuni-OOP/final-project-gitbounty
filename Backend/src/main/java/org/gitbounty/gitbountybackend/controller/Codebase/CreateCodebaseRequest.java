package org.gitbounty.gitbountybackend.controller.Codebase;

// name should be one ending with .git
public record CreateCodebaseRequest(String name, String description) {
}

