package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.model.CodebaseRole;

public record UpdateMemberRoleRequest(CodebaseRole role) {
}