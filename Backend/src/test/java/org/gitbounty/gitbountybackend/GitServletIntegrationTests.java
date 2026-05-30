package org.gitbounty.gitbountybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.time.Instant;

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
import org.gitbounty.gitbountybackend.apis.KeycloakApi;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GitServletIntegrationTests {

    private static final String REPOSITORY_NAME = "demo";
    private static final String OWNER_USERNAME = "git-owner";
    private static final String OWNER_PASSWORD = "git-owner-password";
    private static final String INTRUDER_USERNAME = "git-intruder";
    private static final String INTRUDER_PASSWORD = "git-intruder-password";

    private static final String MOCK_OWNER_TOKEN = "mock-owner-token-string";
    private static final String MOCK_INTRUDER_TOKEN = "mock-intruder-token-string";

    @Autowired
    private Path resolveRepositoriesRoot;

    @LocalServerPort
    private int port;

    @Autowired
    private ServletRegistrationBean<GitServlet> gitServletRegistration;

    @Autowired
    private UserService userService;

    @Autowired
    private CodebaseService codebaseService;

    // 1. Mock the Keycloak API outward network delegation layer
    @MockitoBean
    private KeycloakApi keycloakApi;

    // 2. Mock the internal JWT token decoding engine
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void prepareBareRepository() throws Exception {
        Path serverRepository = resolveRepositoriesRoot.resolve(REPOSITORY_NAME + ".git");

        userService.findByUsername(OWNER_USERNAME)
            .orElseGet(() -> userService.save(new User(OWNER_USERNAME, OWNER_USERNAME + "@test.local", "id-owner")));

        userService.findByUsername(INTRUDER_USERNAME)
            .orElseGet(() -> userService.save(new User(INTRUDER_USERNAME, INTRUDER_USERNAME + "@test.local", "id-intruder")));
        // 3. Configure mock rules BEFORE service initialization executes
        mockKeycloakAuthenticationFlow();

        Principal ownerPrincipal = () -> OWNER_USERNAME;
        codebaseService.createCodebase(REPOSITORY_NAME, "Demo repository", repositoryHttpUrl(), ownerPrincipal);

        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try (var repository = builder.setGitDir(serverRepository.toFile()).build()) {
            assertThat(repository.isBare()).isTrue();
            StoredConfig config = repository.getConfig();
            config.setBoolean("http", null, "uploadpack", true);
            config.setBoolean("http", null, "receivepack", true);
            config.save();
        }
    }

    @AfterEach
    void cleanUp() {
        try {
            codebaseService.deleteCodebase(REPOSITORY_NAME);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userService.findByUsername(OWNER_USERNAME).ifPresent(userService::delete);
        userService.findByUsername(INTRUDER_USERNAME).ifPresent(userService::delete);
    }

    /**
     * Simulates your complete custom KeycloakAuthenticationProvider flow:
     * Basic Credentials -> KeycloakApi Token Request -> JwtDecoder Validation
     */
    private void mockKeycloakAuthenticationFlow() {
        // Step A: Map basic credentials entries to fake string tokens
        when(keycloakApi.requestAccessToken(OWNER_USERNAME, OWNER_PASSWORD)).thenReturn(MOCK_OWNER_TOKEN);
        when(keycloakApi.requestAccessToken(INTRUDER_USERNAME, INTRUDER_PASSWORD)).thenReturn(MOCK_INTRUDER_TOKEN);

        // Step B: Set up JWT mapping expectations when those fake string tokens are resolved
        Jwt ownerJwt = Jwt.withTokenValue(MOCK_OWNER_TOKEN)
            .header("alg", "none")
            .claim("preferred_username", OWNER_USERNAME)
            .claim("email", OWNER_USERNAME + "@test.local")
            .subject("id-owner")
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        Jwt intruderJwt = Jwt.withTokenValue(MOCK_INTRUDER_TOKEN)
            .header("alg", "none")
            .claim("preferred_username", INTRUDER_USERNAME)
            .claim("email", INTRUDER_USERNAME + "@test.local")
            .subject("id-intruder")
            .expiresAt(Instant.now().plusSeconds(3600))
            .build();

        when(jwtDecoder.decode(MOCK_OWNER_TOKEN)).thenReturn(ownerJwt);
        when(jwtDecoder.decode(MOCK_INTRUDER_TOKEN)).thenReturn(intruderJwt);
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

            // JGit natively passes username/password, triggering KeycloakAuthenticationProvider
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

            // Asserts that your provider decodes the token, identifies them as INTRUDER_USERNAME, and denies access
            assertThatThrownBy(() -> pushToOrigin(intruderRepo, intruderRepo.getRepository().getBranch(), credentialsProvider(INTRUDER_USERNAME, INTRUDER_PASSWORD)))
                .isInstanceOf(TransportException.class);
        }
    }

    private String repositoryHttpUrl() {
        return "http://localhost:" + port + "/git/" + REPOSITORY_NAME + ".git";
    }

    private static void commitFile(Git repo, Path repoDir, String content, String message) throws Exception {
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