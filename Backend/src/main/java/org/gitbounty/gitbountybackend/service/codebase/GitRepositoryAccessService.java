package org.gitbounty.gitbountybackend.service.codebase;

import java.io.File;
import java.security.Principal;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class GitRepositoryAccessService {

    private final CodebaseRepository codebaseRepository;
    private final CodebaseMemberRepository codebaseMemberRepository;

    public GitRepositoryAccessService(CodebaseRepository codebaseRepository,
                                      CodebaseMemberRepository codebaseMemberRepository) {
        this.codebaseRepository = codebaseRepository;
        this.codebaseMemberRepository = codebaseMemberRepository;
    }

    public void assertUserCanWrite(Repository repository, Principal principal)
        throws ServiceNotAuthorizedException {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ServiceNotAuthorizedException("Authentication is required to push");
        }

        Codebase codebase = codebaseRepository.findByName(resolveRepositoryName(repository))
            .orElseThrow(() -> new ServiceNotAuthorizedException("Repository is not registered in the database"));

        String username = principal.getName();
        
        // Allow push if user is the owner
        if (codebase.getOwner() != null && username.equals(codebase.getOwner().getUsername())) {
            return;
        }

        // Allow push if user is a codebase member
        boolean isMemberWithPushAccess = codebaseMemberRepository.findByCodebaseId(codebase.getId())
            .stream()
            .anyMatch(member -> member.getUser().getUsername().equals(username));
        
        if (isMemberWithPushAccess) {
            return;
        }

        throw new ServiceNotAuthorizedException("Only repository owners and members may push");
    }

    private String resolveRepositoryName(Repository repository) throws ServiceNotAuthorizedException {
        File repositoryDirectory = repository.getDirectory();
        if (repositoryDirectory == null) {
            throw new ServiceNotAuthorizedException("Repository directory could not be resolved");
        }
        // strip away the .git suffix

        String name = repositoryDirectory.getName();
        return name.endsWith(".git") ? name.substring(0, name.length() - 4) : name;
    }
}



