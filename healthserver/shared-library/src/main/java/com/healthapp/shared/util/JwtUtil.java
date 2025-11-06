package com.healthapp.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
//import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JwtUtil - Shared JWT utility for all microservices
 * ✅ Compatible with JJWT 0.12.3
 */
@Slf4j
public class JwtUtil {

    /**
     * Create SecretKey from string secret
     */
    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract all claims from a JWT token
     * ✅ FIXED: Uses .parser() instead of .parserBuilder() for JJWT 0.12.3
     */
    public static Claims extractAllClaims(String token, String secret) {
        SecretKey key = getSigningKey(secret);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract username/email from token
     */
    public static String extractUsername(String token, String secret) {
        return extractAllClaims(token, secret).getSubject();
    }

    /**
     * Extract email from token (alias for extractUsername)
     */
    public static String extractEmail(String token, String secret) {
        return extractUsername(token, secret);
    }

    /**
     * Extract userId from token
     * ✅ CORRECTION : Essaie "userId" puis "user_id" pour compatibilité
     */
    public static String extractUserId(String token, String secret) {
        Claims claims = extractAllClaims(token, secret);
        
        // Essayer d'abord "userId" (camelCase)
        String userId = claims.get("userId", String.class);
        if (userId != null) {
            return userId;
        }
        
        // Sinon essayer "user_id" (snake_case)
        userId = claims.get("user_id", String.class);
        if (userId != null) {
            return userId;
        }
        
        // Si aucun des deux, retourner le subject
        log.warn("⚠️ No userId found in token, using subject as fallback");
        return claims.getSubject();
    }

    /**
     * Extract roles from token
     */
    @SuppressWarnings("unchecked")
    public static List<String> extractRoles(String token, String secret) {
        return extractAllClaims(token, secret).get("roles", List.class);
    }

    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token, String secret) {
        try {
            return extractAllClaims(token, secret)
                    .getExpiration()
                    .before(new Date());
        } catch (Exception e) {
            log.error("❌ Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Validate token (with username check)
     */
    public static boolean isTokenValid(String token, String username, String secret) {
        try {
            final String extractedUsername = extractUsername(token, secret);
            return extractedUsername.equals(username) && !isTokenExpired(token, secret);
        } catch (Exception e) {
            log.error("❌ Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validate token (without username check)
     */
    public static boolean validateToken(String token, String secret) {
        try {
            extractAllClaims(token, secret);
            return !isTokenExpired(token, secret);
        } catch (Exception e) {
            log.error("❌ Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Generate JWT token with claims
     * @param claims Map of custom claims
     * @param subject Token subject (usually email or username)
     * @param expirationMillis Expiration time in milliseconds
     * @param secret Secret key for signing
     * @return Generated JWT token
     */
    public static String generateToken(Map<String, Object> claims, String subject, Long expirationMillis, String secret) {
        //SecretKey key = getSigningKey(secret);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiry)
                //.signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Generate JWT token (simplified)
     * ✅ NOUVEAU : Méthode simplifiée pour générer un token
     */
    public static String generateToken(String email, String userId, List<String> roles, String secret, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);      // ✅ Utilise "userId" (camelCase)
        claims.put("roles", roles);
        
        log.info("🔐 Generating JWT for email: {}, userId: {}, roles: {}", email, userId, roles);
        
        return generateToken(claims, email, expiration, secret);
    }
    
    /**
     * Generate refresh token
     * ✅ NOUVEAU : Méthode pour générer un refresh token
     */
    public static String generateRefreshToken(String email, String userId, String secret, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");
        
        return generateToken(claims, email, expiration, secret);
    }
}