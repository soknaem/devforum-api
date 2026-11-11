package com.setec.forum.feature.auth;

import com.setec.forum.base.ApiResponse;
import com.setec.forum.feature.auth.dto.request.ChangePasswordRequest;
import com.setec.forum.feature.auth.dto.request.ForgotPasswordRequest;
import com.setec.forum.feature.auth.dto.request.LoginRequest;
import com.setec.forum.feature.auth.dto.request.ResetPasswordRequest;
import com.setec.forum.feature.auth.dto.request.RegisterRequest;
import com.setec.forum.feature.auth.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final Duration ACCESS_TOKEN_MAX_AGE = Duration.ofDays(1);

    private final AuthService authService;
    private final PasswordService passwordService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthResponse auth = authService.register(request);

        addAccessTokenCookie(response, auth.getToken());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "User registered successfully",
                                publicResponse(auth)
                        )
                );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        AuthResponse auth = authService.login(request);

        addAccessTokenCookie(response, auth.getToken());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Login successful",
                        publicResponse(auth)
                )
        );
    }

    /**
     * Always answers the same way, whether or not the e-mail exists (no account enumeration).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.requestReset(request.email());
        return ResponseEntity.ok(ApiResponse.success(
                "If that e-mail is registered, a reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed. You can sign in now.", null));
    }

    /** Signed-in users only (see SecurityConfig). */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication) {
        passwordService.changePassword(authentication.getName(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletResponse response
    ) {
        ResponseCookie cookie = ResponseCookie
                .from(ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false) // localhost; set true in HTTPS production
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Logout successful",
                        null
                )
        );
    }

    private void addAccessTokenCookie(
            HttpServletResponse response,
            String token
    ) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Authentication token was not generated"
            );
        }

        ResponseCookie cookie = ResponseCookie
                .from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false) // localhost; set true in HTTPS production
                .sameSite("Lax")
                .path("/")
                .maxAge(ACCESS_TOKEN_MAX_AGE)
                .build();

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookie.toString()
        );
    }

    private AuthResponse publicResponse(AuthResponse auth) {
        return AuthResponse.builder()
                .userId(auth.getUserId())
                .username(auth.getUsername())
                .email(auth.getEmail())
                .build();
    }
}