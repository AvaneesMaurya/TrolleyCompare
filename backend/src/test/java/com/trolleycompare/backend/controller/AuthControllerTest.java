package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.CreateUserRequest;
import com.trolleycompare.backend.dto.UpdateProfileRequest;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.security.UserPrincipal;
import com.trolleycompare.backend.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private UserService users;
    private AuthenticationManager authenticationManager;
    private SecurityContextRepository contexts;
    private AuthController controller;
    private UserPrincipal principal;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        users = mock(UserService.class);
        authenticationManager = mock(AuthenticationManager.class);
        contexts = mock(SecurityContextRepository.class);
        controller = new AuthController(users, authenticationManager, contexts);
        principal = new UserPrincipal(4L, "Person", "person@example.com", "hash", AppUser.Role.USER, true);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsTheCsrfTokenAndRegistersAnAccount() {
        CsrfToken csrfToken = mock(CsrfToken.class);
        when(csrfToken.getToken()).thenReturn("csrf-value");
        AppUser created = new AppUser("Person", "person@example.com", "hash");
        when(users.register(any(CreateUserRequest.class))).thenReturn(created);

        assertEquals(Map.of("token", "csrf-value"), controller.csrf(csrfToken));
        var result = controller.register(new CreateUserRequest("Person", "person@example.com", "password123"));

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals("person@example.com", result.getBody().email());
    }

    @Test
    void loginRejectsMissingCredentialsAndAuthenticationFailures() {
        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> controller.login(null, new MockHttpServletRequest(), new MockHttpServletResponse()));
        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> controller.login(new AuthController.LoginRequest(" ", "password"),
                        new MockHttpServletRequest(), new MockHttpServletResponse()));
        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> controller.login(new AuthController.LoginRequest("person@example.com", " "),
                        new MockHttpServletRequest(), new MockHttpServletResponse()));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));
        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> controller.login(new AuthController.LoginRequest("person@example.com", "wrong"),
                        new MockHttpServletRequest(), new MockHttpServletResponse()));
    }

    @Test
    void authenticatesAndPersistsTheSecurityContext() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession existingSession = (MockHttpSession) request.getSession(true);
        String oldSessionId = existingSession.getId();

        var result = controller.login(
                new AuthController.LoginRequest("person@example.com", "password"),
                request,
                new MockHttpServletResponse());

        assertEquals("person@example.com", result.email());
        assertNotEquals(oldSessionId, request.getSession(false).getId());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
        verify(contexts).saveContext(any(), eq(request), any(HttpServletResponse.class));
    }

    @Test
    void reportsTheCurrentUserAndRequiresAuthenticationForProfileChanges() {
        assertStatus(HttpStatus.UNAUTHORIZED, () -> controller.currentUser(null));
        assertStatus(HttpStatus.UNAUTHORIZED,
                () -> controller.updateProfile(new UpdateProfileRequest("Person", "person@example.com", null), null));

        assertEquals("person@example.com", controller.currentUser(principal).email());
        AppUser updated = new AppUser("Updated", "updated@example.com", "hash");
        when(users.updateProfile(4L, new UpdateProfileRequest("Updated", "updated@example.com", null)))
                .thenReturn(updated);
        assertEquals("updated@example.com",
                controller.updateProfile(new UpdateProfileRequest("Updated", "updated@example.com", null), principal)
                        .email());
    }

    @Test
    void logoutClearsTheContextAndInvalidatesAnExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        var response = controller.logout(request, new MockHttpServletResponse());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(contexts).saveContext(any(), eq(request), any(HttpServletResponse.class));
    }

    private void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }
}
