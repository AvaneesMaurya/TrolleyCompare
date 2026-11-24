package com.trolleycompare.backend.security;

import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActiveUserSessionFilterTest {

    private AppUserRepository users;
    private SecurityContextRepository contexts;
    private ActiveUserSessionFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        users = mock(AppUserRepository.class);
        contexts = mock(SecurityContextRepository.class);
        filter = new ActiveUserSessionFilter(users, contexts);
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void passesUnauthenticatedRequestsThroughWithoutRepositoryLookup() throws Exception {
        runFilter(new MockHttpServletRequest());

        verifyNoInteractions(users, contexts);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void invalidatesSessionsForRemovedOrDisabledUsers() throws Exception {
        UserPrincipal principal = principal(7L, "Person", true);
        authenticate(principal);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        when(users.findById(7L)).thenReturn(Optional.empty());

        runFilter(request);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(contexts, never()).saveContext(any(), any(), any());

        AppUser disabled = user(8L, "Person");
        disabled.setEnabled(false);
        authenticate(principal(8L, "Person", true));
        MockHttpServletRequest disabledRequest = new MockHttpServletRequest();
        MockHttpSession disabledSession = (MockHttpSession) disabledRequest.getSession(true);
        when(users.findById(8L)).thenReturn(Optional.of(disabled));

        runFilter(disabledRequest);

        assertTrue(disabledSession.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void refreshesChangedUserDetailsInTheStoredSecurityContext() throws Exception {
        UserPrincipal oldPrincipal = principal(9L, "Old Name", true);
        AppUser currentUser = user(9L, "New Name");
        when(users.findById(9L)).thenReturn(Optional.of(currentUser));
        authenticate(oldPrincipal);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chain);

        UserPrincipal refreshed = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals("New Name", refreshed.name());
        verify(contexts).saveContext(any(), eq(request), eq(response));
        verify(chain).doFilter(request, response);
    }

    @Test
    void leavesAnUnchangedEnabledPrincipalAlone() throws Exception {
        AppUser currentUser = user(10L, "Person");
        UserPrincipal principal = new UserPrincipal(currentUser);
        when(users.findById(10L)).thenReturn(Optional.of(currentUser));
        authenticate(principal);

        runFilter(new MockHttpServletRequest());

        verify(contexts, never()).saveContext(any(), any(), any());
        assertEquals(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    private void runFilter(MockHttpServletRequest request) throws Exception {
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
    }

    private UserPrincipal principal(Long id, String name, boolean enabled) {
        AppUser user = user(id, name);
        user.setEnabled(enabled);
        return new UserPrincipal(user);
    }

    private AppUser user(Long id, String name) {
        AppUser user = new AppUser(name, "person@example.com", "hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
