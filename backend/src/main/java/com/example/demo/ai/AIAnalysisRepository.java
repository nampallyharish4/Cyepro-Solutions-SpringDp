package com.example.demo.ai;

import com.example.demo.entity.AIAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AIAnalysisRepository extends JpaRepository<AIAnalysis, UUID> {
}
