package com.setec.forum.feature.auth.service.impl;

import com.setec.forum.config.JwtTokenProvider;
import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.auth.*;
import com.setec.forum.feature.auth.dto.request.LoginRequest;
import com.setec.forum.feature.auth.dto.request.RegisterRequest;
import com.setec.forum.feature.auth.dto.response.AuthResponse;
import com.setec.forum.feature.auth.service.AuthService;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.mapper.AuthMapper;
import com.setec.forum.model.Role;
import com.setec.forum.model.User;
import com.setec.forum.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("forumAuthService")
@Primary
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = authMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);

        UserProfile profile = UserProfile.builder()
                .fullName(request.getFullName())
                .build();
        user.setProfile(profile);

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateTokenFromUsername(savedUser.getUsername());

        return authMapper.toAuthResponse(savedUser, token);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

            String token = jwtTokenProvider.generateToken(authentication);
            return authMapper.toAuthResponse(user, token);
        } catch (BadCredentialsException ex) {
            throw new UnauthorizedException("Invalid username or password");
        }
    }
}
