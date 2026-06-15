package org.gitbounty.gitbountybackend.service;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.dto.BountyDTO;
import org.gitbounty.gitbountybackend.model.Issue;
import org.gitbounty.gitbountybackend.model.IssueStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.gitbounty.gitbountybackend.service.codebase.issue.IssueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BountyService {

    private final BountyRepository bountyRepository;
    private IssueRepository issueRepository;

    @Autowired
    public BountyService(BountyRepository bountyRepository) { this.bountyRepository = bountyRepository; }

    public Bounty createBounty(Bounty bounty) {
        if (bounty.getAmount() <= 0) { throw new IllegalArgumentException("Bounty amount needs to be greater than 0"); }
        return bountyRepository.save(bounty);
    }

    public List<BountyDTO> getAllBounties() {
        return bountyRepository.findAll().stream().map(this::convertToDto).collect(java.util.stream.Collectors.toList());
    }

    public List<BountyDTO> getBountiesByStatus(BountyStatus status) {
        return bountyRepository.findByStatus(status).stream().map(this::convertToDto).collect(java.util.stream.Collectors.toList());
    }

    public BountyDTO getBountyById(Long id) {
        Bounty bounty = bountyRepository.findById(id).orElseThrow(() -> new RuntimeException("Bounty not found"));
        return convertToDto(bounty);
    }

    //convert entity to dto
    private BountyDTO convertToDto(Bounty bounty) {
        BountyDTO dto = new BountyDTO();
        dto.setId(bounty.getId());
        dto.setTitle(bounty.getTitle());
        dto.setAmount(bounty.getAmount());
        dto.setStatus(bounty.getStatus());
        if (bounty.getIssue() != null) dto.setIssueId(bounty.getIssue().getId());
        return dto;
    }

    //convert dto to Entity
    private Bounty convertToEntity(BountyDTO dto) {
        Bounty bounty = new Bounty();
        bounty.setTitle(dto.getTitle());
        bounty.setAmount(dto.getAmount());
        bounty.setStatus(dto.getStatus());
        //linking the Issue will happen via a separate call to IssueRepository
        return bounty;
    }

    public void closeBounty(Long bountyId) {
        Bounty bounty = bountyRepository.findById(bountyId).orElseThrow(() -> new RuntimeException("Bounty not found"));

        //close the Bounty
        bounty.setStatus(BountyStatus.COMPLETED);
        bountyRepository.save(bounty);

        //close the linked Issue
        if (bounty.getIssue() != null) {
            Issue issue = bounty.getIssue();
            issue.setStatus(IssueStatus.CLOSED);
        }
    }

    public void closeIssue(Long issueId) {
        //find the bounty linked to this issue and close it
        bountyRepository.findByIssueId(issueId).ifPresent(bounty -> {
            bounty.setStatus(BountyStatus.COMPLETED);
            bountyRepository.save(bounty);
        });
    }
}