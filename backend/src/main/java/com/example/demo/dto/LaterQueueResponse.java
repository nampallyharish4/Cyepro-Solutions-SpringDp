package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaterQueueResponse {
    private UUID id;
    private UUID eventId;
    private String eventMessage;
    private String eventType;
    private Integer retryCount;
    private OffsetDateTime nextRunAt;
    private String status;
    private OffsetDateTime createdAt;
}
