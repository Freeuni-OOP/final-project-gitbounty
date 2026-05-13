package org.gitbounty.gitbountybackend.service.codebase;

import java.nio.file.Path;
import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final UserRepository userRepository;
    private final Path resolveRepositoriesRoot;

    public CodebaseService(
        CodebaseRepository codebaseRepository,
        UserRepository userRepository,
        Path resolveRepositoriesRoot
    ) {
        this.codebaseRepository = codebaseRepository;
        this.userRepository = userRepository;
        this.resolveRepositoriesRoot = resolveRepositoriesRoot;
    }

    public Codebase createCodebase(String name, String description, String gitUrl, Principal principal) {
        String repositoryName = normalizeRepositoryName(name);
        User owner = resolveOwner(principal);

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Repository already exists: " + repositoryName);
        }

        AbstractCodebaseFactory codebaseFactory = new DiskCodebaseFactory(resolveRepositoriesRoot, codebaseRepository);
        return codebaseFactory.persistCodebase(repositoryName, normalizeDescription(description), gitUrl, owner);
    }

    private User resolveOwner(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        return userRepository.findByUsername(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
    }

    private String normalizeRepositoryName(String name) {
        if (name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }

        String trimmedName = name.trim();
        if (trimmedName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name is required");
        }

        if (trimmedName.contains("/") || trimmedName.contains("\\") || trimmedName.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Repository name must not contain path separators");
        }

        return trimmedName;
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

}







