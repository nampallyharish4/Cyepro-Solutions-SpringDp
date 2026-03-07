package com.example.demo.deduplication;

import com.example.demo.entity.Event;
import com.example.demo.repository.EventRepository;
import org.apache.commons.text.similarity.CosineSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeduplicationEngine {
    private static final Logger log = LoggerFactory.getLogger(DeduplicationEngine.class);
    private final EventRepository eventRepository;
    private static final double SIMILARITY_THRESHOLD = 0.85;

    public DeduplicationEngine(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public Optional<String> checkDuplicate(Event newEvent) {
        // 1. Exact duplicate with dedupe_key
        if (newEvent.getDedupeKey() != null) {
            Optional<Event> existing = eventRepository.findFirstByDedupeKey(newEvent.getDedupeKey());
            if (existing.isPresent() && !existing.get().getId().equals(newEvent.getId())) {
                return Optional.of("Exact duplicate notification by dedupe_key");
            }
        }

        // 2. Near duplicate: check latest events for same user
        List<Event> recentEvents = eventRepository.findFirst5ByUserIdOrderByCreatedAtDesc(newEvent.getUserId());
        for (Event existingEvent : recentEvents) {
            if (existingEvent.getId().equals(newEvent.getId())) continue;
            
            double similarity = calculateCosineSimilarity(newEvent.getMessage(), existingEvent.getMessage());
            if (similarity >= SIMILARITY_THRESHOLD) {
                log.info("Found similarity duplicate: {}% match with event {}", similarity * 100, existingEvent.getId());
                return Optional.of(String.format("Near-duplicate message (%.1f%% similar)", similarity * 100));
            }
        }
        
        return Optional.empty();
    }

    private double calculateCosineSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        CosineSimilarity cosineSimilarity = new CosineSimilarity();
        return cosineSimilarity.cosineSimilarity(getTermWeights(s1), getTermWeights(s2));
    }

    private Map<CharSequence, Integer> getTermWeights(String text) {
        Map<CharSequence, Integer> weights = new HashMap<>();
        for (String word : text.toLowerCase().split("\\s+")) {
            weights.put(word, weights.getOrDefault(word, 0) + 1);
        }
        return weights;
    }
}
