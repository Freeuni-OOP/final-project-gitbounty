package org.gitbounty.gitbountybackend.controller.Codebase;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.User.UserService;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class CodebaseMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CodebaseService codebaseService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private String token;
    private Codebase codebase;

    private String random(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8);
    }

    private Jwt jwtFor(String token, String keycloakId, String username, String email) {
        return Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject(keycloakId)
                .claim("preferred_username", username)
                .claim("email", email)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @BeforeEach
    void setUp() throws Exception {
        token = random("token_");
        String keycloakId = random("kc_");
        String username = random("owner_");
        String email = random("mail_") + "@test.local";

        userService.save(new User(username, email, keycloakId));
        when(jwtDecoder.decode(token)).thenReturn(jwtFor(token, keycloakId, username, email));

        String repoName = random("repo_");
        mockMvc.perform(post("/api/codebases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + repoName + "\",\"description\":\"test repo\"}"))
                .andExpect(status().isCreated());

        codebase = codebaseService.getCodebase(repoName);
    }

    @Test
    void addMember_returnsCreated() throws Exception {
        String memberUsername = random("member_");
        userService.save(new User(memberUsername, random("mail_") + "@test.local", random("kc_")));

        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + memberUsername + "\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(memberUsername))
                .andExpect(jsonPath("$.role").value("DEVELOPER"));
    }

    @Test
    void addMember_returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost_user\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMembers_returnsList() throws Exception {
        String memberUsername = random("member_");
        userService.save(new User(memberUsername, random("mail_") + "@test.local", random("kc_")));

        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + memberUsername + "\",\"role\":\"REPORTER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value(memberUsername));
    }

    @Test
    void removeMember_returnsNoContent() throws Exception {
        String memberUsername = random("member_");
        userService.save(new User(memberUsername, random("mail_") + "@test.local", random("kc_")));

        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + memberUsername + "\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/codebases/" + codebase.getName() + "/members/" + memberUsername)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateMemberRole_returnsUpdatedRole() throws Exception {
        String memberUsername = random("member_");
        userService.save(new User(memberUsername, random("mail_") + "@test.local", random("kc_")));

        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + memberUsername + "\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/codebases/" + codebase.getName() + "/members/" + memberUsername)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MAINTAINER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MAINTAINER"));
    }
    @Test
    void addMember_returns403_whenNotOwner() throws Exception {
        // A different authenticated user who is NOT the codebase owner
        String otherToken = random("token_");
        String otherKc = random("kc_");
        String otherUsername = random("intruder_");
        String otherEmail = random("mail_") + "@test.local";
        userService.save(new User(otherUsername, otherEmail, otherKc));
        when(jwtDecoder.decode(otherToken))
                .thenReturn(jwtFor(otherToken, otherKc, otherUsername, otherEmail));

        // A real target user to attempt to add
        String memberUsername = random("member_");
        userService.save(new User(memberUsername, random("mail_") + "@test.local", random("kc_")));

        mockMvc.perform(post("/api/codebases/" + codebase.getName() + "/members")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + memberUsername + "\",\"role\":\"DEVELOPER\"}"))
                .andExpect(status().isForbidden());
    }
}