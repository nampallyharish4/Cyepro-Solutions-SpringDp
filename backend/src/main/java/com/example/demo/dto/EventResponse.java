package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponse {
    private UUID id;
    private String userId;
    private String eventType;
    private String title;
    private String message;
    private String source;
    private String priorityHint;
    private String channel;
    private Map<String, Object> metadata;
    private String dedupeKey;
    private OffsetDateTime createdAt;
}
