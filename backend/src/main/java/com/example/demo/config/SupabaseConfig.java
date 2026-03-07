package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

/**
 * SupabaseConfig wires up the Supabase project URL and keys
 * so other beans can inject them without repeating @Value everywhere.
 *
 * Properties sourced from application.properties:
 *   supabase.url              – your project URL, e.g. https://xxxx.supabase.co
 *   supabase.service-role-key – server-side secret key (never expose to frontend)
 *   supabase.jwt-secret       – base64-encoded JWT secret used to verify Supabase tokens
 */
@Configuration
public class SupabaseConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    @Value("${supabase.jwt-secret}")
    private String jwtSecret;

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getServiceRoleKey() {
        return serviceRoleKey;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    // ── RestTemplate bean pre-configured for Supabase REST calls ─────────────

    /**
     * A RestTemplate ready to call Supabase REST / Auth APIs.
     * To add the service-role key per-request use an interceptor,
     * or build the header manually in your service class like:
     *
     * <pre>
     *   HttpHeaders headers = new HttpHeaders();
     *   headers.set("apikey", supabaseConfig.getServiceRoleKey());
     *   headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + supabaseConfig.getServiceRoleKey());
     * </pre>
     */
    @Bean(name = "supabaseRestTemplate")
    public RestTemplate supabaseRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("apikey", serviceRoleKey);
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey);
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
