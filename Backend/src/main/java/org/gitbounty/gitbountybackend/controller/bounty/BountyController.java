package org.gitbounty.gitbountybackend.controller.bounty;

import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.service.bounty.BountyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

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
    public ResponseEntity<BountyDTO> createBounty(@RequestBody BountyDTO bountyDto, @AuthenticationPrincipal Jwt jwt) {
        Bounty created = bountyService.createBounty(bountyDto, jwt.getSubject());

        BountyDTO response = bountyService.getBountyById(created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        return ResponseEntity.ok(
                bountyService.getBountyById(id)
        );
    }

    @GetMapping("/repository/{repoId}")
    public List<BountyDTO> getBountiesByRepository(@PathVariable Long repoId) {
        return bountyService.getBountiesByRepository(repoId);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("@bountyPermissions.isBountyRepositoryOwner(" + "#id, authentication.name)")
    public ResponseEntity<Void> cancelBounty(@PathVariable Long id) {
        bountyService.cancelBounty(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<BountyDTO> claimBounty(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        return ResponseEntity.ok(
                bountyService.claimBounty(id, jwt.getSubject())
        );
    }

    @PostMapping("/{id}/unclaim")
    public ResponseEntity<BountyDTO> unclaimBounty(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new AccessDeniedException("Authentication is required");
        }

        return ResponseEntity.ok(
                bountyService.unclaimBounty(id, jwt.getSubject())
        );
    }
}