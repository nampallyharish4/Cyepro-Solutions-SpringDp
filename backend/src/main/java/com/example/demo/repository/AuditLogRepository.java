package com.example.demo.repository;

import com.example.demo.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    long countByDecision(String decision);

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.event LEFT JOIN FETCH a.aiAnalysis LEFT JOIN FETCH a.ruleTriggered ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a")
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query(value = "SELECT a FROM AuditLog a LEFT JOIN FETCH a.event LEFT JOIN FETCH a.aiAnalysis LEFT JOIN FETCH a.ruleTriggered WHERE a.decision = :decision ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(a) FROM AuditLog a WHERE a.decision = :decision")
    Page<AuditLog> findByDecisionOrderByTimestampDesc(@Param("decision") String decision, Pageable pageable);

    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.event LEFT JOIN FETCH a.aiAnalysis LEFT JOIN FETCH a.ruleTriggered WHERE " +
           "(:decision IS NULL OR a.decision = :decision) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findFiltered(@Param("decision") String decision, Pageable pageable);

    @Query("SELECT a FROM AuditLog a LEFT JOIN FETCH a.event LEFT JOIN FETCH a.aiAnalysis LEFT JOIN FETCH a.ruleTriggered ORDER BY a.timestamp DESC LIMIT 10")
    List<AuditLog> findTop10ByOrderByTimestampDesc();
}
