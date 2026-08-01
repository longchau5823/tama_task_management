package com.hutech.tama.config;

import com.hutech.tama.exception.ApiErrorResponse;
import com.hutech.tama.security.ActiveSessionValidationFilter;
import com.hutech.tama.security.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final ActiveSessionValidationFilter activeSessionValidationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig( CustomUserDetailsService userDetailsService, ActiveSessionValidationFilter activeSessionValidationFilter, ObjectMapper objectMapper ) {
        this.userDetailsService = userDetailsService;
        this.activeSessionValidationFilter = activeSessionValidationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.userDetailsService(userDetailsService)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/login", "/register", "/css/**", "/js/**",
                                "/images/**", "/webjars/**", "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/dashboard", "/boards/**", "/labels", "/profile", "/api/**"
                        ).authenticated().anyRequest().denyAll()
                ).formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll()
                ).logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                ).exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            if (isApiRequest(request)) {
                                writeApiError(request, response, HttpStatus.UNAUTHORIZED, "Authentication is required");
                            } else {
                                response.sendRedirect(request.getContextPath() + "/login");
                            }
                        }).accessDeniedHandler((request, response, exception) -> {
                            if (isApiRequest(request)) {
                                writeApiError(request, response, HttpStatus.FORBIDDEN, "Access is denied");
                            } else {
                                response.sendError(HttpStatus.FORBIDDEN.value());
                            }
                        })
                ).addFilterBefore(activeSessionValidationFilter, AuthorizationFilter.class);

        return http.build();
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    private void writeApiError( HttpServletRequest request, HttpServletResponse response, HttpStatus status, String message ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of()
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
