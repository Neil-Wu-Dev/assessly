package com.assessly.mappers;

import com.assessly.models.UserAccount;
import com.assessly.schemas.AuthSchemas.AuthResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {
    public AuthResponse toResponse(UserAccount user, String token) {
        return new AuthResponse(user.getId(), user.getEmail(), token);
    }
}
