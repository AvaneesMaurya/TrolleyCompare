package com.trolleycompare.backend.security;

import com.trolleycompare.backend.dto.UserResponse;
import com.trolleycompare.backend.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public record UserPrincipal(
        Long id,
        String name,
        String email,
        String passwordHash,
        AppUser.Role role,
        boolean enabled) implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public UserPrincipal(AppUser user) {
        this(user.getId(), user.getName(), user.getEmail(), user.getPasswordHash(), user.getRole(), user.isEnabled());
    }

    public UserResponse toResponse() {
        return new UserResponse(id, name, email, role.name(), enabled);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
