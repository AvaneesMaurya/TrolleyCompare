package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.CreateUserRequest;
import com.trolleycompare.backend.dto.UpdateUserRequest;
import com.trolleycompare.backend.dto.UpdateProfileRequest;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UserService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser register(CreateUserRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User details are required.");
        }
        String name = requiredText(request.name(), "Name", 100);
        String email = normalizeEmail(request.email());
        String password = validatePassword(request.password());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        return users.save(new AppUser(name, email, passwordEncoder.encode(password)));
    }

    @Transactional(readOnly = true)
    public List<AppUser> findAll() {
        return users.findAll().stream()
                .sorted((left, right) -> left.getEmail().compareToIgnoreCase(right.getEmail()))
                .toList();
    }

    @Transactional
    public AppUser updateProfile(Long userId, UpdateProfileRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile details are required.");
        }
        AppUser user = findById(userId);
        user.setName(requiredText(request.name(), "Name", 100));
        String email = normalizeEmail(request.email());
        if (!user.getEmail().equals(email) && users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        user.setEmail(email);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(validatePassword(request.password())));
        }
        return users.save(user);
    }

    @Transactional
    public AppUser update(Long id, Long requesterId, UpdateUserRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User details are required.");
        }
        AppUser user = findById(id);
        if (Objects.equals(id, requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot change your own account.");
        }

        AppUser.Role updatedRole = request.role() == null
                ? user.getRole()
                : parseRole(request.role());
        boolean updatedEnabled = request.enabled() == null ? user.isEnabled() : request.enabled();
        if (user.getRole() == AppUser.Role.SUPER_ADMIN && user.isEnabled()
                && (updatedRole != AppUser.Role.SUPER_ADMIN || !updatedEnabled)
                && users.countByRoleAndEnabledTrue(AppUser.Role.SUPER_ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "At least one enabled super-admin account must remain.");
        }

        user.setName(requiredText(request.name(), "Name", 100));
        String email = normalizeEmail(request.email());
        if (!user.getEmail().equals(email) && users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        user.setEmail(email);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(validatePassword(request.password())));
        }
        user.setRole(updatedRole);
        user.setEnabled(updatedEnabled);
        return users.save(user);
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = findById(id);
        if (user.getRole() == AppUser.Role.SUPER_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The super-admin account cannot be deleted here.");
        }
        users.delete(user);
    }

    @Transactional(readOnly = true)
    public AppUser findById(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
    }

    private String requiredText(String value, String field, int maxLength) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " is required and must be at most " + maxLength + " characters.");
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required.");
        }
        String email = value.trim().toLowerCase(Locale.ROOT);
        if (email.length() > 254 || !isValidEmail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid email is required.");
        }
        return email;
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex != email.lastIndexOf('@')) {
            return false;
        }

        int domainDotIndex = email.indexOf('.', atIndex + 2);
        return domainDotIndex >= 0
                && domainDotIndex < email.length() - 1
                && email.chars().noneMatch(Character::isWhitespace);
    }

    private String validatePassword(String password) {
        if (password == null || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters and no more than 72 UTF-8 bytes.");
        }
        return password;
    }

    private AppUser.Role parseRole(String role) {
        try {
            return AppUser.Role.valueOf(role);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or SUPER_ADMIN.");
        }
    }
}
