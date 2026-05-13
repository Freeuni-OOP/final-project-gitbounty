package org.gitbounty.gitbountybackend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gitbounty.gitbountybackend.controller.UserController;
import org.gitbounty.gitbountybackend.dto.CreateUserRequest;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureJsonTesters
class UserControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.objectMapper = new ObjectMapper();
    }

    @Test
    void registerShouldBePublic() throws Exception {
        CreateUserRequest request = new CreateUserRequest("hunter", "h@test.com", "pass");
        User mockUser = new User("hunter", "h@test.com", "hashed_pass");
        mockUser.setId(1L);

        when(userService.createUser(anyString(), anyString(), anyString())).thenReturn(mockUser);

        mockMvc.perform(post("/api/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("hunter"));
    }

    @Test
    @WithMockUser(username = "hunter")
    void getProfileShouldSucceedWhenAuthenticated() throws Exception {
        User mockUser = new User("hunter", "h@test.com", "hashed_pass");
        mockUser.setId(1L);

        when(userService.findById(1L)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("hunter"));
    }
}