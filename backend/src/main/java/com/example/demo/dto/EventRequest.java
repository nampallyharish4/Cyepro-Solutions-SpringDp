package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventRequest {
    @NotNull
    private String userId;
    @NotNull
    private String eventType;
    private String title;
    private String message;
    @NotNull
    private String source;
    private String priorityHint;
    private String channel;
    private Map<String, Object> metadata;
    private String dedupeKey;
    private OffsetDateTime expiresAt;
}
