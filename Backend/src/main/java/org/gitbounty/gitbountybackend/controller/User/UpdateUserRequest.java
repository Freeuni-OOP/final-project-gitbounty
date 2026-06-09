package org.gitbounty.gitbountybackend.controller.User;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UpdateUserRequest(
    @JsonProperty("username")
    String username,
    @JsonProperty("email")
    String email
) {
}

