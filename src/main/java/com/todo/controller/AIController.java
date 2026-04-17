package com.todo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Value("${minimax.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String MINIMAX_URL = "https://api.minimax.io/anthropic/v1/messages";

    @PostMapping("/parse-task")
    public ResponseEntity<?> parseTask(@RequestBody Map<String, String> body, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String transcript = body.get("transcript");
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Empty transcript"));
        }

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "MiniMax API key not configured"));
        }

        try {
            String today = LocalDate.now().toString();
            String prompt = buildPrompt(transcript, today);

            // Build request body (Anthropic Messages API format)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "MiniMax-M2.7");
            requestBody.put("max_tokens", 400);
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(MINIMAX_URL, request, String.class);

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("content").get(0).path("text").asText();

            // Extract JSON from response text (in case model adds extra text)
            text = extractJson(text);

            JsonNode taskJson = objectMapper.readTree(text);
            return ResponseEntity.ok(taskJson);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "AI parsing failed: " + e.getMessage()));
        }
    }

    private String buildPrompt(String transcript, String today) {
        return "You are a task parser. Parse the following voice note into a JSON task object.\n" +
               "Return ONLY valid JSON with these exact fields:\n" +
               "- \"title\" (string, required): concise task title, max 80 chars\n" +
               "- \"description\" (string): additional details if mentioned, else \"\"\n" +
               "- \"category\" (string): \"work\" or \"personal\" based on context, default \"work\"\n" +
               "- \"priority\" (string): \"low\", \"medium\", or \"high\" based on urgency words (urgent/gấp/quan trọng = high), default \"medium\"\n" +
               "- \"deadline\" (string|null): ISO 8601 datetime (YYYY-MM-DDTHH:mm:ss) if date/time mentioned, else null\n\n" +
               "Voice note: \"" + transcript + "\"\n" +
               "Today is " + today + ". Return JSON only, no explanation, no markdown.";
    }

    private String extractJson(String text) {
        // Find first { and last } to extract JSON object
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }
}
