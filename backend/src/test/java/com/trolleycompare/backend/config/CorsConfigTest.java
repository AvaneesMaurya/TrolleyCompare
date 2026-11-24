package com.trolleycompare.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CorsConfigTest {

    @Test
    void registersCredentialedApiAndGraphqlCorsRules() {
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = mock(CorsRegistration.class);
        when(registry.addMapping(anyString())).thenReturn(registration);
        when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);
        when(registration.allowedMethods(any(String[].class))).thenReturn(registration);
        when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);
        when(registration.allowCredentials(anyBoolean())).thenReturn(registration);
        WebMvcConfigurer configurer = new CorsConfig().corsConfigurer(
                new String[] {"https://shop.example", "https://admin.example"});

        configurer.addCorsMappings(registry);

        verify(registry).addMapping("/api/**");
        verify(registry).addMapping("/graphql/**");
        verify(registration, times(2)).allowedOrigins("https://shop.example", "https://admin.example");
        verify(registration, times(1)).allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
        verify(registration, times(1)).allowedMethods("GET", "POST", "OPTIONS");
        verify(registration, times(2)).allowedHeaders("*");
        verify(registration, times(2)).allowCredentials(true);
    }
}
