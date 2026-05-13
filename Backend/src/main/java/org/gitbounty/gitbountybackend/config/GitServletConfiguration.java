package org.gitbounty.gitbountybackend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.http.HttpServletRequest;

import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.UploadPackFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.gitbounty.gitbountybackend.service.GitRepositoryAccessService;

/**
 * Configuration class for registering JGit HTTP Server.
 * This configuration enables the platform to act as a Git server,
 * handling standard Git commands (clone, push, pull) over HTTP.
 */
@Configuration
public class GitServletConfiguration {

    @Bean
    public Path resolveRepositoriesRoot(@Value("${git.repositories-root:repositories}") String repositoriesRoot) {
        Path rootPath = Paths.get(repositoriesRoot).toAbsolutePath().normalize();

        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create Git repositories root at " + rootPath, e);
        }

        return rootPath;
    }

    @Bean
    public RepositoryResolver<HttpServletRequest> repositoryResolver(Path resolveRepositoriesRoot) {
        return (HttpServletRequest request, String repositoryName) -> {
            Path repositoryPath = resolveRepositoriesRoot.resolve(repositoryName).normalize();
            if (!repositoryPath.startsWith(resolveRepositoriesRoot) || !Files.isDirectory(repositoryPath)) {
                throw new RepositoryNotFoundException(repositoryName);
            }

            try {
                return new FileRepositoryBuilder()
                    .setGitDir(repositoryPath.toFile())
                    .setMustExist(true)
                    .build();
            } catch (IOException e) {
                throw new RepositoryNotFoundException(repositoryName);
            }
        };
    }

    /**
     * Register and configure the GitServlet for handling Git protocol requests.
     * The servlet is mapped to /git/* URL pattern and bypasses the standard
     * Spring DispatcherServlet for Git-specific traffic, allowing JGit to manage
     * the Smart HTTP protocol negotiation.
     *
     * @return ServletRegistrationBean configured with GitServlet
     */
    @Bean
    public ServletRegistrationBean<GitServlet> gitServletRegistration(
        RepositoryResolver<HttpServletRequest> repositoryResolver,
        ReceivePackFactory<HttpServletRequest> receivePackFactory
    ) {
        GitServlet gitServlet = new GitServlet();
        gitServlet.setRepositoryResolver(repositoryResolver);
        gitServlet.setUploadPackFactory(uploadPackFactory());
        gitServlet.setReceivePackFactory(receivePackFactory);

        ServletRegistrationBean<GitServlet> registration = new ServletRegistrationBean<>(gitServlet, "/git/*");
        registration.setLoadOnStartup(1);
        registration.setName("GitServlet");

        return registration;
    }

    private UploadPackFactory<HttpServletRequest> uploadPackFactory() {
        return (HttpServletRequest request, org.eclipse.jgit.lib.Repository repository) -> new UploadPack(repository);
    }

    @Bean
    public ReceivePackFactory<HttpServletRequest> receivePackFactory(
        GitRepositoryAccessService gitRepositoryAccessService
    ) {
        return (HttpServletRequest request, org.eclipse.jgit.lib.Repository repository) -> {
            gitRepositoryAccessService.assertOwnerCanWrite(repository, request.getUserPrincipal());
            return new ReceivePack(repository);
        };
    }
}




