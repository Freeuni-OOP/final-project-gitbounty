package org.gitbounty.gitbountybackend.service.User;

import java.util.Optional;

import org.gitbounty.gitbountybackend.model.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    /**
     * Find a user by Keycloak ID.
     *
     * @param keycloakId the Keycloak ID to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByKeycloakId(String keycloakId);

    @Override
    @NullMarked
    Optional<User> findById(Long id);

    boolean existsByKeycloakId(String keycloakId);
}

