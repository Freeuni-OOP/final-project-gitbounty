package org.gitbounty.gitbountybackend.controller.Codebase;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/codebases")
public class CodebaseController {

    private final CodebaseService codebaseService;
    private final CodebaseMemberService memberService;
    private final UserService userService;

    public CodebaseController(
            CodebaseService codebaseService,
            CodebaseMemberService memberService,
            UserService userService
    ) {
        this.codebaseService = codebaseService;
        this.memberService = memberService;
        this.userService = userService;
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
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        List<MemberResponse> members = memberService.getCodebaseRoster(codebase.getId())
                .stream()
                .map(MemberResponse::from)
                .toList();
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{repositoryName}/members")
    public ResponseEntity<MemberResponse> addMember(
            @PathVariable String repositoryName,
            @RequestBody AddMemberRequest request
    ) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = userService.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + request.username()));

        CodebaseMember member = memberService.addMember(codebase, user, request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @PutMapping("/{repositoryName}/members/{username}")
    public ResponseEntity<MemberResponse> updateMemberRole(
            @PathVariable String repositoryName,
            @PathVariable String username,
            @RequestBody UpdateMemberRoleRequest request
    ) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + username));

        CodebaseMember member = memberService.updateMemberRole(codebase.getId(), user.getId(), request.role());
        return ResponseEntity.ok(MemberResponse.from(member));
    }

    @DeleteMapping("/{repositoryName}/members/{username}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String repositoryName,
            @PathVariable String username
    ) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found: " + username));

        memberService.removeMember(codebase.getId(), user.getId());
        return ResponseEntity.noContent().build();
    }
}