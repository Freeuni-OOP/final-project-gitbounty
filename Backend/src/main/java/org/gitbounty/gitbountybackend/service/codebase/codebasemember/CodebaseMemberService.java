package org.gitbounty.gitbountybackend.service.codebase.codebasemember;

import lombok.RequiredArgsConstructor;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodebaseMemberService {

    private final CodebaseMemberRepository memberRepository;

    /**
     * Safely adds a user to a codebase with a designated role.
     * Throws an exception if the user is already a collaborator.
     */
    @Transactional
    public CodebaseMember addMember(Codebase codebase, User user, CodebaseRole role) {
        // Enforce the unique constraint at the application level to avoid generic SQL errors
        if (memberRepository.existsByCodebaseIdAndUserId(codebase.getId(), user.getId())) {
            throw new IllegalStateException(
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
     * Updates an existing member's access permissions.
     */
    @Transactional
    public CodebaseMember updateMemberRole(Long codebaseId, Long userId, CodebaseRole newRole) {
        CodebaseMember member = memberRepository.findByCodebaseIdAndUserId(codebaseId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Membership association not found."));

        member.setRole(newRole);
        return memberRepository.save(member);
    }

    /**
     * Removes a user from a codebase's collaborator list.
     */
    @Transactional
    public void removeMember(Long codebaseId, Long userId) {
        CodebaseMember member = memberRepository.findByCodebaseIdAndUserId(codebaseId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Membership association not found."));

        memberRepository.delete(member);
    }

    /**
     * Read-only fetch for a codebase's internal roster.
     */
    @Transactional(readOnly = true)
    public List<CodebaseMember> getCodebaseRoster(Long codebaseId) {
        return memberRepository.findByCodebaseId(codebaseId);
    }
}