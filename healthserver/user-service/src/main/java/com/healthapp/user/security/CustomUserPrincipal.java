package com.healthapp.user.security;

//import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Singular;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@Setter
@NoArgsConstructor
public class CustomUserPrincipal implements UserDetails {
    
    private String id;
    private String email;
    @Singular
    private Collection<GrantedAuthority> authorities;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;
    private boolean enabled = true;
    
    @Builder
    public CustomUserPrincipal(String id, String email, 
                               Collection<GrantedAuthority> authorities,
                               Boolean accountNonExpired, Boolean accountNonLocked,
                               Boolean credentialsNonExpired, Boolean enabled) {
        this.id = id;
        this.email = email;
        this.authorities = authorities;
        this.accountNonExpired = accountNonExpired != null ? accountNonExpired : true;
        this.accountNonLocked = accountNonLocked != null ? accountNonLocked : true;
        this.credentialsNonExpired = credentialsNonExpired != null ? credentialsNonExpired : true;
        this.enabled = enabled != null ? enabled : true;
    }
    
    @Override
    public String getUsername() {
        return email;
    }
    
    @Override
    public String getPassword() {
        return null; // Password not needed in this service
    }
    
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return authorities;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
}