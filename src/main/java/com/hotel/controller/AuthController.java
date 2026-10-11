package com.hotel.controller;

import com.hotel.dto.RegisterRequest;
import com.hotel.model.User;
import com.hotel.repository.UserRepository;
import com.hotel.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Login and logout are handled by Spring Security (see SecurityConfig):
 *   POST /api/auth/login   POST /api/auth/logout
 * This controller only handles registration and "who am I".
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest req) {
        if (req.name() == null || req.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required.");
        if (req.email() == null || !EMAIL.matcher(req.email().trim()).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
        if (req.password() == null || req.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
        if (userService.emailExists(req.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");

        userService.registerUser(req.name(), req.email(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created. Please sign in."));
    }

    /** Frontend calls this on every page load to know if someone is logged in. */
    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            out.put("authenticated", false);
            return out;
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            out.put("authenticated", false);
            return out;
        }
        out.put("authenticated", true);
        out.put("name", user.getName());
        out.put("email", user.getEmail());
        out.put("role", user.getRole());
        return out;
    }
}
