package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public User createUser(String username, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUserException("Username already exists: " + username);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("Email already exists: " + email);
        }

        return userRepository.save(new User(username, email));
    }
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}

