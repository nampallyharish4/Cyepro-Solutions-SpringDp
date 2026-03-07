package com.example.demo.service;

import com.example.demo.dto.StatsResponse;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.LaterQueue;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.LaterQueueRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MetricsService {
    private final AuditLogRepository auditLogRepository;
    private final EventRepository eventRepository;
    private final LaterQueueRepository laterQueueRepository;

    public MetricsService(AuditLogRepository auditLogRepository, EventRepository eventRepository,
                          LaterQueueRepository laterQueueRepository) {
        this.auditLogRepository = auditLogRepository;
        this.eventRepository = eventRepository;
        this.laterQueueRepository = laterQueueRepository;
    }

    public StatsResponse getDashboardStats() {
        long now = auditLogRepository.countByDecision("NOW");
        long later = auditLogRepository.countByDecision("LATER");
        long never = auditLogRepository.countByDecision("NEVER");

        return StatsResponse.builder()
            .totalEvents(resolveTotalEvents(now, later, never))
            .nowTotal(now)
            .laterTotal(later)
            .neverTotal(never)
                .build();
    }

    public Map<String, Object> getEnhancedStats() {
        long now = auditLogRepository.countByDecision("NOW");
        long later = auditLogRepository.countByDecision("LATER");
        long never = auditLogRepository.countByDecision("NEVER");
        long total = resolveTotalEvents(now, later, never);

        // Queue stats
        List<LaterQueue> allQueue = laterQueueRepository.findAll();
        long waiting = allQueue.stream().filter(q -> "PENDING".equals(q.getStatus()) || "WAITING".equals(q.getStatus())).count();
        long failed = allQueue.stream().filter(q -> "FAILED".equals(q.getStatus())).count();
        long deadLetter = allQueue.stream().filter(q -> "DEAD_LETTER".equals(q.getStatus())).count();
        long sent = allQueue.stream().filter(q -> "COMPLETED".equals(q.getStatus()) || "SENT".equals(q.getStatus())).count();

        // Recent audit logs
        List<AuditLog> recentLogs = auditLogRepository.findTop10ByOrderByTimestampDesc();
        List<Map<String, Object>> recent = recentLogs.stream().map(log -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", log.getId());
            entry.put("decision", log.getDecision());
            entry.put("reason", log.getReason());
            entry.put("processed_at", log.getTimestamp());
            entry.put("timestamp", log.getTimestamp());
            if (log.getEvent() != null) {
                entry.put("event_type", log.getEvent().getEventType());
                entry.put("source", log.getEvent().getSource());
                entry.put("title", log.getEvent().getTitle());
            }
            return entry;
        }).collect(Collectors.toList());

        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("waiting", waiting);
        queue.put("failed", failed);
        queue.put("dead_letter", deadLetter);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("now", now);
        result.put("later", later);
        result.put("never", never);
        result.put("sent", sent);
        result.put("queue", queue);
        result.put("recent", recent);
        return result;
    }

    private long resolveTotalEvents(long now, long later, long never) {
        long storedEvents = eventRepository.count();
        long classifiedEvents = now + later + never;
        return Math.max(storedEvents, classifiedEvents);
    }

    public List<Map<String, Object>> getHourlyTimeline() {
        // Generate 24-hour timeline data from audit logs
        OffsetDateTime now = OffsetDateTime.now();
        List<AuditLog> allLogs = auditLogRepository.findAll();
        DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("HH:00");

        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 23; i >= 0; i--) {
            OffsetDateTime hourStart = now.minusHours(i).withMinute(0).withSecond(0).withNano(0);
            OffsetDateTime hourEnd = hourStart.plusHours(1);
            String label = hourStart.format(hourFmt);

            long nowCount = allLogs.stream().filter(l -> l.getTimestamp() != null
                    && !l.getTimestamp().isBefore(hourStart) && l.getTimestamp().isBefore(hourEnd)
                    && "NOW".equals(l.getDecision())).count();
            long laterCount = allLogs.stream().filter(l -> l.getTimestamp() != null
                    && !l.getTimestamp().isBefore(hourStart) && l.getTimestamp().isBefore(hourEnd)
                    && "LATER".equals(l.getDecision())).count();
            long neverCount = allLogs.stream().filter(l -> l.getTimestamp() != null
                    && !l.getTimestamp().isBefore(hourStart) && l.getTimestamp().isBefore(hourEnd)
                    && "NEVER".equals(l.getDecision())).count();

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", label);
            point.put("now", nowCount);
            point.put("later", laterCount);
            point.put("never", neverCount);
            timeline.add(point);
        }
        return timeline;
    }
}
