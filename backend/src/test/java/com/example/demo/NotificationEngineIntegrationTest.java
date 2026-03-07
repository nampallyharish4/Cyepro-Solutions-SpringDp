package com.example.demo;

import com.example.demo.dto.DecisionResponse;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationEngineIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/api";
    }

    private HttpEntity<Map<String, Object>> buildRequest(String eventType, String message, String priorityHint, String source, String dedupeKey) {
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", "test-user-integration");
        body.put("event_type", eventType);
        body.put("message", message);
        body.put("source", source);
        body.put("priority_hint", priorityHint);
        if (dedupeKey != null) body.put("dedupe_key", dedupeKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    // ─────────────────────────────────────────────────
    // TEST 1: Health Check
    // ─────────────────────────────────────────────────
    @SuppressWarnings("rawtypes")
    @Test
    @Order(1)
    @DisplayName("1. Health endpoint should return OK status")
    void testHealthEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl() + "/health", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("OK", response.getBody().get("status"));
    }

    // ─────────────────────────────────────────────────
    // TEST 2: Rule Engine – HIGH priority → NOW
    // ─────────────────────────────────────────────────
    @Test
    @Order(2)
    @DisplayName("2. HIGH priority event should return NOW (Rule Engine)")
    void testHighPriorityNowDecision() {
        ResponseEntity<DecisionResponse> response = restTemplate.postForEntity(
                baseUrl() + "/events",
                buildRequest("SYSTEM.ALERT", "Critical memory exhaustion on web-01", "HIGH", "Monitoring", null),
                DecisionResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DecisionResponse body2 = response.getBody();
        assertNotNull(body2);
        assertEquals("NOW", body2.getClassification());
    }

    // ─────────────────────────────────────────────────
    // TEST 3: Rule Engine – SECURITY event → NOW
    // ─────────────────────────────────────────────────
    @Test
    @Order(3)
    @DisplayName("3. SECURITY event type should match rule and return NOW")
    void testSecurityEventNowDecision() {
        ResponseEntity<DecisionResponse> response = restTemplate.postForEntity(
                baseUrl() + "/events",
                buildRequest("SECURITY.BREACH", "Unauthorized access detected on port 22", "MEDIUM", "Firewall", null),
                DecisionResponse.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DecisionResponse body3 = response.getBody();
        assertNotNull(body3);
        assertEquals("NOW", body3.getClassification());
    }

    // ─────────────────────────────────────────────────
    // TEST 4: Rule Engine – LOW Priority → NEVER
    // ─────────────────────────────────────────────────
    @Test
    @Order(4)
    @DisplayName("4. LOW priority should get NEVER decision")
    void testLowPriorityNeverDecision() {
        // Create a fresh userId so fatigue doesn't trigger LATER
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", UUID.randomUUID().toString());
        body.put("event_type", "INFO");
        body.put("message", "Weekly report generation has completed");
        body.put("source", "ReportService");
        body.put("priority_hint", "LOW");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<DecisionResponse> response = restTemplate.postForEntity(
                baseUrl() + "/events", new HttpEntity<>(body, headers), DecisionResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DecisionResponse body4 = response.getBody();
        assertNotNull(body4);
        assertEquals("NEVER", body4.getClassification());
    }

    // ─────────────────────────────────────────────────
    // TEST 5: Exact Deduplication (same dedupeKey)
    // ─────────────────────────────────────────────────
    @Test
    @Order(5)
    @DisplayName("5. Exact duplicate (same dedupeKey) should return NEVER")
    void testExactDeduplication() {
        String dedupeKey = "login-fail-unique-" + UUID.randomUUID();
        String uniqueUser = UUID.randomUUID().toString();

        Map<String, Object> body = new HashMap<>();
        body.put("user_id", uniqueUser);
        body.put("event_type", "AUTH");
        body.put("message", "Login from unknown device");
        body.put("source", "AuthService");
        body.put("priority_hint", "MEDIUM");
        body.put("dedupe_key", dedupeKey);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // First event – processed
        restTemplate.postForEntity(baseUrl() + "/events", new HttpEntity<>(body, headers), DecisionResponse.class);

        // Second event – same key → NEVER
        ResponseEntity<DecisionResponse> dup = restTemplate.postForEntity(
                baseUrl() + "/events", new HttpEntity<>(body, headers), DecisionResponse.class);

        assertEquals(HttpStatus.OK, dup.getStatusCode());
        DecisionResponse dupBody = dup.getBody();
        assertNotNull(dupBody);
        assertEquals("NEVER", dupBody.getClassification());
        assertNotNull(dupBody.getReason());
        assertTrue(dupBody.getReason().contains("Exact duplicate"));
    }

    // ─────────────────────────────────────────────────
    // TEST 6: Alert Fatigue – 6th event → LATER
    // ─────────────────────────────────────────────────
    @Test
    @Order(6)
    @DisplayName("6. Alert fatigue: 6th event for same user should be LATER")
    void testAlertFatigueDecision() {
        // Use a MEDIUM priority to avoid rules matching (HIGH→NOW, LOW→NEVER, SECURITY→NOW)
        String fatigueUser = UUID.randomUUID().toString();

        // Event type "SYSTEM.REPORT" doesn't match any rule, so it falls to AI fallback
        // With MEDIUM priority, AI fallback → LATER
        for (int i = 0; i < 5; i++) {
            Map<String, Object> body = Map.of(
                    "user_id", fatigueUser,
                    "event_type", "REPORT.WEEKLY",
                    "message", "Report item " + i,
                    "source", "Syslog",
                    "priority_hint", "MEDIUM"
            );
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(baseUrl() + "/events", new HttpEntity<>(body, headers), DecisionResponse.class);
        }

        // 6th – should hit fatigue
        Map<String, Object> body6 = Map.of(
                "user_id", fatigueUser,
                "event_type", "REPORT.WEEKLY",
                "message", "Report item 6",
                "source", "Syslog",
                "priority_hint", "MEDIUM"
        );
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<DecisionResponse> r = restTemplate.postForEntity(
                baseUrl() + "/events", new HttpEntity<>(body6, h), DecisionResponse.class);

        DecisionResponse rBody = r.getBody();
        assertNotNull(rBody);
        assertEquals("LATER", rBody.getClassification());
        assertTrue(rBody.getReason().contains("fatigue threshold exceeded"));
    }

    // ─────────────────────────────────────────────────
    // TEST 7: Stats endpoint — non-null fields
    // ─────────────────────────────────────────────────
    @SuppressWarnings("rawtypes")
    @Test
    @Order(7)
    @DisplayName("7. Metrics endpoint should contain all count fields")
    void testStatsEndpoint() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl() + "/metrics", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map body7 = response.getBody();
        assertNotNull(body7);
        assertNotNull(body7.get("total"));
        assertNotNull(body7.get("now"));
        assertNotNull(body7.get("later"));
        assertNotNull(body7.get("never"));

        Number total = (Number) body7.get("total");
        Number now = (Number) body7.get("now");
        Number later = (Number) body7.get("later");
        Number never = (Number) body7.get("never");
        long decisionTotal = now.longValue() + later.longValue() + never.longValue();

        assertTrue(total.longValue() >= decisionTotal);
    }

    // ─────────────────────────────────────────────────
    // TEST 8: Missing required fields → 400 Bad Request
    // ─────────────────────────────────────────────────
    @Test
    @Order(8)
    @DisplayName("8. Missing required fields should return 400 Bad Request")
    void testMissingFieldsReturns400() {
        Map<String, Object> incomplete = new HashMap<>();
        incomplete.put("message", "just a message without other fields");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/events", new HttpEntity<>(incomplete, headers), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
