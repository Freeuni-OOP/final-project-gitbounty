package org.gitbounty.gitbountybackend.controller.user;

import org.gitbounty.gitbountybackend.config.TestSecurityConfig;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserPermissions userPermissions;

    private static User testUser(Long id, String username, String email, String keycloakId) {
        User user = new User(username, email, keycloakId);
        user.setId(id);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return user;
    }

    @Test
    void getUserById_ShouldReturnOk_WhenUserExists() throws Exception {
        User user = testUser(1L, "demo", "demo@test.com", "kc-demo");

        when(userService.findById(1L)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@test.com"));

        verify(userService).findById(1L);
    }

    @Test
    void getUserById_ShouldReturnNotFound_WhenUserMissing() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound());

        verify(userService).findById(99L);
    }

    @Test
    void getCurrentUserProfile_ShouldReturnOk_WhenUserExists() throws Exception {
        User user = testUser(1L, "demo", "demo@test.com", "kc-demo");

        when(userService.findByKeycloakId("kc-demo")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/profile/me")
                        .with(jwt().jwt(builder -> builder.subject("kc-demo"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("demo"))
                .andExpect(jsonPath("$.email").value("demo@test.com"));

        verify(userService).findByKeycloakId("kc-demo");
    }

    @Test
    void getCurrentUserProfile_ShouldReturnNotFound_WhenUserMissing() throws Exception {
        when(userService.findByKeycloakId("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/profile/me")
                        .with(jwt().jwt(builder -> builder.subject("ghost"))))
                .andExpect(status().isNotFound());

        verify(userService).findByKeycloakId("ghost");
    }

    @Test
    void updateUserProfile_ShouldReturnOk_WhenOwner() throws Exception {
        User updated = testUser(1L, "new-user", "new@test.com", "kc-demo");

        when(userPermissions.isOwnerById(1L, "kc-demo")).thenReturn(true);
        when(userService.updateUserProfile(1L, "new-user", "new@test.com")).thenReturn(updated);

        mockMvc.perform(put("/api/users/1")
                        .with(jwt().jwt(builder -> builder.subject("kc-demo")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "new-user",
                                "email": "new@test.com"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(jsonPath("$.email").value("new@test.com"));

        verify(userPermissions).isOwnerById(1L, "kc-demo");
        verify(userService).updateUserProfile(1L, "new-user", "new@test.com");
    }

    @Test
    void updateUserProfile_ShouldReturnForbidden_WhenNotOwner() throws Exception {
        when(userPermissions.isOwnerById(1L, "kc-demo")).thenReturn(false);

        mockMvc.perform(put("/api/users/1")
                        .with(jwt().jwt(builder -> builder.subject("kc-demo")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "new-user",
                                "email": "new@test.com"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(userPermissions).isOwnerById(1L, "kc-demo");
        verifyNoInteractions(userService);
    }
}