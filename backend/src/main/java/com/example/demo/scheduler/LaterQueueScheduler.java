package com.example.demo.scheduler;

import com.example.demo.dto.EventRequest;
import com.example.demo.entity.Event;
import com.example.demo.entity.LaterQueue;
import com.example.demo.repository.LaterQueueRepository;
import com.example.demo.service.PrioritizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class LaterQueueScheduler {
    private static final Logger log = LoggerFactory.getLogger(LaterQueueScheduler.class);

    private final LaterQueueRepository laterQueueRepository;
    private final PrioritizationService prioritizationService;

    public LaterQueueScheduler(LaterQueueRepository laterQueueRepository, PrioritizationService prioritizationService) {
        this.laterQueueRepository = laterQueueRepository;
        this.prioritizationService = prioritizationService;
    }

    @Scheduled(fixedRate = 300000) // Every 5 minutes
// ...existing code...
    @Transactional
    public void processLaterQueue() {
        log.info("Running Later Queue processing job...");
        List<LaterQueue> pendingTasks = laterQueueRepository.findByStatusAndNextRunAtBefore("PENDING", OffsetDateTime.now());

        for (LaterQueue task : pendingTasks) {
            try {
                task.setStatus("PROCESSING");
                laterQueueRepository.save(task);

                Event event = task.getEvent();
                EventRequest request = new EventRequest();
                request.setUserId(event.getUserId());
                request.setEventType(event.getEventType());
                request.setMessage(event.getMessage());
                request.setSource(event.getSource());
                request.setPriorityHint(event.getPriorityHint());
                request.setChannel(event.getChannel());
                request.setMetadata(event.getMetadata());
                request.setDedupeKey(event.getDedupeKey());
                request.setExpiresAt(event.getExpiresAt());

                prioritizationService.process(request);

                task.setStatus("COMPLETED");
                laterQueueRepository.save(task);
            } catch (Exception e) {
                log.error("Failed to process deferred event {}: {}", task.getId(), e.getMessage());
                task.setRetryCount(task.getRetryCount() + 1);
                if (task.getRetryCount() >= 3) {
                    task.setStatus("FAILED");
                    // Logic to move to Dead Letter Queue could be added here
                } else {
                    task.setStatus("PENDING");
                    task.setNextRunAt(OffsetDateTime.now().plusMinutes(5));
                }
                laterQueueRepository.save(task);
            }
        }
    }
}
