package org.gitbounty.gitbountybackend.controller.codebase;

import org.gitbounty.gitbountybackend.config.TestSecurityConfig;
import org.gitbounty.gitbountybackend.model.Branch;
import org.gitbounty.gitbountybackend.model.Codebase;
import org.gitbounty.gitbountybackend.model.CodebaseMember;
import org.gitbounty.gitbountybackend.model.CodebaseRole;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.codebase.CodebaseService;
import org.gitbounty.gitbountybackend.service.codebase.branch.BranchService;
import org.gitbounty.gitbountybackend.service.codebase.codebasemember.CodebaseMemberService;
import org.gitbounty.gitbountybackend.service.codebase.dto.CodebaseContentsDTO;
import org.gitbounty.gitbountybackend.service.codebase.dto.FileType;
import org.gitbounty.gitbountybackend.service.codebase.dto.UpdateCodebaseCommand;
import org.gitbounty.gitbountybackend.service.codebase.storage.DirectoryContents;
import org.gitbounty.gitbountybackend.service.codebase.storage.PathContents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CodebaseController.class)
@Import(TestSecurityConfig.class)
class CodebaseControllerTest {

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

    private static User user(Long id, String username, String keycloakId) {
        User user = new User(username, username + "@test.com", keycloakId);
        user.setId(id);
        return user;
    }

    private static Codebase codebase(String name, User owner) {
        Codebase codebase = new Codebase();
        codebase.setId(1L);
        codebase.setName(name);
        codebase.setDescription("Test repo");
        codebase.setGitUrl("http://localhost/git/" + name + ".git");
        codebase.setOwner(owner);
        codebase.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return codebase;
    }

    private static CodebaseMember member(Long id, Codebase codebase, User user, CodebaseRole role) {
        CodebaseMember member = new CodebaseMember();
        member.setId(id);
        member.setCodebase(codebase);
        member.setUser(user);
        member.setRole(role);
        return member;
    }

