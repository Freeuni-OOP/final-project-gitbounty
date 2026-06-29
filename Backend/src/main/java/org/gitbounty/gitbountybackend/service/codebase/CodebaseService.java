package org.gitbounty.gitbountybackend.service.codebase;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CodebaseService {

    private final CodebaseRepository codebaseRepository;
    private final CodebaseStorageService codebaseStorageService;
    private final UserService userService;

    public Codebase createCodebase(String name, String description, String gitUrl, String userId) {
        String repositoryName = normalize(name);
        User owner = userService.findByKeycloakId(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new IllegalStateException("Repository already exists");
        }
        codebaseStorageService.createRepository(repositoryName);

        try {
            return codebaseRepository.saveAndFlush(new Codebase(repositoryName, description == null ? null : description.trim(), gitUrl, owner));
        } catch (RuntimeException e) {
            try { codebaseStorageService.deleteRepository(repositoryName); }
            catch (RuntimeException cleanupError) { e.addSuppressed(cleanupError); }
            throw e;
        }
    }

    public Codebase getCodebase(String repositoryName) {
        String normalized = normalize(repositoryName);
        return codebaseRepository.findByName(normalized).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + normalized));
    }

    public Codebase findById(Long repositoryId) {
        return codebaseRepository.findById(repositoryId).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: Id = " + repositoryId));
    }

    public CodebaseContentsDTO listCodebaseContents(String repositoryName, String path, String branchName) {
        if (!codebaseRepository.existsByName(repositoryName)) {
            throw new CodebaseNotFoundException("Codebase not found: " + repositoryName);
        }
        return new CodebaseContentsDTO(codebaseStorageService.getPathContents(repositoryName, path, branchName));
    }

    public void deleteCodebase(String name) {
        String repositoryName = normalize(name);
        codebaseRepository.findByName(repositoryName).ifPresent(codebaseRepository::delete);
        codebaseStorageService.deleteRepository(repositoryName);
    }

    public Codebase findByName(String repositoryName) {
        return codebaseRepository.findByName(repositoryName).orElseThrow(() -> new CodebaseNotFoundException("Repository not found: " + repositoryName));
    }

    public List<Codebase> getAllCodebases() {
        return codebaseRepository.findAll();
    }

    public Codebase updateCodebase(String repositoryName, UpdateCodebaseCommand command) {
        Codebase codebase = getCodebase(repositoryName); //reuses the getCodebase logic to avoid duplicating the throw
        command.applyTo(codebase);
        return codebaseRepository.save(codebase);
    }

    private String normalize(String name) {
        if (name == null || name.trim().isBlank()) {
            throw new IllegalArgumentException("Repository name is required");
        }
        String trimmed = name.trim();
        if (trimmed.matches(".*[/\\\\].*") || trimmed.contains("..") || trimmed.contains(".git")) {
            throw new IllegalArgumentException("Invalid repository name format");
        }
        return trimmed;
    }
}