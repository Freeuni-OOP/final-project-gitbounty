package org.gitbounty.gitbountybackend.controller.bounty;

import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BountyController.class)
public class BountyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BountyService bountyService;

    @Test
    void getAllBounties_ShouldReturn200_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/bounties"))
                .andExpect(status().isOk());
    }

    @Test
    void getBountiesByRepository_ShouldReturn200_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/bounties/repository/1"))
                .andExpect(status().isOk());
    }
}