package com.setec.forum.feature.auth.service.impl;

import com.setec.forum.exception.UnauthorizedException;
import com.setec.forum.feature.auth.*;
import com.setec.forum.feature.auth.service.PasswordService;
import com.setec.forum.feature.mail.service.EmailService;
import com.setec.forum.feature.resource.UserRepository;
import com.setec.forum.model.PasswordResetToken;
import com.setec.forum.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forgot / reset / change password.
 *
 *  - Asking for a reset NEVER reveals whether an e-mail is registered (same answer either way).
 *  - The link carries a random secret; only its SHA-256 hash is stored; it works once, for 30 minutes.
 *  - At most one reset mail per minute per account.
 *  - A successful reset invalidates every other open reset link of that user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private static final int TOKEN_BYTES = 32;
    private static final long VALID_MINUTES = 30;
    private static final long COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    private final SecureRandom random = new SecureRandom();

    // ------------------------------------------------------------------ forgot

    @Transactional
    @Override
    public void requestReset(String email) {
        String normalized = email == null ? "" : email.trim();
        Optional<User> found = userRepository.findByEmail(normalized);
        if (found.isEmpty()) {
            found = userRepository.findByEmail(normalized.toLowerCase());
        }
        if (found.isEmpty()) {
            return; // silently: do not reveal which e-mails exist
        }

        User user = found.get();

        // One mail per minute is plenty and stops the form being used to spam somebody.
        Optional<PasswordResetToken> last = tokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
        if (last.isPresent() && last.get().getCreatedAt() != null
                && last.get().getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(COOLDOWN_SECONDS))) {
            return;
        }

        tokenRepository.deleteByUserId(user.getId()); // older links stop working

        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(secret))
                .expiresAt(LocalDateTime.now().plusMinutes(VALID_MINUTES))
                .build());

        String link = frontendUrl + "/reset-password?token=" + secret;
        String to = user.getEmail();
        String name = user.getUsername();

        // Off the request thread: SMTP can be slow, and a slow answer would reveal that the account exists.
        CompletableFuture.runAsync(() -> emailService.send(to, "Reset your DevForum password",
                "Hi " + name + ",\n\n"
                        + "Somebody asked to reset the password of your DevForum account.\n"
                        + "Open this link within " + VALID_MINUTES + " minutes to choose a new password:\n\n"
                        + link + "\n\n"
                        + "If this was not you, ignore this e-mail - your password stays the same."));
    }

    // ------------------------------------------------------------------- reset

    @Transactional
    @Override
    public void resetPassword(String secret, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(secret == null ? "" : secret.trim()))
                .orElseThrow(() -> new IllegalArgumentException("This reset link is invalid or has expired"));

        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link is invalid or has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);
        // Any other link that was requested meanwhile is dead too.
        tokenRepository.deleteByUserId(user.getId());
    }

    // ------------------------------------------------------------------ change

    @Transactional
    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("Choose a password that is different from the current one");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.deleteByUserId(user.getId());
    }

    // ----------------------------------------------------------------- cleanup

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    @Override
    public void purgeExpiredTokens() {
        int removed = tokenRepository.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Removed {} expired password reset token(s)", removed);
        }
    }

    private String hash(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
