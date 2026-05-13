package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.StoredConfig;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GitServletIntegrationTests {

    private static final String REPOSITORY_NAME = "demo.git";

    @Autowired
    private Path resolveRepositoriesRoot;

    private Path serverRepository;

    @LocalServerPort
    private int port;

    @Autowired
    private ServletRegistrationBean<GitServlet> gitServletRegistration;

    @BeforeAll
    void prepareBareRepository() throws Exception {
        serverRepository = resolveRepositoriesRoot.resolve(REPOSITORY_NAME);

        try (Git bareRepo = Git.init().setBare(true).setDirectory(serverRepository.toFile()).call()) {
            assertThat(bareRepo.getRepository().isBare()).isTrue();

            StoredConfig config = bareRepo.getRepository().getConfig();
            config.setBoolean("http", null, "uploadpack", true);
            config.setBoolean("http", null, "receivepack", true);
            config.save();
        }
    }

    @AfterAll
    void cleanUp() throws Exception {
        if (!Files.exists(serverRepository)) {
            return;
        }

        try (var paths = Files.walk(serverRepository)) {
            paths.sorted(java.util.Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }

    @Test
    void gitServletIsMappedToGitPathAndLoadsImmediately() {
        assertThat(gitServletRegistration.getUrlMappings()).contains("/git/*");
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
            pushToOrigin(sourceRepo, branch);

            try (Git cloneRepo = Git.cloneRepository()
                .setURI(serverUrl)
                .setDirectory(cloneDir.toFile())
                .setCredentialsProvider(credentialsProvider())
                .call()) {
                assertThat(Files.readString(cloneDir.resolve("README.md"), StandardCharsets.UTF_8))
                    .isEqualTo("first version\n");

                commitFile(sourceRepo, sourceDir, "second version\n", "second commit");
                pushToOrigin(sourceRepo, branch);

                assertThat(cloneRepo.fetch().setRemote("origin").setCredentialsProvider(credentialsProvider()).call())
                    .isNotNull();
                cloneRepo.pull().setRemote("origin").setCredentialsProvider(credentialsProvider()).call();
            }

            assertThat(Files.readString(cloneDir.resolve("README.md"), StandardCharsets.UTF_8))
                .isEqualTo("second version\n");
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

    private static void pushToOrigin(Git repo, String branch) throws Exception {
        Iterable<PushResult> pushResults = repo.push()
            .setRemote("origin")
            .setCredentialsProvider(credentialsProvider())
            .setRefSpecs(new RefSpec("HEAD:refs/heads/" + branch))
            .call();

        assertThat(pushResults).isNotEmpty();
    }

    private static CredentialsProvider credentialsProvider() {
        return new UsernamePasswordCredentialsProvider("git", "git");
    }
}





