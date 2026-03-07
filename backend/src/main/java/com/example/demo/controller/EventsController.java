package com.example.demo.controller;

import com.example.demo.dto.EventResponse;
import com.example.demo.entity.Event;
import com.example.demo.entity.LaterQueue;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.EventRepository;
import com.example.demo.repository.LaterQueueRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class EventsController {

    private final EventRepository eventRepository;
    private final LaterQueueRepository laterQueueRepository;

    public EventsController(EventRepository eventRepository, LaterQueueRepository laterQueueRepository) {
        this.eventRepository = eventRepository;
        this.laterQueueRepository = laterQueueRepository;
    }

    @GetMapping("/events/list")
    public ResponseEntity<Map<String, Object>> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Event> eventPage;

        if (eventType != null && !eventType.isEmpty()) {
            eventPage = eventRepository.findByEventTypeContainingIgnoreCaseOrderByCreatedAtDesc(eventType, pageable);
        } else {
            eventPage = eventRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<EventResponse> events = eventPage.getContent().stream()
                .map(this::toEventResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "content", events,
                "totalElements", eventPage.getTotalElements(),
                "totalPages", eventPage.getTotalPages(),
                "currentPage", eventPage.getNumber(),
                "pageSize", eventPage.getSize()
        ));
    }

    @GetMapping({"/deferred-queue", "/later-queue"})
    public ResponseEntity<Map<String, Object>> getDeferredQueue(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        List<LaterQueue> allItems = laterQueueRepository.findAll();

        // Filter by status
        if (status != null && !status.isEmpty()) {
            allItems = allItems.stream()
                    .filter(q -> status.equalsIgnoreCase(q.getStatus()))
                    .collect(Collectors.toList());
        }

        // Filter by search
        if (search != null && !search.isEmpty()) {
            String lowerSearch = search.toLowerCase();
            allItems = allItems.stream().filter(q -> {
                Event evt = q.getEvent();
                if (evt == null) return false;
                return (evt.getTitle() != null && evt.getTitle().toLowerCase().contains(lowerSearch))
                        || (evt.getSource() != null && evt.getSource().toLowerCase().contains(lowerSearch))
                        || (evt.getEventType() != null && evt.getEventType().toLowerCase().contains(lowerSearch))
                        || (evt.getUserId() != null && evt.getUserId().toLowerCase().contains(lowerSearch));
            }).collect(Collectors.toList());
        }

        // Sort by createdAt desc
        allItems.sort((a, b) -> {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        int total = allItems.size();
        int startIdx = Math.min((page - 1) * limit, total);
        int endIdx = Math.min(startIdx + limit, total);
        List<LaterQueue> pageItems = allItems.subList(startIdx, endIdx);

        List<Map<String, Object>> data = pageItems.stream().map(lq -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", lq.getId());
            item.put("status", lq.getStatus());
            item.put("retry_count", lq.getRetryCount());
            item.put("process_after", lq.getNextRunAt());
            item.put("created_at", lq.getCreatedAt());

            if (lq.getEvent() != null) {
                Event evt = lq.getEvent();
                item.put("event_id", evt.getId());
                item.put("title", evt.getTitle());
                item.put("source", evt.getSource());
                item.put("event_type", evt.getEventType());
                item.put("user_id", evt.getUserId());
                item.put("channel", evt.getChannel());
                item.put("priority_hint", evt.getPriorityHint());
            }
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("total", total);
        return ResponseEntity.ok(result);
    }

    @PostMapping({"/deferred-queue/{id}/force-send", "/later-queue/{id}/force-send"})
    public ResponseEntity<Map<String, Object>> forceSend(@PathVariable UUID id) {
        LaterQueue item = laterQueueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deferred item not found: " + id));

        item.setStatus("SENT");
        item.setNextRunAt(OffsetDateTime.now());
        laterQueueRepository.save(item);

        return ResponseEntity.ok(Map.of(
                "status", "SENT",
                "message", "Item force-sent successfully"
        ));
    }

    private EventResponse toEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType())
                .title(event.getTitle())
                .message(event.getMessage())
                .source(event.getSource())
                .priorityHint(event.getPriorityHint())
                .channel(event.getChannel())
                .metadata(event.getMetadata())
                .dedupeKey(event.getDedupeKey())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
