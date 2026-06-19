package org.gitbounty.gitbountybackend.service;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
//import org.gitbounty.gitbountybackend.service.User.UserRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BountyService {

    private final BountyRepository bountyRepository;
    private final IssueRepository issueRepository;
    //private final UserRepository userRepository;

    @Autowired
    public BountyService(BountyRepository bountyRepository, IssueRepository issueRepository //, UserRepository userRepository
    ) {
        this.bountyRepository = bountyRepository;
        this.issueRepository = issueRepository;
        //this.userRepository = userRepository;
    }

    @Transactional
    public Bounty createBountyWithPermission(BountyDTO dto, String userId) {
        //validate Issue Existence
        Issue issue = issueRepository.findById(dto.getIssueId()).orElseThrow(() -> new IllegalArgumentException("Issue not found with id: " + dto.getIssueId()));

        //User owner = userRepository.findByKeycloakId(userId).orElseThrow(() -> new IllegalArgumentException("User not found with Keycloak ID: " + userId));

        //if (owner.getBalance() < dto.getAmount()) {
        //    throw new IllegalArgumentException("Insufficient funds in wallet to put bounty into escrow.");
        //}

        //owner.setBalance(owner.getBalance()-dto.getAmount());
        //userRepository.save(owner);

        // create the Bounty Entity
        Bounty bounty = new Bounty();
        bounty.setTitle(dto.getTitle());
        bounty.setDescription(dto.getDescription());
        bounty.setAmount(dto.getAmount());
        bounty.setStatus(BountyStatus.OPEN);
        bounty.setIssue(issue);
        bounty.setCreatedAt(java.time.LocalDateTime.now());

        return bountyRepository.save(bounty);
    }

    public Bounty createBounty(Bounty bounty) {
        if (bounty.getAmount() <= 0) { throw new IllegalArgumentException("Bounty amount needs to be greater than 0"); }
        return bountyRepository.save(bounty);
    }

    public List<BountyDTO> getAllBounties() {
        return bountyRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<BountyDTO> getBountiesByStatus(BountyStatus status) {
        return bountyRepository.findByStatus(status).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public BountyDTO getBountyById(Long id) {
        Bounty bounty = bountyRepository.findById(id).orElseThrow(() -> new RuntimeException("Bounty not found"));
        return convertToDto(bounty);
    }

    private BountyDTO convertToDto(Bounty bounty) {
        BountyDTO dto = new BountyDTO();
        dto.setId(bounty.getId());
        dto.setTitle(bounty.getTitle());
        dto.setAmount(bounty.getAmount());
        dto.setStatus(bounty.getStatus());
        if (bounty.getIssue() != null) dto.setIssueId(bounty.getIssue().getId());
        return dto;
    }

    private Bounty convertToEntity(BountyDTO dto) {
        Bounty bounty = new Bounty();
        bounty.setTitle(dto.getTitle());
        bounty.setAmount(dto.getAmount());
        bounty.setStatus(dto.getStatus());
        return bounty;
    }

    public void closeBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId).orElseThrow(() -> new RuntimeException("Bounty not found"));
        bounty.setStatus(BountyStatus.COMPLETED);
        bountyRepository.save(bounty);

        if (bounty.getIssue() != null) {
            Issue issue = bounty.getIssue();
            issue.setStatus(IssueStatus.CLOSED);
            issueRepository.save(issue);
        }
    }

    public void closeIssue(Long issueId) {
        bountyRepository.findByIssueId(issueId).ifPresent(bounty -> {
            bounty.setStatus(BountyStatus.COMPLETED);
            bountyRepository.save(bounty);
        });
    }
}