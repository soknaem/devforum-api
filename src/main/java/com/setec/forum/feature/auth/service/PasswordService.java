package com.setec.forum.feature.auth.service;

import com.setec.forum.feature.auth.*;

public interface PasswordService {
    void requestReset(String email);
    void resetPassword(String secret, String newPassword);
    void changePassword(String username, String currentPassword, String newPassword);
    void purgeExpiredTokens();
}
