package org.gitbounty.gitbountybackend.controller;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.service.BountyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bounties")
public class BountyController {

    private final BountyService bountyService;

    @Autowired
    public BountyController(BountyService bountyService) {
        this.bountyService = bountyService;
    }

    @PostMapping
    @PreAuthorize("@bountyPermissions.isIssueRepositoryOwner(#bountyDto.issueId, authentication.name)")
    public ResponseEntity<Bounty> createBounty(@RequestBody BountyDTO bountyDto, @AuthenticationPrincipal Jwt jwt) {
        Bounty created = bountyService.createBounty(bountyDto, jwt.getSubject());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    public List<BountyDTO> getAllBounties() {
        return bountyService.getAllBounties();
    }

    @GetMapping("/status/{status}")
    public List<BountyDTO> getBountiesByStatus(@PathVariable BountyStatus status) {
        return bountyService.getBountiesByStatus(status);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BountyDTO> getBountyById(@PathVariable Long id) {
        return ResponseEntity.ok(bountyService.getBountyById(id));
    }
}