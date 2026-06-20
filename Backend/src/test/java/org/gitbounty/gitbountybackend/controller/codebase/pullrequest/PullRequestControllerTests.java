package org.gitbounty.gitbountybackend.controller.codebase.pullrequest;

import org.gitbounty.gitbountybackend.config.KeycloakAuthenticationProvider;
import org.gitbounty.gitbountybackend.config.SecurityConfig;
import org.gitbounty.gitbountybackend.controller.codebase.CodebasePermissions;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.model.PullRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.issue.pullrequest.PullRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PullRequestController.class)
@Import(SecurityConfig.class)
class PullRequestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PullRequestService pullRequestService;

    @MockitoBean
    private CodebasePermissions codebasePermissions;

    @MockitoBean
    private KeycloakAuthenticationProvider keycloakAuthenticationProvider;


    @Test
    void createPullRequest_ShouldReturnCreated() throws Exception {
        String json = """
            {
                "sourceBranch": "feature",
                "targetBranch": "master",
                "title": "New feature",
                "description": "Amazing PR"
            }
            """;
        Branch source = new Branch();
        source.setName("feature");

        Branch target = new Branch();
        target.setName("master");

        User author = new User();
        author.setUsername("user-123");
        // Populate the PullRequest
        PullRequest pr = new PullRequest();
        pr.setSourceBranch(source);
        pr.setTargetBranch(target);
        pr.setTitle("New feature");
        pr.setDescription("Amazing PR");
        pr.setAuthor(author);

        when(pullRequestService.createPullRequest(any())).thenReturn(pr);

        mockMvc.perform(post("/api/codebases/my-repo/pull-requests")
                .with(jwt().jwt(builder -> builder.subject("user-123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated());
    }

    @Test
    void mergePullRequest_ShouldBeForbidden_WhenNotOwner() throws Exception {
        // Mock permission check to return false
        when(codebasePermissions.isOwnerBySubject("my-repo", "user-123")).thenReturn(false);

        mockMvc.perform(post("/api/codebases/my-repo/pull-requests/1/merge")
                .with(jwt().jwt(builder -> builder.subject("user-123"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void mergePullRequest_ShouldSucceed_WhenOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "user-123")).thenReturn(true);

        mockMvc.perform(post("/api/codebases/my-repo/pull-requests/1/merge")
                .with(jwt().jwt(builder -> builder.subject("user-123"))))
            .andExpect(status().isOk());

        verify(pullRequestService).mergePullRequestForCodebase("my-repo", 1);
    }

    @Test
    void closePullRequest_ShouldSucceed_WhenOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "user-123")).thenReturn(true);

        mockMvc.perform(patch("/api/codebases/my-repo/pull-requests/1")
                .with(jwt().jwt(builder -> builder.subject("user-123"))))
            .andExpect(status().isNoContent());

        verify(pullRequestService).updatePRStatus("my-repo", 1, IssueStatus.CLOSED);
    }
}