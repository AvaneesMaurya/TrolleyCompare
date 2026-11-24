package com.trolleycompare.backend.config;

import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityConfigTest {

    @Test
    void createsPasswordAndSessionContextBeansAndAuthenticationManager() throws Exception {
        SecurityConfig config = new SecurityConfig();
        assertInstanceOf(BCryptPasswordEncoder.class, config.passwordEncoder());
        assertInstanceOf(HttpSessionSecurityContextRepository.class, config.securityContextRepository());

        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        assertSame(authenticationManager, config.authenticationManager(authenticationConfiguration));
    }

    @Test
    void loadsUsersByEmailAndRejectsUnknownAccounts() {
        AppUserRepository users = mock(AppUserRepository.class);
        AppUser person = new AppUser("Person", "person@example.com", "hash");
        when(users.findByEmailIgnoreCase("person@example.com")).thenReturn(Optional.of(person));
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
        UserDetailsService service = new SecurityConfig().userDetailsService(users);

        assertInstanceOf(UserPrincipal.class, service.loadUserByUsername("person@example.com"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing@example.com"));
        verify(users).findByEmailIgnoreCase("person@example.com");
        verify(users).findByEmailIgnoreCase("missing@example.com");
    }
}
