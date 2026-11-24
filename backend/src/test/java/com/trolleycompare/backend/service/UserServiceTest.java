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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

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

    @Test
    void registrationRejectsMissingOrInvalidFieldsAndPasswordsOverTheBcryptByteLimit() {
        UserService service = new UserService(users, passwordEncoder);

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.register(null));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.register(new CreateUserRequest(" ", "valid@example.com", "password123")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.register(new CreateUserRequest("x".repeat(101), "valid@example.com", "password123")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.register(new CreateUserRequest("Person", "not-an-email", "password123")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.register(new CreateUserRequest("Person", "person@example.com", "short")));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.register(new CreateUserRequest("Person", "person@example.com", "😀".repeat(19))));
        verifyNoInteractions(users);
    }

    @Test
    void findAllSortsAccountsByEmailIgnoringCase() {
        AppUser zed = new AppUser("Zed", "zed@example.com", "encoded");
        AppUser alice = new AppUser("Alice", "alice@example.com", "encoded");
        when(users.findAll()).thenReturn(List.of(zed, alice));

        assertEquals(List.of(alice, zed), new UserService(users, passwordEncoder).findAll());
    }

    @Test
    void profileUpdateRejectsMissingUsersInvalidFieldsAndDuplicateEmail() {
        UserService service = new UserService(users, passwordEncoder);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.updateProfile(1L, null));
        assertStatus(HttpStatus.NOT_FOUND,
                () -> service.updateProfile(1L, new UpdateProfileRequest("Person", "person@example.com", null)));

        AppUser person = new AppUser("Person", "person@example.com", "encoded");
        when(users.findById(1L)).thenReturn(Optional.of(person));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.updateProfile(1L, new UpdateProfileRequest("", "person@example.com", null)));
        assertStatus(HttpStatus.BAD_REQUEST,
                () -> service.updateProfile(1L, new UpdateProfileRequest("Person", "bad-email", null)));

        when(users.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT,
                () -> service.updateProfile(1L, new UpdateProfileRequest("Person", "taken@example.com", null)));
        verify(users, never()).save(any(AppUser.class));
    }

    @Test
    void profileUpdateEncodesNewPasswordAndLeavesBlankPasswordUnchanged() {
        AppUser person = new AppUser("Person", "person@example.com", "old-hash");
        when(users.findById(1L)).thenReturn(Optional.of(person));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwordEncoder);

        AppUser unchanged = service.updateProfile(1L,
                new UpdateProfileRequest("Person", "person@example.com", "  "));
        assertEquals("old-hash", unchanged.getPasswordHash());

        AppUser changed = service.updateProfile(1L,
                new UpdateProfileRequest("Person", "person@example.com", "newpassword"));
        assertTrue(passwordEncoder.matches("newpassword", changed.getPasswordHash()));
        assertNotEquals("newpassword", changed.getPasswordHash());
    }

    @Test
    void updateRejectsMissingRequestsUsersBadRolesAndDuplicateEmails() {
        UserService service = new UserService(users, passwordEncoder);
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.update(2L, 1L, null));
        assertStatus(HttpStatus.NOT_FOUND,
                () -> service.update(2L, 1L, new UpdateUserRequest("Person", "person@example.com", null, null, null)));

        when(users.findById(2L)).thenReturn(Optional.of(new AppUser("Person", "person@example.com", "encoded")));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.update(2L, 1L,
                new UpdateUserRequest("Person", "person@example.com", null, null, "OWNER")));
        when(users.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
        assertStatus(HttpStatus.CONFLICT, () -> service.update(2L, 1L,
                new UpdateUserRequest("Person", "taken@example.com", null, null, null)));
    }

    @Test
    void updatePreservesOmittedFieldsAndEncodesAnOptionalNewPassword() {
        AppUser person = new AppUser("Person", "person@example.com", "old-hash");
        when(users.findById(2L)).thenReturn(Optional.of(person));
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UserService service = new UserService(users, passwordEncoder);

        AppUser unchanged = service.update(2L, 1L,
                new UpdateUserRequest("Person", "person@example.com", null, null, null));
        assertEquals(AppUser.Role.USER, unchanged.getRole());
        assertTrue(unchanged.isEnabled());
        assertEquals("old-hash", unchanged.getPasswordHash());

        AppUser changed = service.update(2L, 1L,
                new UpdateUserRequest("Person", "person@example.com", "newpassword", false, "USER"));
        assertFalse(changed.isEnabled());
        assertTrue(passwordEncoder.matches("newpassword", changed.getPasswordHash()));
    }

    @Test
    void lastEnabledSuperAdminCannotBeDisabledButOtherAdminsCanBeChanged() {
        AppUser admin = new AppUser("Admin", "admin@example.com", "encoded");
        admin.setRole(AppUser.Role.SUPER_ADMIN);
        when(users.findById(2L)).thenReturn(Optional.of(admin));
        when(users.countByRoleAndEnabledTrue(AppUser.Role.SUPER_ADMIN)).thenReturn(1L);
        UserService service = new UserService(users, passwordEncoder);

        assertStatus(HttpStatus.CONFLICT, () -> service.update(2L, 1L,
                new UpdateUserRequest("Admin", "admin@example.com", null, false, null)));
        when(users.countByRoleAndEnabledTrue(AppUser.Role.SUPER_ADMIN)).thenReturn(2L);
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertFalse(service.update(2L, 1L,
                new UpdateUserRequest("Admin", "admin@example.com", null, false, null)).isEnabled());
    }

    @Test
    void deletingRegularUsersSucceedsAndMissingUsersReturnNotFound() {
        AppUser person = new AppUser("Person", "person@example.com", "encoded");
        when(users.findById(2L)).thenReturn(Optional.of(person));
        UserService service = new UserService(users, passwordEncoder);

        service.delete(2L);
        verify(users).delete(person);
        assertStatus(HttpStatus.NOT_FOUND, () -> service.delete(3L));
    }

    private void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }
}
