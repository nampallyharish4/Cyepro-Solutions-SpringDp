package com.example.demo.repository;

import com.example.demo.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    Optional<Event> findFirstByDedupeKey(String dedupeKey);
    List<Event> findFirst5ByUserIdOrderByCreatedAtDesc(String userId);
    Page<Event> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Event> findByEventTypeContainingIgnoreCaseOrderByCreatedAtDesc(String eventType, Pageable pageable);
}
