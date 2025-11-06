package com.healthapp.auth.security;

import com.healthapp.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;  // ✅ AJOUT DE L'IMPORT
import java.util.stream.Collectors;

@Service
@Slf4j
public class JwtSecurity {
    
    @Value("${app.jwt.secret}")
    private String secretKey;
    
    @Value("${app.jwt.expiration}")
    private long jwtExpiration;
    
    @Value("${app.jwt.refresh-token.expiration}")
    private long refreshExpiration;
    
    public String generateToken(User user) {
        Map<String, Object> claims = buildAccessTokenClaims(user);
        return buildTokenWithEmail(claims, user.getEmail(), jwtExpiration);
    }
    
    public String generateRefreshToken(User user) {
        return buildTokenWithEmail(new HashMap<>(), user.getEmail(), refreshExpiration);
    }
    
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    public long getAccessTokenExpiration() {
        return jwtExpiration;
    }
    
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }
    
    private String buildTokenWithEmail(
            Map<String, Object> extraClaims,
            String email,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(email)
                .issuer("health-app")
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSignInKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }
    
    private SecretKey getSignInKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private Map<String, Object> buildAccessTokenClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId());
        claims.put("email", user.getEmail());
        claims.put("full_name", user.getFullName());
        
        List<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toList());
        claims.put("roles", roles);
        
        claims.put("is_activated", user.getIsActivated());
        claims.put("account_status", user.getAccountStatus().name());
        
        return claims;
    }
}