package org.gitbounty.gitbountybackend.service.codebase.dto;

import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.FileContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;

import java.util.List;

public record CodebaseContentsDTO(
    String type,          // "FILE" or "DIRECTORY"
    String content,       // Null if directory
    List<String> items    // Null if file
) {
    public CodebaseContentsDTO(PathContents entry) {
        this(
            entry instanceof FileContents ? "FILE" : "DIRECTORY",
            entry instanceof FileContents f ? f.contents() : null,
            entry instanceof DirectoryContents d ? d.contents() : null
        );
    }
}
