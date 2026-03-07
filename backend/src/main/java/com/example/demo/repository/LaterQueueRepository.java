package com.example.demo.repository;

import com.example.demo.entity.LaterQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LaterQueueRepository extends JpaRepository<LaterQueue, UUID> {
    List<LaterQueue> findByStatusAndNextRunAtBefore(String status, OffsetDateTime time);
}
