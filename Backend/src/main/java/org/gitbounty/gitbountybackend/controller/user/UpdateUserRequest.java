package org.gitbounty.gitbountybackend.controller.user;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateUserRequest(
    @JsonProperty("username")
    String username,
    @JsonProperty("email")
    String email
) {
}

