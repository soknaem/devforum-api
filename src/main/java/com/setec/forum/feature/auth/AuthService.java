package com.setec.forum.feature.auth;

import com.setec.forum.feature.auth.dto.request.LoginRequest;
import com.setec.forum.feature.auth.dto.request.RegisterRequest;
import com.setec.forum.feature.auth.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
