package org.gitbounty.gitbountybackend.service;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BountyService {

    private final BountyRepository bountyRepository;

    @Autowired
    public BountyService(BountyRepository bountyRepository) { this.bountyRepository = bountyRepository; }

    public Bounty createBounty(Bounty bounty) {
        if (bounty.getAmount() <= 0) { throw new IllegalArgumentException("Bounty amount needs to be greater than 0"); }
        return bountyRepository.save(bounty);
    }

    public List<Bounty> getAllBounties() { return bountyRepository.findAll(); }

    public List<Bounty> getBountiesByStatus(BountyStatus status) { return bountyRepository.findByStatus(status); }

    public Bounty getBountyById(Long id) {
        return bountyRepository.findById(id).orElseThrow(() -> new RuntimeException("Bounty not found with the id: " + id));
    }
}