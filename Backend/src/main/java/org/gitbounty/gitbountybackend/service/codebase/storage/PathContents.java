package org.gitbounty.gitbountybackend.service.codebase.storage;

public sealed interface PathContents permits FileContents, DirectoryContents {
}
