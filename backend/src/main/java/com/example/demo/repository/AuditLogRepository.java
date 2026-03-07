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

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByDecisionOrderByTimestampDesc(String decision, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:decision IS NULL OR a.decision = :decision) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findFiltered(@Param("decision") String decision, Pageable pageable);

    List<AuditLog> findTop10ByOrderByTimestampDesc();
}
