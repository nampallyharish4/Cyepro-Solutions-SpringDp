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
public class AuditLogResponse {
    private UUID id;
    private UUID eventId;
    private String eventType;
    private String eventMessage;
    private String source;
    private String priorityHint;
    private String decision;
    private String reason;
    private String ruleName;
    private String aiModel;
    private Double aiConfidence;
    private Boolean aiFallbackUsed;
    private OffsetDateTime timestamp;
}
