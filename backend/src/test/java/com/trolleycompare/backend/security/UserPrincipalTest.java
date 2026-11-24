package com.trolleycompare.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.trolleycompare.backend.model.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    void exposesAccountIdentityRoleAndResponseWithoutPassword() throws Exception {
        AppUser user = new AppUser("Person", "person@example.com", "password-hash");
        user.setRole(AppUser.Role.SUPER_ADMIN);
        ReflectionTestUtils.setField(user, "id", 12L);

        UserPrincipal principal = new UserPrincipal(user);

        assertEquals(12L, principal.id());
        assertEquals("person@example.com", principal.getUsername());
        assertEquals("password-hash", principal.getPassword());
        assertTrue(principal.isEnabled());
        assertEquals("ROLE_SUPER_ADMIN", principal.getAuthorities().iterator().next().getAuthority());
        assertEquals("SUPER_ADMIN", principal.toResponse().role());
        assertEquals("person@example.com", principal.toResponse().email());
        assertFalse(new ObjectMapper().writeValueAsString(principal.toResponse()).contains("password"));
    }

    @Test
    void preservesDisabledStateForAuthentication() {
        AppUser user = new AppUser("Person", "person@example.com", "hash");
        user.setEnabled(false);

        assertFalse(new UserPrincipal(user).isEnabled());
    }
}
