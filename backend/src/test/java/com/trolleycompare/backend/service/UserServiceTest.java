package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.CreateUserRequest;
import com.trolleycompare.backend.dto.UpdateProfileRequest;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.dto.UpdateUserRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository users;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void registrationCreatesRegularUserWithNormalizedEmailAndEncodedPassword() {
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwordEncoder);

        AppUser created = service.register(new CreateUserRequest("  Person  ", "PERSON@Example.com", "password123"));

        assertEquals("Person", created.getName());
        assertEquals("person@example.com", created.getEmail());
        assertEquals(AppUser.Role.USER, created.getRole());
        assertTrue(passwordEncoder.matches("password123", created.getPasswordHash()));
        assertNotEquals("password123", created.getPasswordHash());
    }

    @Test
    void registrationRejectsDuplicateEmail() {
        when(users.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);
        UserService service = new UserService(users, passwordEncoder);

        assertThrows(ResponseStatusException.class,
                () -> service.register(new CreateUserRequest("Person", "person@example.com", "password123")));
        verify(users, never()).save(any());
    }

    @Test
    void superAdminCannotBeDeletedByUserManagement() {
        AppUser superAdmin = new AppUser("Admin", "admin@example.com", "encoded");
        superAdmin.setRole(AppUser.Role.SUPER_ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(superAdmin));
        UserService service = new UserService(users, passwordEncoder);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L));
        verify(users, never()).delete(any(AppUser.class));
    }

    @Test
    void superAdminCanPromoteARegularUser() {
        AppUser user = new AppUser("Person", "person@example.com", "encoded");
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwordEncoder);

        AppUser updated = service.update(2L, 1L,
                new UpdateUserRequest("Person", "person@example.com", null, true, "SUPER_ADMIN"));

        assertEquals(AppUser.Role.SUPER_ADMIN, updated.getRole());
    }

    @Test
    void finalEnabledSuperAdminCannotBeDemoted() {
        AppUser admin = new AppUser("Admin", "admin@example.com", "encoded");
        admin.setRole(AppUser.Role.SUPER_ADMIN);
        ReflectionTestUtils.setField(admin, "id", 2L);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(users.countByRoleAndEnabledTrue(AppUser.Role.SUPER_ADMIN)).thenReturn(1L);
        UserService service = new UserService(users, passwordEncoder);

        assertThrows(ResponseStatusException.class, () -> service.update(2L, 1L,
                new UpdateUserRequest("Admin", "admin@example.com", null, true, "USER")));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void superAdminCannotChangeTheirOwnRole() {
        AppUser admin = new AppUser("Admin", "admin@example.com", "encoded");
        admin.setRole(AppUser.Role.SUPER_ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        UserService service = new UserService(users, passwordEncoder);

        assertThrows(ResponseStatusException.class, () -> service.update(1L, 1L,
                new UpdateUserRequest("Admin", "admin@example.com", null, true, "USER")));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void profileUpdateChangesUserDetailsWithoutChangingRole() {
        AppUser admin = new AppUser("Admin", "admin@example.com", "encoded");
        admin.setRole(AppUser.Role.SUPER_ADMIN);
        when(users.findById(1L)).thenReturn(Optional.of(admin));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwordEncoder);

        AppUser updated = service.updateProfile(1L,
                new UpdateProfileRequest("Updated Admin", "updated@example.com", null));

        assertEquals("Updated Admin", updated.getName());
        assertEquals("updated@example.com", updated.getEmail());
        assertEquals(AppUser.Role.SUPER_ADMIN, updated.getRole());
        assertEquals("encoded", updated.getPasswordHash());
    }
}
