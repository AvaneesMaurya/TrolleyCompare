package com.trolleycompare.backend.security;

import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.repository.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

public class ActiveUserSessionFilter extends OncePerRequestFilter {
    private final AppUserRepository users;
    private final SecurityContextRepository contextRepository;

    public ActiveUserSessionFilter(AppUserRepository users, SecurityContextRepository contextRepository) {
        this.users = users;
        this.contextRepository = contextRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            Optional<AppUser> currentUser = users.findById(principal.id());
            if (currentUser.isEmpty() || !currentUser.get().isEnabled()) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) {
                    request.getSession(false).invalidate();
                }
            } else if (!principal.equals(new UserPrincipal(currentUser.get()))) {
                UserPrincipal refreshedPrincipal = new UserPrincipal(currentUser.get());
                UsernamePasswordAuthenticationToken refreshedAuthentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                refreshedPrincipal, null, refreshedPrincipal.getAuthorities());
                refreshedAuthentication.setDetails(authentication.getDetails());
                SecurityContext refreshedContext = SecurityContextHolder.createEmptyContext();
                refreshedContext.setAuthentication(refreshedAuthentication);
                SecurityContextHolder.setContext(refreshedContext);
                contextRepository.saveContext(refreshedContext, request, response);
            }
        }
        filterChain.doFilter(request, response);
    }
}
