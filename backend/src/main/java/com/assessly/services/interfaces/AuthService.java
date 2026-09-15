package com.assessly.services.interfaces;

import com.assessly.models.UserAccount;

public interface AuthService {
    UserAccount register(String email, String password);
    UserAccount login(String email, String password);
    UserAccount requireUser(String sessionToken);
    String createSession(UserAccount user);
    void logout(String sessionToken);
}
