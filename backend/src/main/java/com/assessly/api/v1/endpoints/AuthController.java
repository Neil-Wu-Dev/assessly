package com.assessly.api.v1.endpoints;

import com.assessly.mappers.AuthMapper;
import com.assessly.models.UserAccount;
import com.assessly.schemas.AuthSchemas.*;
import com.assessly.services.interfaces.ApiKeySessionService;
import com.assessly.services.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final ApiKeySessionService apiKeys;
    private final AuthMapper mapper;

    public AuthController(AuthService auth, ApiKeySessionService apiKeys, AuthMapper mapper) {
        this.auth = auth;
        this.apiKeys = apiKeys;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        UserAccount user = auth.register(request.email(), request.password());
        return mapper.toResponse(user, auth.createSession(user));
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserAccount user = auth.login(request.email(), request.password());
        return mapper.toResponse(user, auth.createSession(user));
    }

    @PostMapping("/ai-session")
    ApiSessionResponse connectAiProvider(@RequestHeader("X-Assessly-Session") String token, @Valid @RequestBody ApiKeyRequest request) {
        var session = apiKeys.connect(auth.requireUser(token).getId(), request.providerName(), request.baseUrl(), request.modelName(), request.apiKey());
        return new ApiSessionResponse(session.status(), session.providerName(), session.baseUrl(), session.modelName(), session.expiresAt(), session.remainingSeconds(), session.message());
    }

    @GetMapping("/ai-session")
    ApiSessionResponse aiProviderStatus(@RequestHeader("X-Assessly-Session") String token) {
        var session = apiKeys.status(auth.requireUser(token).getId());
        return new ApiSessionResponse(session.status(), session.providerName(), session.baseUrl(), session.modelName(), session.expiresAt(), session.remainingSeconds(), session.message());
    }
}
