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
public class RuleResponse {
    private UUID id;
    private String name;
    private Map<String, Object> conditionJson;
    private String action;
    private Integer priority;
    private Boolean enabled;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
