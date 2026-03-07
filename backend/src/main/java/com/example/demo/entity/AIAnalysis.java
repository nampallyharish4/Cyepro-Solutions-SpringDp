package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_analysis")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AIAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "event_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Event event;

    @Column(nullable = false)
    private String classification; 

    @Column(nullable = false)
    private Double confidence;

    private String reason;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "fallback_used")
    @Builder.Default
    private Boolean fallbackUsed = false;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
