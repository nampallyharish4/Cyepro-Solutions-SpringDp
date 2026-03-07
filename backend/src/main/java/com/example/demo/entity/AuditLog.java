package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private String decision;

    @ManyToOne
    @JoinColumn(name = "rule_id")
    private Rule ruleTriggered;

    @Column(nullable = false)
    private String reason;

    @OneToOne
    @JoinColumn(name = "ai_analysis_id")
    private AIAnalysis aiAnalysis;

    @Column(updatable = false)
    private OffsetDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(UUID id, Event event, String decision, Rule ruleTriggered, String reason, AIAnalysis aiAnalysis, OffsetDateTime timestamp) {
        this.id = id;
        this.event = event;
        this.decision = decision;
        this.ruleTriggered = ruleTriggered;
        this.reason = reason;
        this.aiAnalysis = aiAnalysis;
        this.timestamp = timestamp;
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = OffsetDateTime.now();
        }
    }

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public Rule getRuleTriggered() { return ruleTriggered; }
    public void setRuleTriggered(Rule ruleTriggered) { this.ruleTriggered = ruleTriggered; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public AIAnalysis getAiAnalysis() { return aiAnalysis; }
    public void setAiAnalysis(AIAnalysis aiAnalysis) { this.aiAnalysis = aiAnalysis; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public static class AuditLogBuilder {
        private UUID id;
        private Event event;
        private String decision;
        private Rule ruleTriggered;
        private String reason;
        private AIAnalysis aiAnalysis;
        private OffsetDateTime timestamp;

        public AuditLogBuilder id(UUID id) { this.id = id; return this; }
        public AuditLogBuilder event(Event event) { this.event = event; return this; }
        public AuditLogBuilder decision(String decision) { this.decision = decision; return this; }
        public AuditLogBuilder ruleTriggered(Rule ruleTriggered) { this.ruleTriggered = ruleTriggered; return this; }
        public AuditLogBuilder reason(String reason) { this.reason = reason; return this; }
        public AuditLogBuilder aiAnalysis(AIAnalysis aiAnalysis) { this.aiAnalysis = aiAnalysis; return this; }
        public AuditLogBuilder timestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            return new AuditLog(id, event, decision, ruleTriggered, reason, aiAnalysis, timestamp);
        }
    }
}