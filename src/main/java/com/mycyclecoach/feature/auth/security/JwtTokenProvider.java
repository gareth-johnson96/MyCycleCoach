package com.mycyclecoach.feature.auth.security;

import com.mycyclecoach.config.JwtConfig;
import com.mycyclecoach.feature.auth.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.signingKey = initializeSigningKey();
    }

    private SecretKey initializeSigningKey() {
        String secret = jwtConfig.getSecret();
        byte[] secretBytes = secret.getBytes();

        // Try to decode as Base64 first (for 512-bit Base64-encoded secrets)
        try {
            byte[] decodedSecret = Base64.getDecoder().decode(secret);
            if (decodedSecret.length >= 64) { // 512 bits
                log.info("Using Base64-decoded secret key");
                return Keys.hmacShaKeyFor(decodedSecret);
            }
        } catch (Exception e) {
            log.debug("Secret is not Base64-encoded, treating as raw bytes");
        }

        // If the secret is less than 512 bits (64 bytes), pad it
        if (secretBytes.length < 64) {
            log.warn("JWT secret is less than 512 bits. Padding with zeros to 512 bits for HS512 algorithm.");
            byte[] paddedSecret = new byte[64];
            System.arraycopy(secretBytes, 0, paddedSecret, 0, secretBytes.length);
            return Keys.hmacShaKeyFor(paddedSecret);
        }

        // For secrets that are at least 512 bits, use them directly
        return Keys.hmacShaKeyFor(secretBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    /**
     * Generate an access token for the given user ID
     */
    public String generateAccessToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getAccessTokenTtl());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Generate a refresh token for the given user ID
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getRefreshTokenTtl());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Get user ID from token
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Validate token and check if it's expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            throw new TokenExpiredException("Token has expired");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if token is expired without throwing exception
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.warn("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
