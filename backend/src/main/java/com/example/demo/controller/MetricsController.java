package com.example.demo.controller;

import com.example.demo.service.MetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetricsController {
    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping({"/metrics", "/stats"})
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(metricsService.getEnhancedStats());
    }

    @GetMapping("/metrics/timeline")
    public ResponseEntity<List<Map<String, Object>>> getTimeline() {
        return ResponseEntity.ok(metricsService.getHourlyTimeline());
    }
}
