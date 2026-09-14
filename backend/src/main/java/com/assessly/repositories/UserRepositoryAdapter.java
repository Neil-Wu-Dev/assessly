package com.assessly.repositories;

import com.assessly.models.UserAccount;
import com.assessly.repositories.interfaces.UserRepository;
import com.assessly.repositories.jpa.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
class UserRepositoryAdapter implements UserRepository {
    private final UserJpaRepository jpa;

    UserRepositoryAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    public UserAccount save(UserAccount user) { return jpa.save(user); }
    public Optional<UserAccount> findByEmail(String email) { return jpa.findByEmail(email); }
    public Optional<UserAccount> findById(UUID id) { return jpa.findById(id); }
    public boolean existsByEmail(String email) { return jpa.existsByEmail(email); }
}
