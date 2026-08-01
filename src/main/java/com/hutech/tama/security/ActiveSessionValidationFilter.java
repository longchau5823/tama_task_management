package com.hutech.tama.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ActiveSessionValidationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;

    public ActiveSessionValidationFilter(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (requiresReauthentication(authentication)) {
            invalidateCurrentSession(request);
        }
        filterChain.doFilter(request, response);
    }

    private boolean requiresReauthentication(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return false;
        }

        try {
            UserDetails latest = userDetailsService.loadUserByUsername(authentication.getName());
            if (latest == null
                    || !latest.isEnabled()
                    || !latest.isAccountNonExpired()
                    || !latest.isAccountNonLocked()
                    || !latest.isCredentialsNonExpired()) {
                return true;
            }

            Set<String> currentAuthorities = roleAuthorities(authentication.getAuthorities());
            Set<String> latestAuthorities = roleAuthorities(latest.getAuthorities());
            return !currentAuthorities.equals(latestAuthorities);
        } catch (UsernameNotFoundException exception) {
            return true;
        }
    }

    private Set<String> roleAuthorities(
            Collection<? extends GrantedAuthority> authorities
    ) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    private void invalidateCurrentSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
