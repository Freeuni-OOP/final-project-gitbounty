package org.gitbounty.gitbountybackend.codebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.gitbounty.gitbountybackend.dto.CodebaseResponse;
import org.gitbounty.gitbountybackend.dto.CreateCodebaseRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.CodebaseRepository;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class CodebaseControllerIntegrationTests {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodebaseRepository codebaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Path resolveRepositoriesRoot;

    private String createdRepositoryName;
    private String createdOwnerUsername;

    @AfterEach
    void cleanUp() throws Exception {
        if (createdRepositoryName != null) {
            codebaseRepository.findByName(createdRepositoryName).ifPresent(codebaseRepository::delete);

            Path repositoryPath = resolveRepositoriesRoot.resolve(createdRepositoryName);
            if (Files.exists(repositoryPath)) {
                try (var paths = Files.walk(repositoryPath)) {
                    paths.sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                }
            }
        }

        if (createdOwnerUsername != null) {
            userRepository.findByUsername(createdOwnerUsername).ifPresent(userRepository::delete);
        }
    }

    @Test
    void authenticatedUserCanCreateRepository() {
        String username = "owner_" + UUID.randomUUID().toString().substring(0, 8);
        String password = "password-" + UUID.randomUUID().toString().substring(0, 8);
        String repositoryName = "repo_" + UUID.randomUUID().toString().substring(0, 8) + ".git";

        createdOwnerUsername = username;
        createdRepositoryName = repositoryName;

        userRepository.save(
            new User(username, username + "@test.local", passwordEncoder.encode(password))
        );

        ResponseEntity<CodebaseResponse> response = restTemplate.exchange(
            URI.create("http://localhost:" + port + "/api/codebases"),
            HttpMethod.POST,
            authenticatedEntity(username, password, new CreateCodebaseRequest(repositoryName, "Demo repository")),
            CodebaseResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo(repositoryName);
        assertThat(response.getBody().ownerUsername()).isEqualTo(username);
        assertThat(response.getBody().gitUrl()).isEqualTo("http://localhost:" + port + "/git/" + repositoryName);
        assertThat(codebaseRepository.findByName(repositoryName)).isPresent();
        assertThat(Files.exists(resolveRepositoriesRoot.resolve(repositoryName))).isTrue();
    }

    @Test
    void anonymousUserCannotCreateRepository() {
        assertThatThrownBy(() -> restTemplate.exchange(
            URI.create("http://localhost:" + port + "/api/codebases"),
            HttpMethod.POST,
            new HttpEntity<>(new CreateCodebaseRequest("anon-repo.git", "Anonymous repo"), new HttpHeaders()),
            String.class
        ))
            .isInstanceOf(HttpClientErrorException.Unauthorized.class)
            .satisfies(error -> {
                HttpClientErrorException.Unauthorized unauthorized = (HttpClientErrorException.Unauthorized) error;
                assertThat(unauthorized.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
    }

    private HttpEntity<CreateCodebaseRequest> authenticatedEntity(String username, String password, CreateCodebaseRequest body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(username, password);
        return new HttpEntity<>(body, headers);
    }
}




