package com.example.demo.controller;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public HealthController(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("aiService");
        String cbState = cb.getState().name();
        long failureCount = cb.getMetrics().getNumberOfFailedCalls();

        Map<String, Object> aiService = new LinkedHashMap<>();
        aiService.put("status", "CLOSED".equals(cbState) ? "HEALTHY" : "DEGRADED");
        aiService.put("circuit_breaker", cbState);
        aiService.put("failure_count", failureCount);
        aiService.put("failure_threshold", 5);

        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "OK");
        health.put("engine", "RUNNING");
        health.put("database", "CONNECTED");
        health.put("ai_service", aiService);
        health.put("timestamp", java.time.OffsetDateTime.now().toString());
        return ResponseEntity.ok(health);
    }
}
