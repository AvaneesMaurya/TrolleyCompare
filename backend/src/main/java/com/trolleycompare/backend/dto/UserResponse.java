package com.trolleycompare.backend.dto;

import com.trolleycompare.backend.model.AppUser;

public record UserResponse(Long id, String name, String email, String role, boolean enabled) {
    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled());
    }
}