    private static Branch branch(Long id, String name, Codebase codebase) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        branch.setCodebase(codebase);
        return branch;
    }

    @Test
    void createCodebase_ShouldReturnCreated() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase created = codebase("my-repo", owner);

        when(codebaseService.createCodebase(eq("my-repo"), eq("Test repo"), anyString(), eq("kc-owner")))
                .thenReturn(created);

        mockMvc.perform(post("/api/codebases/create")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": " my-repo ",
                                "description": "Test repo"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/git/my-repo.git"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("my-repo"))
                .andExpect(jsonPath("$.description").value("Test repo"))
                .andExpect(jsonPath("$.ownerUsername").value("owner"));

        verify(codebaseService).createCodebase(eq("my-repo"), eq("Test repo"), anyString(), eq("kc-owner"));
    }

    @Test
    void getCodebase_ShouldReturnOk() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase repo = codebase("my-repo", owner);
        Branch masterBranch = branch(1L, "refs/heads/master", repo);

        when(codebaseService.getCodebase("my-repo")).thenReturn(repo);
        when(branchService.getAllBranchesForCodebase(repo)).thenReturn(List.of(masterBranch));

        mockMvc.perform(get("/api/codebases/my-repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("my-repo"))
                .andExpect(jsonPath("$.ownerUsername").value("owner"))
                .andExpect(jsonPath("$.branches").isArray())
                .andExpect(jsonPath("$.branches[0].name").value("master"));

        verify(codebaseService).getCodebase("my-repo");
        verify(branchService).getAllBranchesForCodebase(repo);
    }

    @Test
    void updateCodebase_ShouldReturnOk_WhenOwner() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase updated = codebase("new-repo", owner);
        updated.setDescription("Updated repo");

        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(true);
        when(codebaseService.updateCodebase(eq("my-repo"), any(UpdateCodebaseCommand.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/codebases/my-repo")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "new-repo",
                                "description": "Updated repo",
                                "gitUrl": "http://localhost/git/new-repo.git"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new-repo"))
                .andExpect(jsonPath("$.description").value("Updated repo"));

        verify(codebasePermissions).isOwnerBySubject("my-repo", "kc-owner");
        verify(codebaseService).updateCodebase(eq("my-repo"), any(UpdateCodebaseCommand.class));
    }

    @Test
    void updateCodebase_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(false);

        mockMvc.perform(patch("/api/codebases/my-repo")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "description": "Updated repo"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(codebasePermissions).isOwnerBySubject("my-repo", "kc-owner");
        verifyNoInteractions(codebaseService);
    }

    @Test
    void getContents_ShouldReturnOk() throws Exception {
        CodebaseContentsDTO contents = new CodebaseContentsDTO(FileType.FILE, "hello world", null);

        when(codebaseService.listCodebaseContents("my-repo", "src/Main.java", "dev"))
                .thenReturn(contents);

        mockMvc.perform(get("/api/codebases/my-repo/contents/src/Main.java")
                        .param("branch", "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FILE"))
                .andExpect(jsonPath("$.content").value("hello world"));

        verify(codebaseService).listCodebaseContents("my-repo", "src/Main.java", "dev");
    }

    @Test
    void getMembers_ShouldReturnOk() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        User developer = user(2L, "developer", "kc-dev");
        Codebase repo = codebase("my-repo", owner);
        CodebaseMember repoMember = member(10L, repo, developer, CodebaseRole.DEVELOPER);

        when(memberService.getCodebaseRoster("my-repo")).thenReturn(List.of(repoMember));

        mockMvc.perform(get("/api/codebases/my-repo/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].username").value("developer"))
                .andExpect(jsonPath("$[0].email").value("developer@test.com"))
                .andExpect(jsonPath("$[0].role").value("DEVELOPER"));

        verify(memberService).getCodebaseRoster("my-repo");
    }

    @Test
    void addMember_ShouldReturnCreated_WhenOwner() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        User developer = user(2L, "developer", "kc-dev");
        Codebase repo = codebase("my-repo", owner);
        CodebaseMember repoMember = member(10L, repo, developer, CodebaseRole.DEVELOPER);

        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(true);
        when(memberService.addMember("my-repo", "developer", CodebaseRole.DEVELOPER)).thenReturn(repoMember);

        mockMvc.perform(post("/api/codebases/my-repo/members")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "developer",
                                "role": "DEVELOPER"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.username").value("developer"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"));

        verify(memberService).addMember("my-repo", "developer", CodebaseRole.DEVELOPER);
    }

    @Test
    void addMember_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(false);

        mockMvc.perform(post("/api/codebases/my-repo/members")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "developer",
                                "role": "DEVELOPER"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    void updateMemberRole_ShouldReturnOk_WhenOwner() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        User developer = user(2L, "developer", "kc-dev");
        Codebase repo = codebase("my-repo", owner);
        CodebaseMember repoMember = member(10L, repo, developer, CodebaseRole.MAINTAINER);

        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(true);
        when(memberService.updateMemberRole("my-repo", "developer", CodebaseRole.MAINTAINER))
                .thenReturn(repoMember);

        mockMvc.perform(put("/api/codebases/my-repo/members/developer")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "role": "MAINTAINER"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("developer"))
                .andExpect(jsonPath("$.role").value("MAINTAINER"));

        verify(memberService).updateMemberRole("my-repo", "developer", CodebaseRole.MAINTAINER);
    }

    @Test
    void updateMemberRole_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(false);

        mockMvc.perform(put("/api/codebases/my-repo/members/developer")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "role": "MAINTAINER"
                            }
                            """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    void removeMember_ShouldReturnNoContent_WhenOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(true);

        mockMvc.perform(delete("/api/codebases/my-repo/members/developer")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner"))))
                .andExpect(status().isNoContent());

        verify(memberService).removeMember("my-repo", "developer");
    }

    @Test
    void removeMember_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(codebasePermissions.isOwnerBySubject("my-repo", "kc-owner")).thenReturn(false);

        mockMvc.perform(delete("/api/codebases/my-repo/members/developer")
                        .with(jwt().jwt(builder -> builder.subject("kc-owner"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(memberService);
    }

    @Test
    void getAllCodebases_IsPublic_ShouldReturnOk() throws Exception {
        when(codebaseService.getAllCodebases())
                .thenReturn(List.of(codebase("my-repo", user(1L, "owner", "kc-owner"))));

        mockMvc.perform(get("/api/codebases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("my-repo"));

        verify(codebaseService).getAllCodebases();
    }

    @Test
    void getCodebase_IsPublic_ShouldReturnOk() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase repo = codebase("my-repo", owner);
        Branch masterBranch = branch(1L, "refs/heads/master", repo);

        when(codebaseService.getCodebase("my-repo")).thenReturn(repo);
        when(branchService.getAllBranchesForCodebase(repo)).thenReturn(List.of(masterBranch));

        mockMvc.perform(get("/api/codebases/my-repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("my-repo"))
                .andExpect(jsonPath("$.branches").isArray());

        verify(codebaseService, atLeastOnce()).getCodebase("my-repo");
        verify(branchService).getAllBranchesForCodebase(repo);
    }

    @Test
    void getCodebase_ShouldReturnWithMultipleBranches() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase repo = codebase("my-repo", owner);
        Branch masterBranch = branch(1L, "refs/heads/master", repo);
        Branch devBranch = branch(2L, "refs/heads/dev", repo);
        Branch featureBranch = branch(3L, "refs/heads/feature/new-feature", repo);

        when(codebaseService.getCodebase("my-repo")).thenReturn(repo);
        when(branchService.getAllBranchesForCodebase(repo)).thenReturn(List.of(masterBranch, devBranch, featureBranch));

        mockMvc.perform(get("/api/codebases/my-repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branches").isArray())
                .andExpect(jsonPath("$.branches.length()").value(3))
                .andExpect(jsonPath("$.branches[0].name").value("master"))
                .andExpect(jsonPath("$.branches[1].name").value("dev"))
                .andExpect(jsonPath("$.branches[2].name").value("feature/new-feature"));

        verify(codebaseService).getCodebase("my-repo");
        verify(branchService).getAllBranchesForCodebase(repo);
    }

    @Test
    void getCodebase_ShouldReturnWithNoBranches() throws Exception {
        User owner = user(1L, "owner", "kc-owner");
        Codebase repo = codebase("my-repo", owner);

        when(codebaseService.getCodebase("my-repo")).thenReturn(repo);
        when(branchService.getAllBranchesForCodebase(repo)).thenReturn(List.of());

        mockMvc.perform(get("/api/codebases/my-repo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("my-repo"))
                .andExpect(jsonPath("$.branches").isArray())
                .andExpect(jsonPath("$.branches.length()").value(0));

        verify(codebaseService).getCodebase("my-repo");
        verify(branchService).getAllBranchesForCodebase(repo);
    }

    @Test
    void getContents_IsPublic_ShouldReturnOk() throws Exception {
        when(codebaseService.listCodebaseContents("my-repo", "src/Main.java", "master"))
                .thenReturn(new CodebaseContentsDTO(FileType.FILE, "hello world", null));

        mockMvc.perform(get("/api/codebases/my-repo/contents/src/Main.java")
                        .param("branch", "master"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FILE"));

        verify(codebaseService).listCodebaseContents("my-repo", "src/Main.java", "master");
    }

    @Test
    void getAllCodebases_ShouldReturnList() throws Exception {
        User owner = user(1L, "owner", "kc-owner");

        when(codebaseService.getAllCodebases()).thenReturn(List.of(codebase("my-repo", owner), codebase("other-repo", owner)));

        mockMvc.perform(get("/api/codebases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("my-repo"))
                .andExpect(jsonPath("$[1].name").value("other-repo"));

        verify(codebaseService).getAllCodebases();
    }

    @Test
    void getAllCodebases_ShouldReturnEmptyList() throws Exception {
        when(codebaseService.getAllCodebases()).thenReturn(List.of());

        mockMvc.perform(get("/api/codebases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verify(codebaseService).getAllCodebases();
    }

    @Test
    void getContents_WithNestedPath_ShouldPassCleanPathToService() throws Exception {
        PathContents mockPathContents = new DirectoryContents("src/main/java", List.of());
        CodebaseContentsDTO mockDto = new CodebaseContentsDTO(mockPathContents);

        when(codebaseService.listCodebaseContents("my-repo", "src/main/java", "main"))
            .thenReturn(mockDto);

        // Act & Assert
        mockMvc.perform(get("/api/codebases/my-repo/contents/src/main/java")
                .param("branch", "main")
                .with(jwt()))
            .andExpect(status().isOk());

        verify(codebaseService).listCodebaseContents("my-repo", "src/main/java", "main");
    }

    @Test
    void getContents_WithRootPath_ShouldPassDefaultSlashToService() throws Exception {
        PathContents mockPathContents = new DirectoryContents("/", List.of());
        CodebaseContentsDTO mockDto = new CodebaseContentsDTO(mockPathContents);

        when(codebaseService.listCodebaseContents("my-repo", "/", "main"))
            .thenReturn(mockDto);

        mockMvc.perform(get("/api/codebases/my-repo/contents/")
                .with(jwt()))
            .andExpect(status().isOk());

        verify(codebaseService).listCodebaseContents("my-repo", "/", "main");
    }
    @Test
    void getContents_WithPathTraversalAttack_ShouldSanitizeOrRejectRequest() throws Exception {
        PathContents mockPathContents = new DirectoryContents("/", List.of());
        CodebaseContentsDTO mockDto = new CodebaseContentsDTO(mockPathContents);

        // Scenario A: Expecting the application to normalize paths and block execution
        // Or if you reject with a 400 Bad Request, change the `.andExpect(status().isOk())` to `.isBadRequest()`
        when(codebaseService.listCodebaseContents(eq("my-repo"), anyString(), eq("master")))
            .thenReturn(mockDto);

        mockMvc.perform(get("/api/codebases/my-repo/contents/../../etc/passwd")
                .with(jwt()))
            .andExpect(status().isBadRequest()); // Assumes your mitigation throws an exception or rejects it

        // Ensure service layer is never invoked with malicious root escapes
        verify(codebaseService, never()).listCodebaseContents("my-repo", "../../etc/passwd", "master");
    }
}
