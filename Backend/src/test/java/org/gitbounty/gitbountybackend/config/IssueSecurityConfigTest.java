package org.gitbounty.gitbountybackend.config;

import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.controller.issue.IssueController;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IssueController.class)
@Import(SecurityConfig.class)
class IssueSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IssueService issueService;

    @MockitoBean
    private CodebasePermissions codebasePermissions;

    @MockitoBean
    private KeycloakAuthenticationProvider keycloakAuthenticationProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listIssues_WithoutAuthentication_RemainsPublic()
            throws Exception {
        when(issueService.listIssues("my-repo"))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/codebases/my-repo/issues")
                )
                .andExpect(status().isOk());
    }

    @Test
    void createIssue_WithoutAuthentication_ReturnsUnauthorized()
            throws Exception {
        mockMvc.perform(
                        post("/api/codebases/my-repo/issues")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "title": "Unauthenticated issue",
                                            "description": "Must be rejected"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(issueService);
    }
}