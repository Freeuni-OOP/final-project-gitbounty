package org.gitbounty.gitbountybackend.service.codebase;

import java.io.File;
import java.security.Principal;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.springframework.stereotype.Service;

@Service
public class GitRepositoryAccessService {

    private final CodebaseRepository codebaseRepository;

    public GitRepositoryAccessService(CodebaseRepository codebaseRepository) {
        this.codebaseRepository = codebaseRepository;
    }

    public void assertOwnerCanWrite(Repository repository, Principal principal)
        throws ServiceNotAuthorizedException {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ServiceNotAuthorizedException("Authentication is required to push");
        }

        Codebase codebase = codebaseRepository.findByName(resolveRepositoryName(repository))
            .orElseThrow(() -> new ServiceNotAuthorizedException("Repository is not registered in the database"));

        if (codebase.getOwner() == null || !principal.getName().equals(codebase.getOwner().getUsername())) {
            throw new ServiceNotAuthorizedException("Only the repository owner may push");
        }
    }

    private String resolveRepositoryName(Repository repository) throws ServiceNotAuthorizedException {
        File repositoryDirectory = repository.getDirectory();
        if (repositoryDirectory == null) {
            throw new ServiceNotAuthorizedException("Repository directory could not be resolved");
        }
        // strip away the .git suffix


        return repositoryDirectory.getName().replace(".git", "");
    }
}



