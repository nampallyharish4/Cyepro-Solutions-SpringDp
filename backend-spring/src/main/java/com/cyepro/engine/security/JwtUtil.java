package com.cyepro.engine.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.AlgorithmParameters;
import java.security.spec.ECGenParameterSpec;
import java.util.*;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    private final Map<String, PublicKey> supabaseKeys = new HashMap<>();

    @PostConstruct
    public void init() {
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            loadSupabaseJwks();
        }
    }

    private void loadSupabaseJwks() {
        try {
            String jwksUrl = supabaseUrl + "/auth/v1/.well-known/jwks.json";
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jwks = mapper.readTree(response.body());
            JsonNode keys = jwks.get("keys");

            KeyFactory kf = KeyFactory.getInstance("EC");
            AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
            params.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);

            for (JsonNode key : keys) {
                String kid = key.get("kid").asText();
                byte[] xBytes = Base64.getUrlDecoder().decode(key.get("x").asText());
                byte[] yBytes = Base64.getUrlDecoder().decode(key.get("y").asText());
                ECPoint point = new ECPoint(new BigInteger(1, xBytes), new BigInteger(1, yBytes));
                ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
                supabaseKeys.put(kid, kf.generatePublic(pubSpec));
            }
            log.info("Loaded {} Supabase JWKS keys for ES256 verification", supabaseKeys.size());
        } catch (Exception e) {
            log.warn("Failed to load Supabase JWKS: {}", e.getMessage());
        }
    }

    private SecretKey getSigningKey() {
        String padded = secret;
        while (padded.getBytes(StandardCharsets.UTF_8).length < 32) {
            padded = padded + secret;
        }
        return Keys.hmacShaKeyFor(padded.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UUID id, String email, String role) {
        return Jwts.builder()
                .subject(id.toString())
                .claim("email", email)
                .claim("role", role)
                .claim("id", id.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        // Try HS256 first (backend-generated tokens)
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception ignored) {
            // Fall through to try ES256
        }

        // Try ES256 with Supabase JWKS public keys
        for (PublicKey pubKey : supabaseKeys.values()) {
            try {
                return Jwts.parser()
                        .verifyWith(pubKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
            } catch (Exception ignored) {
                // Try next key
            }
        }

        throw new SecurityException("JWT signature verification failed with all available keys");
    }

    public boolean isValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
