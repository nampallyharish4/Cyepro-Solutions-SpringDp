package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "later_queue")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LaterQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Event event;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_run_at")
    private OffsetDateTime nextRunAt;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        if (nextRunAt == null) nextRunAt = OffsetDateTime.now();
    }
}
