package com.trolleycompare.backend.config;

import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.security.ActiveUserSessionFilter;
import com.trolleycompare.backend.security.UserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AppUserRepository users) {
        return email -> users.findByEmailIgnoreCase(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository contextRepository,
            AppUserRepository users) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/graphql/**"))
                .securityContext(context -> context.securityContextRepository(contextRepository))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                response.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((request, response, exception) ->
                                response.sendError(HttpStatus.FORBIDDEN.value())))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/history/**").authenticated()
                        .requestMatchers("/graphql/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterAfter(new ActiveUserSessionFilter(users, contextRepository), SecurityContextHolderFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable());

        return http.build();
    }
}
