package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SecurityConfig wires up:
 *   - Stateless JWT-based session management
 *   - The SupabaseJwtFilter before Spring's default auth filter
 *   - CSRF disabled (REST API, no browser form posts)
 *   - Public routes that don't require a token
 *
 * To protect additional routes, add them in the requestMatchers section below
 * or use @PreAuthorize / @Secured on individual controller methods.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SupabaseJwtFilter supabaseJwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST APIs use tokens, not cookies
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — no HTTP sessions; every request must carry its token
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Route-level authorization
            .authorizeHttpRequests(auth -> auth
                // ── Public endpoints (no JWT required) ───────────────────────
                .requestMatchers(
                    "/health",
                    "/actuator/**"
                ).permitAll()

                // ── All API endpoints require a valid Supabase JWT ──────────
                .requestMatchers("/api/**").authenticated()

                // ── Everything else requires a valid Supabase JWT ─────────────
                .anyRequest().authenticated()
            )

            // Return 401 (not 403) for unauthenticated requests
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, authEx) ->
                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required"))
            )

            // Insert Supabase JWT validation before Spring's default filter
            .addFilterBefore(supabaseJwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
