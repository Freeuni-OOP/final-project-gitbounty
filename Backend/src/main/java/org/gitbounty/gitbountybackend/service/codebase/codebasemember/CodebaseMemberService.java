package org.gitbounty.gitbountybackend.service.codebase.codebasemember;

import lombok.RequiredArgsConstructor;
import org.gitbounty.gitbountybackend.exception.CodebaseMemberNotFoundException;
import org.gitbounty.gitbountybackend.exception.DuplicateCodebaseMemberException;
import org.gitbounty.gitbountybackend.exception.UserNotFoundException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodebaseMemberService {

    private final CodebaseMemberRepository memberRepository;
    private final CodebaseService codebaseService;
    private final UserService userService;

    /**
     * Safely adds a user (by username) to a codebase (by name) with a designated role.
     * Resolves the codebase and user internally and throws if either is missing
     * or if the user is already a collaborator.
     */
    @Transactional
    public CodebaseMember addMember(String repositoryName, String username, CodebaseRole role) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = resolveUser(username);

        if (memberRepository.existsByCodebaseIdAndUserId(codebase.getId(), user.getId())) {
            throw new DuplicateCodebaseMemberException(
                    String.format("User '%s' is already a member of this codebase.", user.getUsername())
            );
        }

        CodebaseMember newMember = CodebaseMember.builder()
                .codebase(codebase)
                .user(user)
                .role(role)
                .build();

        return memberRepository.save(newMember);
    }

    /**
     * Updates an existing member's access permissions, resolving codebase and user by name.
     */
    @Transactional
    public CodebaseMember updateMemberRole(String repositoryName, String username, CodebaseRole newRole) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = resolveUser(username);

        CodebaseMember member = memberRepository.findByCodebaseIdAndUserId(codebase.getId(), user.getId())
                .orElseThrow(() -> new CodebaseMemberNotFoundException("Membership association not found."));

        member.setRole(newRole);
        return memberRepository.save(member);
    }

    /**
     * Removes a user from a codebase's collaborator list, resolving by name.
     */
    @Transactional
    public void removeMember(String repositoryName, String username) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = resolveUser(username);

        CodebaseMember member = memberRepository.findByCodebaseIdAndUserId(codebase.getId(), user.getId())
                .orElseThrow(() -> new CodebaseMemberNotFoundException("Membership association not found."));

        memberRepository.delete(member);
    }

    /**
     * Read-only fetch for a codebase's internal roster, resolving by name.
     */
    @Transactional(readOnly = true)
    public List<CodebaseMember> getCodebaseRoster(String repositoryName) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        return memberRepository.findByCodebaseId(codebase.getId());
    }

    @Transactional(readOnly = true)
    public User getMemberUser(String repositoryName, String username) {
        Codebase codebase = codebaseService.getCodebase(repositoryName);
        User user = resolveUser(username);

        return memberRepository.findByCodebaseIdAndUserId(codebase.getId(), user.getId())
                .orElseThrow(() -> new CodebaseMemberNotFoundException("Membership association not found."))
                .getUser();
    }

    private User resolveUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));
    }
}