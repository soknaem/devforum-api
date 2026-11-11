package com.setec.forum.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Issues and validates JWTs using the signing key managed by {@link JwtKeyManager} (randomly
 * generated on first boot and persisted locally - see that class for details).
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtKeyManager jwtKeyManager;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    protected void init() {
        this.signingKey = jwtKeyManager.getSigningKey();
    }

    public String generateToken(Authentication authentication) {
        return generateTokenFromUsername(authentication.getName());
    }

    public String generateTokenFromUsername(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // jjwt 0.12.x fluent API: subject()/issuedAt()/expiration() instead of the old
        // setSubject()/setIssuedAt()/setExpiration(); signWith(key) infers the algorithm.
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        // jjwt 0.12.x: Jwts.parser() (not parserBuilder()), verifyWith() (not setSigningKey()),
        // parseSignedClaims() + getPayload() (not parseClaimsJws() + getBody()).
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
