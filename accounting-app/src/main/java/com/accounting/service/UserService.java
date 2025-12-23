package com.accounting.service;

import com.accounting.model.User;
import com.accounting.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String email, String password) {
        if (findByUsername(username).isPresent()) {
            throw new IllegalStateException("username exists");
        }
        User u = new User(username, email, encoder.encode(password));
        return userRepository.save(u);
    }

    public Optional<User> login(String username, String password) {
        Optional<User> u = findByUsername(username);
        if (u.isPresent() && encoder.matches(password, u.get().getPasswordHash())) {
            return u;
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }
}
