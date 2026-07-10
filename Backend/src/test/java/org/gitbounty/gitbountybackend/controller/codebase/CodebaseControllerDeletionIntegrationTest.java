package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.config.KeycloakAuthenticationProvider;
import org.gitbounty.gitbountybackend.config.SecurityConfig;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises DELETE /api/codebases/{id} against the real security filter chain
 * (SecurityConfig), rather than a permit-all test config, to confirm unauthenticated
 * and unauthorized requests are actually rejected end-to-end.
 */
@WebMvcTest(CodebaseController.class)
@Import(SecurityConfig.class)
class CodebaseControllerDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CodebaseService codebaseService;

    @MockitoBean
    private CodebaseMemberService memberService;

    @MockitoBean
    private CodebasePermissions codebasePermissions;

    @MockitoBean
    private BranchService branchService;

    @MockitoBean
    private KeycloakAuthenticationProvider keycloakAuthenticationProvider;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static Codebase codebase(String name, String ownerKeycloakId) {
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        owner.setKeycloakId(ownerKeycloakId);

        Codebase codebase = new Codebase();
        codebase.setId(1L);
        codebase.setName(name);
        codebase.setDescription("desc");
        codebase.setGitUrl("http://localhost/git/" + name + ".git");
        codebase.setOwner(owner);
        codebase.setCreatedAt(LocalDateTime.now());
        return codebase;
    }

    @Test
    void deleteCodebase_ShouldReturnNoContent_WhenOwner() throws Exception {
        Codebase repo = codebase("my-repo", "kc-owner");

        when(codebasePermissions.canDeleteRepository(1L, "kc-owner")).thenReturn(true);
        when(codebaseService.deleteRepository(1L)).thenReturn(repo);

        mockMvc.perform(delete("/api/codebases/1")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner"))))
                .andExpect(status().isNoContent());

        verify(codebaseService).deleteRepository(1L);
    }

    @Test
    void deleteCodebase_ShouldReturnForbidden_WhenAuthenticatedButNotAuthorized() throws Exception {
        when(codebasePermissions.canDeleteRepository(1L, "kc-reporter")).thenReturn(false);

        mockMvc.perform(delete("/api/codebases/1")
                        .with(jwt().jwt(builder -> builder.subject("kc-reporter"))))
                .andExpect(status().isForbidden());

        verify(codebaseService, never()).deleteRepository(any());
    }

    @Test
    void deleteCodebase_ShouldReturnUnauthorized_WhenNoCredentialsProvided() throws Exception {
        mockMvc.perform(delete("/api/codebases/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(codebaseService, codebasePermissions);
    }
}
