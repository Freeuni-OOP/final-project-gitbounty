package org.gitbounty.gitbountybackend.controller.codebase;

import java.net.URI;
import java.util.List;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberService;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/codebases")
public class CodebaseController {

    private final CodebaseService codebaseService;
    private final CodebaseMemberService memberService;
    private final CodebasePermissions codebasePermissions;
    private final BranchService branchService;

    public CodebaseController(
            CodebaseService codebaseService,
            CodebaseMemberService memberService,
            CodebasePermissions codebasePermissions,
            BranchService branchService) {
        this.codebaseService = codebaseService;
        this.memberService = memberService;
        this.codebasePermissions = codebasePermissions;
        this.branchService = branchService;
    }

    @GetMapping
    public ResponseEntity<List<CodebaseResponse>> getAllCodebases() {
        List<CodebaseResponse> codebases = codebaseService.getAllCodebases()
                .stream()
                .map(CodebaseResponse::from)
                .toList();
        return ResponseEntity.ok(codebases);
    }

    @PostMapping("/create")
    public ResponseEntity<CodebaseResponse> createCodebase(
            @RequestBody CreateCodebaseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String repositoryName = request.name() == null ? "" : request.name().trim();
        String gitUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/git/")
                .path(repositoryName).path(".git")
                .toUriString();

        Codebase codebase = codebaseService.createCodebase(
                repositoryName,
                request.description(),
                gitUrl,
                jwt.getSubject()
        );

        return ResponseEntity.created(URI.create(codebase.getGitUrl()))
                .body(CodebaseResponse.from(codebase, getBranchResponses(codebase)));
    }


    @GetMapping("/{repositoryName}")
    public ResponseEntity<CodebaseResponse> getCodebase(
            @PathVariable String repositoryName
    ) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        return ResponseEntity.ok(CodebaseResponse.from(codebase, getBranchResponses(codebase)));
    }

    @PatchMapping("/{repositoryName}")
    public ResponseEntity<CodebaseResponse> updateCodebase(
        @PathVariable String repositoryName,
        @RequestBody UpdateCodebaseCommand command,
        @AuthenticationPrincipal Jwt jwt
    ) {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to update codebase");
        }
        Codebase updatedCodebase = codebaseService.updateCodebase(repositoryName, command);
        return ResponseEntity.ok(CodebaseResponse.from(updatedCodebase));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCodebase(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (!codebasePermissions.canDeleteRepository(id, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to delete this repository");
        }

        codebaseService.deleteRepository(id);

        return ResponseEntity.noContent().build();
    }

    // don't know how to test controllers
    @GetMapping("/{repositoryName}/contents/{*path}")
    public ResponseEntity<CodebaseContentsDTO> getContents(
        @PathVariable String repositoryName,
        @PathVariable String path,
        @RequestParam(defaultValue = "main") String branch
    ) {
        // Clean up the path if it's empty or null (e.g., when calling just /contents/)
        String cleanedPath = (path == null || path.isEmpty() || path.equals("/")) ? "/"
            : path.replaceAll("^/", ""); // removes leading slash if present

        CodebaseContentsDTO contents = codebaseService.listCodebaseContents(repositoryName, cleanedPath, branch);
        return ResponseEntity.ok(contents);
    }
    // --- Member endpoints ---

    @GetMapping("/{repositoryName}/members")
    public ResponseEntity<List<MemberResponse>> getMembers(
            @PathVariable String repositoryName
    ) {
        List<MemberResponse> members = memberService.getCodebaseRoster(repositoryName)
                .stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{repositoryName}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable String repositoryName,
            @RequestBody AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to add member");
        }
        CodebaseMember member = memberService.addMember(repositoryName, request.username(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @PutMapping("/{repositoryName}/members/{username}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable String repositoryName,
            @PathVariable String username,
            @RequestBody UpdateMemberRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to update member");
        }
        CodebaseMember member = memberService.updateMemberRole(repositoryName, username, request.role());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @DeleteMapping("/{repositoryName}/members/{username}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String repositoryName,
            @PathVariable String username,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if(!codebasePermissions.isOwnerBySubject(repositoryName, jwt.getSubject())) {
            throw new AccessDeniedException("Don't have permission to remove member");
        }
        memberService.removeMember(repositoryName, username);
        return ResponseEntity.noContent().build();
    }

    private List<BranchResponse> getBranchResponses(Codebase codebase) {
        return branchService.getAllBranchesForCodebase(codebase).stream().map(BranchResponse::from).toList();
    }
}