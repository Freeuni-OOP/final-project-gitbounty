package org.gitbounty.gitbountybackend.service.codebase.storage;

import java.util.List;

public record DirectoryContents(String name, List<String> contents) implements PathContents {
}
