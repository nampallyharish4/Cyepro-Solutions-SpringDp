package com.example.demo.ai;

import com.example.demo.entity.AIAnalysis;
import com.example.demo.entity.Event;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class AIService {
    private static final Logger log = LoggerFactory.getLogger(AIService.class);

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String modelName;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Gson gson = new Gson();

    public AIService() {}

    @CircuitBreaker(name = "aiService", fallbackMethod = "fallbackClassification")
    public AIAnalysis classify(Event event) {
        log.info("Calling AI service for classification using model: {}", modelName);

        String prompt = String.format("You are a notification prioritization system.\n\n" +
                        "Classify the notification into: NOW, LATER, NEVER\n\n" +
                        "Input:\nEvent Type: %s\nMessage: %s\nPriority Hint: %s\nSource: %s\n\n" +
                        "Return JSON with format: { \"classification\": \"NOW|LATER|NEVER\", \"confidence\": 0.85, \"reason\": \"explanation\" }",
                event.getEventType(), event.getMessage(), event.getPriorityHint(), event.getSource());

        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are an expert notification engine assistant."),
                        Map.of("role", "user", "content", prompt)
                },
                "temperature", 0.1,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        try {
            HttpEntity<String> request = new HttpEntity<>(gson.toJson(body), headers);
            String responseStr = restTemplate.postForObject(apiUrl, request, String.class);
            log.debug("AI Response: {}", responseStr);

            JsonObject responseJson = gson.fromJson(responseStr, JsonObject.class);
            String content = responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            log.info("Parsed AI content: {}", content);
            JsonObject classificationResult = gson.fromJson(content, JsonObject.class);

            return AIAnalysis.builder()
                    .classification(classificationResult.get("classification").getAsString().toUpperCase())
                    .confidence(classificationResult.get("confidence").getAsDouble())
                    .reason(classificationResult.get("reason").getAsString())
                    .modelUsed(modelName)
                    .build();
        } catch (Exception e) {
            log.error("Error parsing AI response: {}", e.getMessage());
            throw new RuntimeException("AI processing failed", e);
        }
    }

    public AIAnalysis fallbackClassification(Event event, Throwable t) {
        log.error("AI call failed or parsing error, using fallback logic: {}", t.getMessage());
        String classification;
        String priorityHint = event.getPriorityHint() != null ? event.getPriorityHint().toUpperCase() : "MEDIUM";

        if ("HIGH".equals(priorityHint)) {
            classification = "NOW";
        } else if ("MEDIUM".equals(priorityHint)) {
            classification = "LATER";
        } else {
            classification = "NEVER";
        }

        return AIAnalysis.builder()
                .classification(classification)
                .confidence(0.5)
                .reason("Fallback result based on priority hint due to AI engine unavailability.")
                .fallbackUsed(true)
                .modelUsed("Fallback Engine")
                .build();
    }
}