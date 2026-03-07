package com.example.demo.service;

import com.example.demo.dto.DecisionResponse;
import com.example.demo.dto.EventRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PrioritizationServiceTest {

    @Autowired
    private PrioritizationService prioritizationService;

    @Test
    @Order(1)
    @DisplayName("Rule match: HIGH priority should give NOW")
    void testRuleMatchedNowDecision() {
        EventRequest request = EventRequest.builder()
                .userId(UUID.randomUUID().toString())
                .eventType("MONITOR")
                .message("CPU Spike detected at 99%")
                .source("AWS-CloudWatch")
                .priorityHint("HIGH")
                .build();

        DecisionResponse response = prioritizationService.process(request);
        assertNotNull(response);
        assertEquals("NOW", response.getClassification());
    }

    @Test
    @Order(2)
    @DisplayName("Exact deduplication: same dedupeKey returns NEVER")
    void testExactDedupeDecision() {
        String uniqueKey = "dedup-test-" + UUID.randomUUID();
        String userId = UUID.randomUUID().toString();

        EventRequest request = EventRequest.builder()
                .userId(userId)
                .eventType("AUTH.CHECK")
                .message("Password reset attempt")
                .source("AuthService")
                .priorityHint("MEDIUM")
                .dedupeKey(uniqueKey)
                .build();

        // First call – saved
        prioritizationService.process(request);

        // Second call with same dedupeKey – should be deduplicated
        DecisionResponse dup = prioritizationService.process(request);
        assertEquals("NEVER", dup.getClassification());
        assertTrue(dup.getReason().contains("Exact duplicate"));
    }

    @Test
    @Order(3)
    @DisplayName("Alert fatigue: 6th event for same user returns LATER")
    void testFatigueThrottling() {
        String userId = UUID.randomUUID().toString();

        // Send 5 events with MEDIUM priority (no rule fires — falls to AI fallback returning LATER)
        for (int i = 0; i < 5; i++) {
            EventRequest req = EventRequest.builder()
                    .userId(userId)
                    .eventType("REPORT.TRACE")
                    .message("Trace log event number " + i)
                    .source("Logger")
                    .priorityHint("MEDIUM")
                    .build();
            prioritizationService.process(req);
        }

        // 6th event — should hit fatigue limit
        EventRequest req6 = EventRequest.builder()
                .userId(userId)
                .eventType("REPORT.TRACE")
                .message("Trace log event number 5")
                .source("Logger")
                .priorityHint("MEDIUM")
                .build();

        DecisionResponse response = prioritizationService.process(req6);
        assertEquals("LATER", response.getClassification());
        assertTrue(response.getReason().contains("fatigue threshold exceeded"));
    }
}
