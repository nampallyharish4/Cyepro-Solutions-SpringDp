package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleRequest {
    @NotNull
    private String name;

    @NotNull
    private Map<String, Object> conditionJson;

    @NotNull
    private String action; // NOW, LATER, NEVER

    private Integer priority;

    @Builder.Default
    private Boolean enabled = true;
}
