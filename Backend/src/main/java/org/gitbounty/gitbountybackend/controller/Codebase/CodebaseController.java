package org.gitbounty.gitbountybackend.controller.Codebase;

import java.net.URI;
import java.security.Principal;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/codebases")
public class CodebaseController {

    private final CodebaseService codebaseService;

    public CodebaseController(CodebaseService codebaseService) {
        this.codebaseService = codebaseService;
    }

    @PostMapping
    public ResponseEntity<CodebaseResponse> createCodebase(
        @RequestBody CreateCodebaseRequest request,
        Principal principal
    ) {
        String repositoryName = request.name() == null ? "" : request.name().trim();
        String gitUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/git/")
            .path(repositoryName)
            .path(".git")
            .toUriString();

        Codebase codebase = codebaseService.createCodebase(
            repositoryName,
            request.description(),
            gitUrl,
            principal
        );

        return ResponseEntity.created(URI.create(codebase.getGitUrl()))
            .body(CodebaseResponse.from(codebase));
    }
}

