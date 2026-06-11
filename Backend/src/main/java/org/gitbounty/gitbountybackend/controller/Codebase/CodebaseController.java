package org.gitbounty.gitbountybackend.controller.Codebase;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/codebases")
public class CodebaseController {

    private final CodebaseService codebaseService;
    private final CodebaseMemberService memberService;

    public CodebaseController(
            CodebaseService codebaseService,
            CodebaseMemberService memberService
    ) {
        this.codebaseService = codebaseService;
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<CodebaseResponse> createCodebase(
            @RequestBody CreateCodebaseRequest request,
            Principal principal
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
                principal
        );

        return ResponseEntity.created(URI.create(codebase.getGitUrl()))
                .body(CodebaseResponse.from(codebase));
    }

    @GetMapping("/{repositoryName}")
    public ResponseEntity<CodebaseResponse> getCodebase(
            @PathVariable String repositoryName
    ) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        return ResponseEntity.ok(CodebaseResponse.from(codebase));
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
    @PreAuthorize("@codebasePermissions.isOwner(#repositoryName, authentication.name)")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable String repositoryName,
            @RequestBody AddMemberRequest request
    ) {
        CodebaseMember member = memberService.addMember(repositoryName, request.username(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @PutMapping("/{repositoryName}/members/{username}")
    @PreAuthorize("@codebasePermissions.isOwner(#repositoryName, authentication.name)")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable String repositoryName,
            @PathVariable String username,
            @RequestBody UpdateMemberRoleRequest request
    ) {
        CodebaseMember member = memberService.updateMemberRole(repositoryName, username, request.role());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @DeleteMapping("/{repositoryName}/members/{username}")
    @PreAuthorize("@codebasePermissions.isOwner(#repositoryName, authentication.name)")
    public ResponseEntity<Void> removeMember(
            @PathVariable String repositoryName,
            @PathVariable String username
    ) {
        memberService.removeMember(repositoryName, username);
        return ResponseEntity.noContent().build();
    }
}