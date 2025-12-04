package com.healthapp.user.security;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

@Getter
@Setter
@ToString
public class CustomUserPrincipal {

    // ✅ ID MongoDB
    private String id;

    // ✅ ID Keycloak
    private String keycloakId;

    // ✅ Email
    private String email;

    // ✅ Username
    private String username;

    // ✅ Rôles Spring Security
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * ✅ SIGNATURE 100% COMPATIBLE SPRING
     */
    public static CustomUserPrincipal fromJwt(
            Jwt jwt,
            Collection<? extends GrantedAuthority> authorities
    ) {
        CustomUserPrincipal principal = new CustomUserPrincipal();

        principal.setEmail(jwt.getClaimAsString("email"));
        principal.setUsername(jwt.getClaimAsString("preferred_username"));
        principal.setAuthorities(authorities);

        return principal;
    }
}
