package org.gitbounty.gitbountybackend.repository;
import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BountyRepository extends JpaRepository<Bounty, Long> {

    //spring already provides:
    //.save(Bounty b)
    //.findById(Long id)
    //.findAll()
    //.deleteById(Long id)

    List<Bounty> findByStatus(BountyStatus status);
}