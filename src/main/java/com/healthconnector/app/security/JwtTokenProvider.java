package com.healthconnector.app.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Provides JWT creation, parsing, and validation.
 * Tokens include: userId, email, role, organizationId, sessionId.
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Generate a short-lived access token.
     */
    public String generateAccessToken(String userId, String email, String role, String organizationId) {
        String sessionId = UUID.randomUUID().toString();
        return Jwts.builder()
                .subject(userId)
                .claim(SecurityConstants.CLAIM_USER_ID,      userId)
                .claim(SecurityConstants.CLAIM_EMAIL,        email)
                .claim(SecurityConstants.CLAIM_ROLE,         role)
                .claim(SecurityConstants.CLAIM_ORGANIZATION, organizationId)
                .claim(SecurityConstants.CLAIM_SESSION_ID,   sessionId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generate a long-lived refresh token (userId only).
     */
    public String generateRefreshToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .claim(SecurityConstants.CLAIM_USER_ID, userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUserIdFromToken(String token) {
        return parseAllClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseAllClaims(token).get(SecurityConstants.CLAIM_ROLE, String.class);
    }

    public boolean validateToken(String token) {
        try {
            parseAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
        } catch (UnsupportedJwtException e) {
        } catch (MalformedJwtException e) {
        } catch (SecurityException e) {
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseAllClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }
}
