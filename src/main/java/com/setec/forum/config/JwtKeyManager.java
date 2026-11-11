package com.setec.forum.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

/**
 * Generates a random JWT signing key on first boot and persists it to a local file so it survives
 * restarts. Never hardcode a secret in application.yml for anything beyond local experimentation -
 * this class is the single source of truth for the signing key used by {@link JwtTokenProvider}.
 *
 * File location is controlled by `jwt.secret-file` (defaults to ./config/jwt-secret.key, relative
 * to the working directory the app is launched from). Add that path to .gitignore.
 */
@Slf4j
@Component
public class JwtKeyManager {

    @Value("${jwt.secret-file:./config/jwt-secret.key}")
    private String secretFilePath;

    @Getter
    private SecretKey signingKey;

    @PostConstruct
    public void init() throws IOException, NoSuchAlgorithmException {
        Path path = Path.of(secretFilePath);

        if (Files.exists(path)) {
            String encoded = Files.readString(path, StandardCharsets.UTF_8).trim();
            byte[] keyBytes = Base64.getDecoder().decode(encoded);
            this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
            log.info("Loaded existing JWT signing key from {}", path.toAbsolutePath());
            return;
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        keyGenerator.init(256);
        SecretKey generated = keyGenerator.generateKey();
        String encoded = Base64.getEncoder().encodeToString(generated.getEncoded());

        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, encoded, StandardCharsets.UTF_8);
        restrictPermissions(path);

        this.signingKey = generated;
        log.warn("Generated a new random JWT signing key and saved it to {}. " +
                        "All previously issued tokens (if any) are now invalid. " +
                        "Keep this file out of version control.",
                path.toAbsolutePath());
    }

    private void restrictPermissions(Path path) {
        try {
            Set<PosixFilePermission> readOnlyOwner = EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(path, readOnlyOwner);
        } catch (UnsupportedOperationException | IOException ex) {
            // Non-POSIX filesystem (e.g. Windows) - skip silently, this is a best-effort hardening step.
            log.debug("Could not restrict permissions on {}: {}", path, ex.getMessage());
        }
    }
}
