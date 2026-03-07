package com.example.demo.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * SupabaseJwtFilter intercepts every request and validates the Supabase-issued
 * JWT from the Authorization header.
 *
 * Flow:
 *   1. Extract Bearer token from "Authorization" header.
 *   2. Verify + parse with the Supabase JWT secret.
 *   3. Set a Spring Security Authentication so the request is seen as authenticated.
 *
 * If no token is present the filter simply continues the chain — the
 * SecurityConfig decides which endpoints require authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private final SupabaseConfig supabaseConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token — continue unauthenticated (SecurityConfig will block if needed)
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = parseToken(token);

            String userId  = claims.getSubject();                         // Supabase user UUID
            String role    = claims.get("role", String.class);            // "authenticated" etc.
            String email   = claims.get("email", String.class);

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role.toUpperCase() : "USER"))
            );

            // Attach user info to the principal so controllers can access it
            SupabasePrincipal principal = new SupabasePrincipal(userId, email, role);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Supabase JWT verified for user: {}", userId);

        } catch (JwtException ex) {
            log.warn("Invalid Supabase JWT: {}", ex.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Parses and validates the JWT using the Supabase JWT secret.
     * Supabase stores the secret as a base64-encoded string.
     */
    private Claims parseToken(String token) {
        String rawSecret = supabaseConfig.getJwtSecret();
        byte[] secretBytes;

        try {
            // Try base64 decode first (Supabase dashboard shows base64-encoded secret)
            secretBytes = Base64.getDecoder().decode(rawSecret);
        } catch (IllegalArgumentException e) {
            // Fallback: treat as plain UTF-8 string
            secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        }

        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretBytes))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Inner record: principal stored in the SecurityContext ─────────────────

    /**
     * Lightweight principal that carries Supabase user information.
     * Inject it into a controller with:
     *
     * <pre>
     *   @GetMapping("/me")
     *   public ResponseEntity<?> me(@AuthenticationPrincipal SupabaseJwtFilter.SupabasePrincipal user) {
     *       return ResponseEntity.ok(user);
     *   }
     * </pre>
     */
    public record SupabasePrincipal(String userId, String email, String role) {}
}
