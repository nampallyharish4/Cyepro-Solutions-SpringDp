package com.example.demo.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "events")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    private String title;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String source;

    @Column(name = "priority_hint")
    private String priorityHint;

    private String channel;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> metadata;

    @Column(name = "dedupe_key")
    private String dedupeKey;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public Event() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public static EventBuilder builder() { return new EventBuilder(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getPriorityHint() { return priorityHint; }
    public void setPriorityHint(String priorityHint) { this.priorityHint = priorityHint; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }

    public static class EventBuilder {
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
        private OffsetDateTime expiresAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime deletedAt;

        public EventBuilder id(UUID id) { this.id = id; return this; }
        public EventBuilder userId(String userId) { this.userId = userId; return this; }
        public EventBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public EventBuilder title(String title) { this.title = title; return this; }
        public EventBuilder message(String message) { this.message = message; return this; }
        public EventBuilder source(String source) { this.source = source; return this; }
        public EventBuilder priorityHint(String priorityHint) { this.priorityHint = priorityHint; return this; }
        public EventBuilder channel(String channel) { this.channel = channel; return this; }
        public EventBuilder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }
        public EventBuilder dedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; return this; }
        public EventBuilder expiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        public EventBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public EventBuilder deletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; return this; }

        public Event build() {
            Event e = new Event();
            e.id = id; e.userId = userId; e.eventType = eventType; e.title = title;
            e.message = message; e.source = source; e.priorityHint = priorityHint;
            e.channel = channel; e.metadata = metadata; e.dedupeKey = dedupeKey;
            e.expiresAt = expiresAt; e.createdAt = createdAt; e.deletedAt = deletedAt;
            return e;
        }
    }
}