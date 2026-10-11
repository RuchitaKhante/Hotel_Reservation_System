package com.hotel.service;

import com.hotel.model.User;
import com.hotel.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(normalize(email)).isPresent();
    }

    public User registerUser(String name, String email, String password) {
        User user = new User();
        user.setName(name.trim());
        user.setEmail(normalize(email));
        user.setPassword(passwordEncoder.encode(password)); // BCrypt hash, never plain text
        user.setRole("ROLE_USER");                          // default role
        return userRepository.save(user);
    }
}
