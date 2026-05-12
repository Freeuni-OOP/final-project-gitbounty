package org.gitbounty.gitbountybackend.repository;

import java.util.Optional;

import org.gitbounty.gitbountybackend.model.Codebase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CodebaseRepository extends JpaRepository<Codebase, Long> {

	Optional<Codebase> findByName(String name);
}


