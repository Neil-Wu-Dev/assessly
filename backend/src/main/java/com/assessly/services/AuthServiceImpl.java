package com.assessly.services;

import com.assessly.exceptions.AuthenticationException;
import com.assessly.models.UserAccount;
import com.assessly.repositories.interfaces.UserRepository;
import com.assessly.services.interfaces.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UUID> sessions = new ConcurrentHashMap<>();

    public AuthServiceImpl(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount register(String email, String password) {
        UserAccount.validateEmail(email);
        UserAccount.validatePassword(password);
        String normalized = email.toLowerCase();
        if (users.existsByEmail(normalized)) throw new AuthenticationException("Email is already registered.");
        return users.save(new UserAccount(normalized, passwordEncoder.encode(password)));
    }

    public UserAccount login(String email, String password) {
        UserAccount.validateEmail(email);
        return users.findByEmail(email.toLowerCase())
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .orElseThrow(() -> new AuthenticationException("Email or password is incorrect."));
    }

    public UserAccount requireUser(String sessionToken) {
        UUID userId = sessions.get(sessionToken);
        if (userId == null) throw new AuthenticationException("Authentication session is missing or invalid.");
        return users.findById(userId).orElseThrow(() -> new AuthenticationException("Authenticated user no longer exists."));
    }

    public String createSession(UserAccount user) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, user.getId());
        return token;
    }
}
