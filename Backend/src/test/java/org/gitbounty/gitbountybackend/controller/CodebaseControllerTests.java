package org.gitbounty.gitbountybackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.security.Principal;
import java.time.Instant;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CodebaseControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodebaseService codebaseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private Jwt jwtFor(String token) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("test-user-id")
                .claim("preferred_username", "tester")
                .claim("email", "tester@test.local")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private User owner() {
        return new User("tester", "tester@test.local", "test-user-id");
    }

    @Test
    void createCodebaseShouldReturnCreatedCodebaseResponse() throws Exception {
        String token = "valid-token";

        Codebase codebase = new Codebase(
                "gitbounty-core",
                "Demo repository",
                "http://localhost/git/gitbounty-core.git",
                owner()
        );

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.createCodebase(
                eq("gitbounty-core"),
                eq("Demo repository"),
                eq("http://localhost/git/gitbounty-core.git"),
                any(Principal.class)
        )).thenReturn(codebase);

        mockMvc.perform(post("/api/codebases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "name": "gitbounty-core",
                      "description": "Demo repository"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/git/gitbounty-core.git"))
                .andExpect(jsonPath("$.name").value("gitbounty-core"))
                .andExpect(jsonPath("$.description").value("Demo repository"))
                .andExpect(jsonPath("$.gitUrl").value("http://localhost/git/gitbounty-core.git"))
                .andExpect(jsonPath("$.ownerUsername").value("tester"));
    }

    @Test
    void createCodebaseShouldTrimRepositoryName() throws Exception {
        String token = "valid-token";

        Codebase codebase = new Codebase(
                "gitbounty-core",
                "Demo repository",
                "http://localhost/git/gitbounty-core.git",
                owner()
        );

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.createCodebase(
                eq("gitbounty-core"),
                eq("Demo repository"),
                eq("http://localhost/git/gitbounty-core.git"),
                any(Principal.class)
        )).thenReturn(codebase);

        mockMvc.perform(post("/api/codebases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "name": "   gitbounty-core   ",
                      "description": "Demo repository"
                    }
                    """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("gitbounty-core"));
    }

    @Test
    void createCodebaseShouldReturnConflictWhenServiceThrowsConflict() throws Exception {
        String token = "valid-token";

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.createCodebase(
                eq("existing-repo"),
                eq("Existing repository"),
                eq("http://localhost/git/existing-repo.git"),
                any(Principal.class)
        )).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Repository already exists"));

        mockMvc.perform(post("/api/codebases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                      "name": "existing-repo",
                      "description": "Existing repository"
                    }
                    """))
                .andExpect(status().isConflict());
    }

    @Test
    void getCodebaseShouldReturnCodebaseResponse() throws Exception {
        String token = "valid-token";

        Codebase codebase = new Codebase(
                "gitbounty-core",
                "Demo repository",
                "http://localhost/git/gitbounty-core.git",
                owner()
        );

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.getCodebase("gitbounty-core")).thenReturn(codebase);

        mockMvc.perform(get("/api/codebases/gitbounty-core")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("gitbounty-core"))
                .andExpect(jsonPath("$.description").value("Demo repository"))
                .andExpect(jsonPath("$.gitUrl").value("http://localhost/git/gitbounty-core.git"))
                .andExpect(jsonPath("$.ownerUsername").value("tester"));
    }

    @Test
    void getCodebaseShouldReturn404WhenRepositoryDoesNotExist() throws Exception {
        String token = "valid-token";

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.getCodebase("missing-repo"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Repository not found: missing-repo"));

        mockMvc.perform(get("/api/codebases/missing-repo")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCodebaseShouldHandleNullRepositoryName() throws Exception {
        String token = "valid-token";

        Codebase codebase = new Codebase(
                "",
                "Demo repository",
                "http://localhost/git/.git",
                owner()
        );

        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token));
        when(codebaseService.createCodebase(
                eq(""),
                eq("Demo repository"),
                eq("http://localhost/git/.git"),
                any(Principal.class)
        )).thenReturn(codebase);

        mockMvc.perform(post("/api/codebases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                  "description": "Demo repository"
                }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(""))
                .andExpect(jsonPath("$.gitUrl").value("http://localhost/git/.git"));
    }
}