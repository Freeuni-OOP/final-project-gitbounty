package org.gitbounty.gitbountybackend.service;

import org.gitbounty.gitbountybackend.exception.DuplicateUserException;
import org.gitbounty.gitbountybackend.model.User;
import org.gitbounty.gitbountybackend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User createUser(String username, String email) {
        return createUser(username, email, null);
    }

    public User createUser(String username, String email, String rawPassword) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUserException("Username already exists: " + username);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateUserException("Email already exists: " + email);
        }

        String encodedPassword = rawPassword == null ? null : passwordEncoder.encode(rawPassword);
        return userRepository.save(new User(username, email, encodedPassword));
    }
}

