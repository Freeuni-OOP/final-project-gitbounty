package org.gitbounty.gitbountybackend.controller;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.service.BountyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController //tells spring this class handles HTTP requests
@RequestMapping("/api/bounties") //all URLs for this controller start with /api/bounties
public class BountyController {

    private final BountyService bountyService;

    @Autowired
    public BountyController(BountyService bountyService) { this.bountyService = bountyService; }

    //create a new bounty: POST http://localhost:8080/api/bounties
    @PostMapping
    @PreAuthorize("@bountyPermissions.isIssueRepositoryOwner(#bountyDto.issueId, authentication.name)")
    public ResponseEntity<Bounty> createBounty(@RequestBody BountyDTO bountyDto) {
            Bounty created = bountyService.createBountyWithPermission(bountyDto, null);
         return ResponseEntity.ok(created);
    }

    @GetMapping
    public List<BountyDTO> getAllBounties() { return bountyService.getAllBounties(); }

    //get bounties by status: GET http://localhost:8080/api/bounties/status/{status}
    @GetMapping("/status/{status}")
    public List<BountyDTO> getBountiesByStatus(@PathVariable BountyStatus status) {
        return bountyService.getBountiesByStatus(status);
    }

    //get a single bounty: GET http://localhost:8080/api/bounties/{id}
    @GetMapping("/{id}")
    public ResponseEntity<BountyDTO> getBountyById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(bountyService.getBountyById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build(); //returns 404 if id does not exist
        }
    }
}