package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.model.Bounty;
import org.gitbounty.gitbountybackend.model.BountyStatus;
import org.gitbounty.gitbountybackend.repository.BountyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BountyServiceTest {

    @Mock //"fake" repository
    private BountyRepository bountyRepository;

    @InjectMocks //put the repo in our service
    private BountyService bountyService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBounty_SaveBounty_WhenAmountPositive() {
        Bounty bounty = new Bounty("Fixx Bug", "Fix it please", 100.0, BountyStatus.OPEN);
        when(bountyRepository.save(any(Bounty.class))).thenReturn(bounty);

        //call the method we are testing
        Bounty savedBounty = bountyService.createBounty(bounty);

        //check that it worked and the save method was called
        assertNotNull(savedBounty);
        assertEquals("Fixx Bug", savedBounty.getTitle());
        verify(bountyRepository, times(1)).save(bounty);
    }

    @Test
    void createBounty_Exception_WhenAmountIsBad() {
        //bounty with invalid amount
        Bounty invalidBounty = new Bounty("Cheap Bug", "Too cheap", -10.0, BountyStatus.OPEN);

        //should be IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            bountyService.createBounty(invalidBounty);
        });

        //check that save method was not called
        verify(bountyRepository, never()).save(any());
    }

    @Test
    void getBountyById_ShouldReturnBounty_WhenIdExists() {
        Bounty bounty = new Bounty("Task", "Desc", 50.0, BountyStatus.OPEN);
        when(bountyRepository.findById(1L)).thenReturn(Optional.of(bounty));

        Bounty found = bountyService.getBountyById(1L);

        assertEquals("Task", found.getTitle());
    }
}