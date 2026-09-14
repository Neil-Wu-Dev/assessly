package com.assessly.repositories.interfaces;

import com.assessly.models.UserAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    UserAccount save(UserAccount user);
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findById(UUID id);
    boolean existsByEmail(String email);
}
