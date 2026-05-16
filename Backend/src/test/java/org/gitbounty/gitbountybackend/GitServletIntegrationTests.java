package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import java.security.Principal;

import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitServletIntegrationTests {

    private static final String REPOSITORY_NAME = "demo.git";
    private static final String OWNER_USERNAME = "git-owner";
    private static final String OWNER_PASSWORD = "git-owner-password";
    private static final String INTRUDER_USERNAME = "git-intruder";
    private static final String INTRUDER_PASSWORD = "git-intruder-password";

    @Autowired
    private Path resolveRepositoriesRoot;

    private Path serverRepository;

    @LocalServerPort
    private int port;

    @Autowired
    private ServletRegistrationBean<GitServlet> gitServletRegistration;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodebaseService codebaseService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    void prepareBareRepository() throws Exception {
        serverRepository = resolveRepositoriesRoot.resolve(REPOSITORY_NAME);
        // Ensure users exist before creating the codebase
        userRepository.findByUsername(OWNER_USERNAME)
            .orElseGet(() -> userRepository.save(
                new User(OWNER_USERNAME, OWNER_USERNAME + "@test.local", passwordEncoder.encode(OWNER_PASSWORD))
            ));

        userRepository.findByUsername(INTRUDER_USERNAME)
            .orElseGet(() -> userRepository.save(
                new User(INTRUDER_USERNAME, INTRUDER_USERNAME + "@test.local", passwordEncoder.encode(INTRUDER_PASSWORD))
            ));

        // Create codebase via the service so tests exercise the same codepath as application
        Principal ownerPrincipal = () -> OWNER_USERNAME;
        codebaseService.createCodebase(REPOSITORY_NAME, "Demo repository", repositoryHttpUrl(), ownerPrincipal);

        // Open the created bare repository and ensure HTTP upload/receive are enabled
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (var repository = builder.setGitDir(serverRepository.toFile()).build()) {
            assertThat(repository.isBare()).isTrue();
            StoredConfig config = repository.getConfig();
            config.setBoolean("http", null, "uploadpack", true);
            config.setBoolean("http", null, "receivepack", true);
            config.save();
        }
    }

    @AfterAll
    void cleanUp() {
        // Use the service deletion method so cleanup goes through the same code paths
        try {
            codebaseService.deleteCodebase(REPOSITORY_NAME);
        } catch (Exception e) {
            // ignore - best effort cleanup
        }

        userRepository.findByUsername(OWNER_USERNAME).ifPresent(userRepository::delete);
        userRepository.findByUsername(INTRUDER_USERNAME).ifPresent(userRepository::delete);
    }

    @Test
    void gitServletIsMappedToGitPathAndLoadsImmediately() {
        assertThat(gitServletRegistration.getUrlMappings()).contains("/git/*");
    }

    @Test
    void anonymousRequestsToGitServletAreRejected() throws Exception {
        URL url = URI.create("http://localhost:" + port + "/git/" + REPOSITORY_NAME + "/info/refs?service=git-upload-pack").toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertThat(connection.getResponseCode()).isEqualTo(HttpURLConnection.HTTP_UNAUTHORIZED);
    }

    @Test
    void gitServletSupportsCloneFetchPushAndPullOverHttp() throws Exception {
        String serverUrl = repositoryHttpUrl();
        Path sourceDir = Files.createTempDirectory("git-source-");
        Path cloneDir = Files.createTempDirectory("git-clone-");

        try (Git sourceRepo = Git.init().setDirectory(sourceDir.toFile()).call()) {
            String branch = sourceRepo.getRepository().getBranch();

            commitFile(sourceRepo, sourceDir, "first version\n", "initial commit");
            configureOrigin(sourceRepo, serverUrl);
            pushToOrigin(sourceRepo, branch, credentialsProvider(OWNER_USERNAME, OWNER_PASSWORD));

            try (Git cloneRepo = Git.cloneRepository()
                .setURI(serverUrl)
                .setDirectory(cloneDir.toFile())
                .setCredentialsProvider(credentialsProvider(OWNER_USERNAME, OWNER_PASSWORD))
                .call()) {
                assertThat(Files.readString(cloneDir.resolve("README.md"), StandardCharsets.UTF_8))
                    .isEqualTo("first version\n");

                commitFile(sourceRepo, sourceDir, "second version\n", "second commit");
                pushToOrigin(sourceRepo, branch, credentialsProvider(OWNER_USERNAME, OWNER_PASSWORD));

                assertThat(cloneRepo.fetch().setRemote("origin").setCredentialsProvider(credentialsProvider(OWNER_USERNAME, OWNER_PASSWORD)).call())
                    .isNotNull();
                cloneRepo.pull().setRemote("origin").setCredentialsProvider(credentialsProvider(OWNER_USERNAME, OWNER_PASSWORD)).call();
            }

            assertThat(Files.readString(cloneDir.resolve("README.md"), StandardCharsets.UTF_8))
                .isEqualTo("second version\n");
        }
    }

    @Test
    void nonOwnerCannotPushToGitServlet() throws Exception {
        String serverUrl = repositoryHttpUrl();
        Path intruderDir = Files.createTempDirectory("git-intruder-");

        try (Git intruderRepo = Git.cloneRepository()
            .setURI(serverUrl)
            .setDirectory(intruderDir.toFile())
            .setCredentialsProvider(credentialsProvider(INTRUDER_USERNAME, INTRUDER_PASSWORD))
            .call()) {
            commitFile(intruderRepo, intruderDir, "intruder version\n", "intruder commit");

            assertThatThrownBy(() -> pushToOrigin(intruderRepo, intruderRepo.getRepository().getBranch(), credentialsProvider(INTRUDER_USERNAME, INTRUDER_PASSWORD)))
                .isInstanceOf(TransportException.class);
        }
    }

    private String repositoryHttpUrl() {
        return "http://localhost:" + port + "/git/" + REPOSITORY_NAME;
    }

    private static void commitFile(Git repo, Path repoDir, String content, String message)
        throws Exception {
        String fileName = "README.md";
        Files.writeString(repoDir.resolve(fileName), content, StandardCharsets.UTF_8);
        repo.add().addFilepattern(fileName).call();
        repo.commit()
            .setMessage(message)
            .setAuthor("Test User", "test@example.com")
            .setCommitter("Test User", "test@example.com")
            .call();
    }

    private static void configureOrigin(Git repo, String serverUrl) throws Exception {
        repo.remoteAdd()
            .setName("origin")
            .setUri(new URIish(serverUrl))
            .call();
    }

    private static void pushToOrigin(Git repo, String branch, CredentialsProvider credentialsProvider) throws Exception {
        Iterable<PushResult> pushResults = repo.push()
            .setRemote("origin")
            .setCredentialsProvider(credentialsProvider)
            .setRefSpecs(new RefSpec("HEAD:refs/heads/" + branch))
            .call();

        assertThat(pushResults).isNotEmpty();
    }

    private static CredentialsProvider credentialsProvider(String username, String password) {
        return new UsernamePasswordCredentialsProvider(username, password);
    }
}





