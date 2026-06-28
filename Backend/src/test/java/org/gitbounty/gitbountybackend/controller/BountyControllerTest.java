package org.gitbounty.gitbountybackend.controller;

import org.gitbounty.gitbountybackend.config.TestSecurityConfig;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.service.BountyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BountyController.class)
@Import(TestSecurityConfig.class)
class BountyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BountyService bountyService;

    private static Bounty bounty(Long id, String title, Double amount, BountyStatus status) {
        Bounty bounty = new Bounty();
        bounty.setId(id);
        bounty.setTitle(title);
        bounty.setDescription("Test bounty description");
        bounty.setAmount(amount);
        bounty.setStatus(status);
        bounty.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));
        return bounty;
    }

    private static BountyDTO bountyDto(Long id, String title, Double amount, BountyStatus status, Long issueId) {
        BountyDTO dto = new BountyDTO();
        dto.setId(id);
        dto.setTitle(title);
        dto.setDescription("Test bounty description");
        dto.setAmount(amount);
        dto.setStatus(status);
        dto.setIssueId(issueId);
        return dto;
    }

    @Test
    void createBounty_ShouldReturnOk() throws Exception {
        Bounty created = bounty(1L, "Fix bug", 100.0, BountyStatus.OPEN);

        when(bountyService.createBounty(any(BountyDTO.class), eq("kc-demo"))).thenReturn(created);

        mockMvc.perform(post("/api/bounties")
                        .with(jwt().jwt(builder -> builder.subject("kc-demo")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Fix bug",
                                "description": "Test bounty description",
                                "amount": 100.0,
                                "issueId": 10
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Fix bug"))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(bountyService).createBounty(any(BountyDTO.class), eq("kc-demo"));
    }

    @Test
    void getAllBounties_ShouldReturnOk() throws Exception {
        BountyDTO first = bountyDto(1L, "First bounty", 100.0, BountyStatus.OPEN, 10L);
        BountyDTO second = bountyDto(2L, "Second bounty", 200.0, BountyStatus.COMPLETED, 20L);

        when(bountyService.getAllBounties()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/bounties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("First bounty"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].title").value("Second bounty"));

        verify(bountyService).getAllBounties();
    }

    @Test
    void getBountiesByStatus_ShouldReturnOk() throws Exception {
        BountyDTO dto = bountyDto(1L, "Open bounty", 100.0, BountyStatus.OPEN, 10L);

        when(bountyService.getBountiesByStatus(BountyStatus.OPEN)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/bounties/status/OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Open bounty"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(bountyService).getBountiesByStatus(BountyStatus.OPEN);
    }

    @Test
    void getBountyById_ShouldReturnOk() throws Exception {
        BountyDTO dto = bountyDto(1L, "Single bounty", 150.0, BountyStatus.OPEN, 10L);

        when(bountyService.getBountyById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/bounties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Single bounty"))
                .andExpect(jsonPath("$.amount").value(150.0))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.issueId").value(10));

        verify(bountyService).getBountyById(1L);
    }
}