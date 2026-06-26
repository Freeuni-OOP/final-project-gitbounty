package org.gitbounty.gitbountybackend.service.codebase.dto;

import org.gitbounty.gitbountybackend.model.Codebase;

public record UpdateCodebaseCommand(
    String name,

    String description,

    String gitUrl
) {
    public void applyTo(Codebase codebase) {
        if (name != null) codebase.setName(name);
        if (description != null) codebase.setDescription(description);
        if (gitUrl != null) codebase.setGitUrl(gitUrl);
    }
}
