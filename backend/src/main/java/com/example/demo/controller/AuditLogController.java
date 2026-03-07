package com.example.demo.controller;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping({"/audit", "/audit-logs"})
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(required = false) String decision) {

        int pageIndex = Math.max(0, page - 1);
        Pageable pageable = PageRequest.of(pageIndex, limit);
        Page<AuditLog> auditPage;

        if (decision != null && !decision.isEmpty()) {
            auditPage = auditLogRepository.findByDecisionOrderByTimestampDesc(decision.toUpperCase(), pageable);
        } else {
            auditPage = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }

        List<Map<String, Object>> data = auditPage.getContent().stream()
                .map(this::toFrontendFormat)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("total", auditPage.getTotalElements());
        result.put("page", page);
        result.put("limit", limit);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/audit-logs/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentLogs() {
        List<Map<String, Object>> logs = auditLogRepository.findTop10ByOrderByTimestampDesc()
                .stream()
                .map(this::toFrontendFormat)
                .collect(Collectors.toList());
        return ResponseEntity.ok(logs);
    }

    private Map<String, Object> toFrontendFormat(AuditLog log) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", log.getId());
        result.put("decision", log.getDecision());
        result.put("reason", log.getReason());
        result.put("processed_at", log.getTimestamp() != null ? log.getTimestamp() : java.time.OffsetDateTime.now());

        // Nested event object as "notification_events" (matches frontend expectation)
        Map<String, Object> evt = new LinkedHashMap<>();
        if (log.getEvent() != null) {
            result.put("event_id", log.getEvent().getId());
            evt.put("id", log.getEvent().getId());
            evt.put("user_id", log.getEvent().getUserId());
            evt.put("event_type", log.getEvent().getEventType());
            evt.put("title", log.getEvent().getTitle());
            evt.put("message", log.getEvent().getMessage());
            evt.put("source", log.getEvent().getSource());
            evt.put("channel", log.getEvent().getChannel());
            evt.put("priority_hint", log.getEvent().getPriorityHint());
            evt.put("metadata", log.getEvent().getMetadata());
            evt.put("created_at", log.getEvent().getCreatedAt());
        } else {
            // Orphaned audit log — event was deleted; provide fallback from audit fields
            evt.put("title", log.getReason());
            evt.put("source", log.getDecision());
            evt.put("event_type", "ARCHIVED");
        }
        result.put("notification_events", evt);

        // AI fields
        if (log.getAiAnalysis() != null) {
            result.put("ai_used", true);
            result.put("ai_model", log.getAiAnalysis().getModelUsed());
            result.put("ai_confidence", log.getAiAnalysis().getConfidence());
            result.put("is_fallback", log.getAiAnalysis().getFallbackUsed());
        } else {
            result.put("ai_used", false);
            result.put("is_fallback", false);
        }

        // Rule object
        if (log.getRuleTriggered() != null) {
            Map<String, Object> rule = new LinkedHashMap<>();
            rule.put("id", log.getRuleTriggered().getId());
            rule.put("name", log.getRuleTriggered().getName());
            result.put("rules", rule);
        }

        return result;
    }
}
