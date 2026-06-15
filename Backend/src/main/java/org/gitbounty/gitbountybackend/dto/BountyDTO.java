package org.gitbounty.gitbountybackend.dto;

import lombok.Data;
import org.gitbounty.gitbountybackend.model.BountyStatus;

@Data
public class BountyDTO {
    private Long id;
    private String title;
    private Double amount;
    private BountyStatus status;
    private Long issueId; //only id of the issue
}