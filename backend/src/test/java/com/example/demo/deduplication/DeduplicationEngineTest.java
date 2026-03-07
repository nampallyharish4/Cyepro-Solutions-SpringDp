package com.example.demo.deduplication;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeduplicationEngineTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private DeduplicationEngine deduplicationEngine;

    private String userId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userId = UUID.randomUUID().toString();
    }

    @Test
    void testExactDuplicateWithDedupeKey() {
        String key = "test-key";
        Event existing = Event.builder().id(UUID.randomUUID()).dedupeKey(key).build();
        Event newEvent = Event.builder().id(UUID.randomUUID()).userId(userId).dedupeKey(key).build();

        when(eventRepository.findFirstByDedupeKey(key)).thenReturn(Optional.of(existing));

        Optional<String> result = deduplicationEngine.checkDuplicate(newEvent);
        assertTrue(result.isPresent());
        assertEquals("Exact duplicate notification by dedupe_key", result.get());
    }

    @Test
    void testSimilarityDuplicate() {
        Event existing = Event.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message("login login login login failure detected")
                .build();
        
        Event newEvent = Event.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message("login login login login failure detected")
                .build();

        when(eventRepository.findFirst5ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(existing));

        Optional<String> result = deduplicationEngine.checkDuplicate(newEvent);
        assertTrue(result.isPresent());
        assertTrue(result.get().contains("Near-duplicate"));
    }

    @Test
    void testNotDuplicate() {
        Event existing = Event.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message("Low disk space on server A")
                .build();
        
        Event newEvent = Event.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .message("Memory usage at 95% on server B")
                .build();

        when(eventRepository.findFirst5ByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(existing));

        Optional<String> result = deduplicationEngine.checkDuplicate(newEvent);
        assertFalse(result.isPresent());
    }
}
