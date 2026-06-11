package org.gitbounty.gitbountybackend.controller.Codebase;

import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;

public record MemberResponse(
        Long id,
        String username,
        String email,
        CodebaseRole role
) {
    public static MemberResponse from(CodebaseMember member) {
        return new MemberResponse(
                member.getId(),
                member.getUser().getUsername(),
                member.getUser().getEmail(),
                member.getRole()
        );
    }
}