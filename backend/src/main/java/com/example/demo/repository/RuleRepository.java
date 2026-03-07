package com.example.demo.repository;

import com.example.demo.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {
    List<Rule> findByEnabledTrueOrderByPriorityDesc();
}
