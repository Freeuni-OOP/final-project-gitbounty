package org.gitbounty.gitbountybackend.service.codebase;

import java.util.List;

import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.exception.CodebaseNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.storage.CodebaseStorageService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.model.Branch;

@Service
public class CodebaseService {

    private static final Logger log = LoggerFactory.getLogger(CodebaseService.class);

    private final CodebaseRepository codebaseRepository;
    private final CodebaseStorageService codebaseStorageService;
    private final UserService userService;
    private final BranchService branchService;
    private final CodebaseDeletionCascadeService codebaseDeletionCascadeService;

    public CodebaseService(CodebaseRepository codebaseRepository,
                            CodebaseStorageService codebaseStorageService,
                            UserService userService,
                            BranchService branchService,
                            CodebaseDeletionCascadeService codebaseDeletionCascadeService) {
        this.codebaseRepository = codebaseRepository;
        this.codebaseStorageService = codebaseStorageService;
        this.userService = userService;
        this.branchService = branchService;
        this.codebaseDeletionCascadeService = codebaseDeletionCascadeService;
    }

    public Codebase createCodebase(String name, String description, String gitUrl, String userId) {
        String repositoryName = normalize(name);
        User owner = userService.findByKeycloakId(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (codebaseRepository.findByName(repositoryName).isPresent()) {
            throw new IllegalStateException("Repository already exists");
        }

        codebaseStorageService.createRepository(repositoryName);

        try {
            Codebase codebase = codebaseRepository.saveAndFlush(new Codebase(repositoryName, description == null ? null : description.trim(), gitUrl, owner));
            branchService.createNewBranchForCodebase(codebase, Branch.DEFAULT_NAME);

            return codebase;
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

    /**
     * Deletes a repository entirely: the DB cascade (transactions detached, pull requests,
     * issues, branches, and the codebase row itself) in one transaction, then the bare git
     * repository directory on disk strictly after that transaction commits.
     *
     * The DB cascade itself lives in CodebaseDeletionCascadeService, a separate @Transactional
     * bean, rather than a method on this class. Calling an @Transactional method via "this."
     * (self-invocation) bypasses Spring's transactional proxy entirely, silently breaking the
     * DB cascade's atomicity - each repository call inside it would commit on its own instead
     * of all-or-nothing. Routing the call through a genuinely different bean goes through that
     * bean's own real proxy, so the DB step stays one real transaction, while the filesystem
     * step here - deliberately outside that transaction - only runs after it commits, and
     * never rolls the DB back if it fails.
     */
    public Codebase deleteRepository(Long repositoryId) {
        Codebase deleted = codebaseDeletionCascadeService.deleteRepositoryRecords(repositoryId);

        try {
            codebaseStorageService.deleteRepository(deleted.getName());
        } catch (RuntimeException e) {
            // The DB deletion already committed; an orphaned directory is recoverable
            // and must not fail the request or trigger a rollback.
            log.warn("Failed to delete repository directory for '{}'", deleted.getName(), e);
        }

        return deleted;
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
