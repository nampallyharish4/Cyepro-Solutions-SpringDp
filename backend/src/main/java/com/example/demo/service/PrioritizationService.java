package com.example.demo.service;

import com.example.demo.ai.AIAnalysisRepository;
import com.example.demo.ai.AIService;
import com.example.demo.deduplication.DeduplicationEngine;
import com.example.demo.dto.DecisionResponse;
import com.example.demo.dto.EventRequest;
import com.example.demo.entity.*;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.LaterQueueRepository;
import com.example.demo.rules.RuleEngine;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class PrioritizationService {
    private static final Logger log = LoggerFactory.getLogger(PrioritizationService.class);
    private final EventRepository eventRepository;
    private final AuditLogRepository auditLogRepository;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final LaterQueueRepository laterQueueRepository;
    private final DeduplicationEngine deduplicationEngine;
    private final RuleEngine ruleEngine;
    private final AIService aiService;

    public PrioritizationService(EventRepository eventRepository, AuditLogRepository auditLogRepository, AIAnalysisRepository aiAnalysisRepository, LaterQueueRepository laterQueueRepository, DeduplicationEngine deduplicationEngine, RuleEngine ruleEngine, AIService aiService) {
        this.eventRepository = eventRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.laterQueueRepository = laterQueueRepository;
        this.deduplicationEngine = deduplicationEngine;
        this.ruleEngine = ruleEngine;
        this.aiService = aiService;
    }

    // Alert Fatigue Check (Cache: UserID -> Count)
    private final Cache<String, Integer> fatigueCache = Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Transactional
    public DecisionResponse process(EventRequest request) {
        log.info("Processing event for user: {}", request.getUserId());

        // --- 1a. Pre-save deduplication check (by dedupeKey) ---
        if (request.getDedupeKey() != null) {
            Optional<Event> existingByKey = eventRepository.findFirstByDedupeKey(request.getDedupeKey());
            if (existingByKey.isPresent()) {
                // Return immediately without persisting the duplicate
                return DecisionResponse.builder()
                        .eventId(existingByKey.get().getId())
                        .classification("NEVER")
                        .reason("Exact duplicate notification by dedupe_key")
                        .confidence(1.0)
                        .build();
            }
        }

        // 1b. Ingestion – persist after key-check
        Event event = Event.builder()
                .userId(request.getUserId())
                .eventType(request.getEventType())
                .title(request.getTitle())
                .message(request.getMessage() != null ? request.getMessage() : (request.getTitle() != null ? request.getTitle() : ""))
                .source(request.getSource())
                .priorityHint(request.getPriorityHint() != null ? request.getPriorityHint() : "medium")
                .channel(request.getChannel())
                .metadata(request.getMetadata())
                .dedupeKey(request.getDedupeKey())
                .expiresAt(request.getExpiresAt())
                .build();
        event = eventRepository.save(event);

        // 2. Deduplication (similarity check)
        Optional<String> duplicateReason = deduplicationEngine.checkDuplicate(event);
        if (duplicateReason.isPresent()) {
            return finalizeDecision(event, "NEVER", duplicateReason.get(), null, null);
        }

        // 3. Alert Fatigue Check
        Integer count = fatigueCache.getIfPresent(event.getUserId());
        if (count == null) count = 0;
        if (count >= 5) {
            return finalizeDecision(event, "LATER", "Alert fatigue threshold exceeded (max 5 per 10m)", null, null);
        }
        fatigueCache.put(event.getUserId(), count + 1);

        // 4. Rule Engine
        Optional<RuleEngine.RuleEvaluationResult> ruleResult = ruleEngine.evaluate(event);
        if (ruleResult.isPresent()) {
            return finalizeDecision(event, ruleResult.get().action(), ruleResult.get().reason(), ruleResult.get().rule(), null);
        }

        // 5. AI Classification
        AIAnalysis aiAnalysis = aiService.classify(event);
        aiAnalysis.setEvent(event);
        aiAnalysis = aiAnalysisRepository.save(aiAnalysis);

        return finalizeDecision(event, aiAnalysis.getClassification(), aiAnalysis.getReason(), null, aiAnalysis);
    }

    private DecisionResponse finalizeDecision(Event event, String decision, String reason, Rule rule, AIAnalysis aiAnalysis) {
        AuditLog logEntry = AuditLog.builder()
                .event(event)
                .decision(decision)
                .ruleTriggered(rule)
                .reason(reason)
                .aiAnalysis(aiAnalysis)
                .build();
        auditLogRepository.save(logEntry);

        if ("LATER".equals(decision)) {
            LaterQueue lq = LaterQueue.builder()
                    .event(event)
                    .nextRunAt(OffsetDateTime.now().plusMinutes(5))
                    .build();
            laterQueueRepository.save(lq);
        }

        return DecisionResponse.builder()
                .eventId(event.getId())
                .classification(decision)
                .reason(reason)
                .confidence(aiAnalysis != null ? aiAnalysis.getConfidence() : 1.0)
                .build();
    }
}
