package com.example.demo.controller;

import com.example.demo.entity.Rule;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rules")
public class RulesController {

    private final RuleRepository ruleRepository;

    public RulesController(RuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllRules() {
        List<Map<String, Object>> rules = ruleRepository.findAll().stream()
                .filter(r -> r.getDeletedAt() == null)
                .map(this::toFrontendFormat)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRule(@PathVariable UUID id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        return ResponseEntity.ok(toFrontendFormat(rule));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRule(@RequestBody Map<String, Object> request) {
        Rule rule = fromFrontendFormat(request, new Rule());
        rule = ruleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(toFrontendFormat(rule));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable UUID id, @RequestBody Map<String, Object> request) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        rule = fromFrontendFormat(request, rule);
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(toFrontendFormat(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setDeletedAt(java.time.OffsetDateTime.now());
        ruleRepository.save(rule);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRule(@PathVariable UUID id) {
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setEnabled(!rule.getEnabled());
        rule = ruleRepository.save(rule);
        return ResponseEntity.ok(toFrontendFormat(rule));
    }

    /**
     * Convert frontend format (condition_type, condition_value, target_priority, priority_order)
     * to internal Rule entity (conditionJson, action, priority).
     */
    private Rule fromFrontendFormat(Map<String, Object> request, Rule rule) {
        if (request.containsKey("name")) {
            rule.setName((String) request.get("name"));
        }

        // Accept both frontend format and backend format
        if (request.containsKey("condition_type") || request.containsKey("condition_value")) {
            Map<String, Object> condJson = new HashMap<>();
            condJson.put("type", request.getOrDefault("condition_type", "source"));
            condJson.put("value", request.getOrDefault("condition_value", ""));
            rule.setConditionJson(condJson);
        } else if (request.containsKey("condition_json")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cj = (Map<String, Object>) request.get("condition_json");
            rule.setConditionJson(cj);
        }

        if (request.containsKey("target_priority")) {
            rule.setAction(((String) request.get("target_priority")).toUpperCase());
        } else if (request.containsKey("action")) {
            rule.setAction(((String) request.get("action")).toUpperCase());
        }

        if (request.containsKey("priority_order")) {
            rule.setPriority(((Number) request.get("priority_order")).intValue());
        } else if (request.containsKey("priority")) {
            rule.setPriority(((Number) request.get("priority")).intValue());
        }

        if (request.containsKey("enabled")) {
            rule.setEnabled((Boolean) request.get("enabled"));
        } else if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }

        return rule;
    }

    /**
     * Convert Rule entity to frontend format with condition_type, condition_value, target_priority, priority_order.
     */
    private Map<String, Object> toFrontendFormat(Rule rule) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", rule.getId());
        result.put("name", rule.getName());

        // Extract condition_type and condition_value from conditionJson
        if (rule.getConditionJson() != null) {
            // Support both "type" (new frontend format) and "field" (seed data format) as condition_type
            Object type = rule.getConditionJson().get("type");
            if (type == null) type = rule.getConditionJson().get("field");
            result.put("condition_type", type != null ? type : "source");
            result.put("condition_value", rule.getConditionJson().getOrDefault("value", ""));
        } else {
            result.put("condition_type", "source");
            result.put("condition_value", "");
        }

        result.put("target_priority", rule.getAction());
        result.put("priority_order", rule.getPriority());
        result.put("enabled", rule.getEnabled());
        result.put("created_at", rule.getCreatedAt());
        result.put("updated_at", rule.getUpdatedAt());

        // Also include original backend fields for backward compatibility
        result.put("condition_json", rule.getConditionJson());
        result.put("action", rule.getAction());
        result.put("priority", rule.getPriority());
        return result;
    }
}
