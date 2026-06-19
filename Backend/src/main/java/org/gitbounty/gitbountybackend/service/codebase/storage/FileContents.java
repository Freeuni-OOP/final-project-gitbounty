package org.gitbounty.gitbountybackend.service.codebase.storage;

public record FileContents(String name, String contents) implements PathContents {
}
