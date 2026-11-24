package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.CreateUserRequest;
import com.trolleycompare.backend.dto.UpdateUserRequest;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.security.UserPrincipal;
import com.trolleycompare.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserControllerTest {

    private UserService users;
    private AdminUserController controller;

    @BeforeEach
    void setUp() {
        users = mock(UserService.class);
        controller = new AdminUserController(users);
    }

    @Test
    void listsUsersAsResponses() {
        AppUser user = new AppUser("Person", "person@example.com", "hash");
        when(users.findAll()).thenReturn(List.of(user));

        var result = controller.listUsers();

        assertEquals(1, result.size());
        assertEquals("person@example.com", result.get(0).email());
        assertEquals("USER", result.get(0).role());
    }

    @Test
    void createsUsersAndReturnsTheirResponse() {
        CreateUserRequest request = new CreateUserRequest("Person", "person@example.com", "password123");
        when(users.register(request)).thenReturn(new AppUser("Person", "person@example.com", "hash"));

        var result = controller.createUser(request);

        assertEquals("Person", result.name());
        verify(users).register(request);
    }

    @Test
    void updatesAndDeletesUsersUsingTheAuthenticatedRequester() {
        AppUser updated = new AppUser("Updated", "updated@example.com", "hash");
        UpdateUserRequest request = new UpdateUserRequest("Updated", "updated@example.com", null, true, "USER");
        UserPrincipal requester = new UserPrincipal(9L, "Admin", "admin@example.com", "hash", AppUser.Role.SUPER_ADMIN, true);
        when(users.update(2L, 9L, request)).thenReturn(updated);

        assertEquals("updated@example.com", controller.updateUser(2L, request, requester).email());
        controller.deleteUser(2L);

        verify(users).update(2L, 9L, request);
        verify(users).delete(2L);
    }
}
