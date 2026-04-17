package com.todo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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

    @PostMapping({"/parse-task", "/voice-to-task"})
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

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "MiniMax-M2.7");
            requestBody.put("max_tokens", 500);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(MINIMAX_URL, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().isBlank()) {
                return ResponseEntity.status(502).body(Map.of("error", "AI service returned an invalid response"));
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root.path("content").isArray() && root.path("content").size() > 0
                ? root.path("content").get(0).path("text").asText("")
                : "";

            text = extractJson(text);

            JsonNode taskJson = objectMapper.readTree(text);
            Map<String, Object> normalized = normalizeTask(taskJson, transcript);
            return ResponseEntity.ok(Map.of(
                "ok", true,
                "type", "task",
                "data", normalized
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "AI parsing failed: " + e.getMessage()));
        }
    }

    private String buildPrompt(String transcript, String today) {
        return "You are a voice-to-task parser. Convert the spoken content into a structured JSON task object.\n" +
               "Return ONLY valid JSON with these exact fields:\n" +
               "- \"title\" (string, required): concise task title, max 80 chars\n" +
               "- \"description\" (string): longer task details or extra context, else \"\"\n" +
               "- \"category\" (string): \"work\" or \"personal\" based on context, default \"work\"\n" +
               "- \"priority\" (string): \"low\", \"medium\", or \"high\" based on urgency words (urgent/gấp/quan trọng = high), default \"medium\"\n" +
               "- \"deadline\" (string|null): ISO 8601 datetime (YYYY-MM-DDTHH:mm:ss) if date/time mentioned, else null\n" +
               "- \"subtasks\" (array): optional list of subtasks, each item is {\"title\": string, \"completed\": false}\n\n" +
               "Important rules:\n" +
               "- If the user says a relative date like 'hôm nay', 'mai', 'thứ 2', convert it into an absolute datetime using today as reference.\n" +
               "- If only a date is mentioned without time, choose 09:00:00 local time.\n" +
               "- If no deadline is mentioned, use null.\n" +
               "- If the voice note mentions steps, checklist items, or multiple mini tasks, put them in subtasks.\n" +
               "- Do not invent information that was not spoken.\n\n" +
               "Voice note: \"" + transcript + "\"\n" +
               "Today is " + today + ". Return JSON only, no explanation, no markdown.";
    }

    private Map<String, Object> normalizeTask(JsonNode taskJson, String transcript) {
        Map<String, Object> result = new HashMap<>();
        String title = textValue(taskJson, "title", "");
        String description = textValue(taskJson, "description", "");
        if (title.isBlank()) {
            title = summarizeTranscript(transcript);
        }
        if (description.isBlank()) {
            description = transcript == null ? "" : transcript.trim();
        }
        result.put("title", title);
        result.put("description", description);
        result.put("category", normalizeChoice(textValue(taskJson, "category", "work"), List.of("work", "personal"), "work"));
        result.put("priority", normalizeChoice(textValue(taskJson, "priority", "medium"), List.of("low", "medium", "high"), "medium"));
        result.put("deadline", normalizeDeadline(taskJson.path("deadline").isNull() ? null : taskJson.path("deadline").asText(null)));
        result.put("subtasks", normalizeSubtasks(taskJson.path("subtasks"), transcript));
        result.put("rawTranscript", transcript == null ? "" : transcript.trim());
        return result;
    }

    private List<Map<String, Object>> normalizeSubtasks(JsonNode subtasksNode, String transcript) {
        List<Map<String, Object>> subtasks = new ArrayList<>();
        if (subtasksNode != null && subtasksNode.isArray()) {
            for (JsonNode item : subtasksNode) {
                String title = textValue(item, "title", "");
                if (title.isBlank()) continue;
                Map<String, Object> subtask = new HashMap<>();
                subtask.put("id", java.util.UUID.randomUUID().toString());
                subtask.put("note", title);
                subtask.put("title", title);
                subtask.put("completed", item.path("completed").asBoolean(false));
                subtasks.add(subtask);
            }
        }

        if (subtasks.isEmpty() && transcript != null && !transcript.isBlank()) {
            subtasks.addAll(inferSubtasks(transcript));
        }
        return subtasks;
    }

    private String textValue(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return fallback;
        String text = value.asText().trim();
        return text.isEmpty() ? fallback : text;
    }

    private String normalizeChoice(String value, List<String> allowed, String fallback) {
        if (value == null) return fallback;
        String normalized = value.trim().toLowerCase();
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private String normalizeDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) {
            return null;
        }

        try {
            LocalDateTime parsed = LocalDateTime.parse(deadline);
            return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(deadline.replace("Z", "")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            } catch (Exception e) {
                return null;
            }
        }
    }

    private String summarizeTranscript(String transcript) {
        if (transcript == null) return "New task";
        String cleaned = transcript.trim();
        if (cleaned.isEmpty()) return "New task";
        int max = Math.min(cleaned.length(), 80);
        return cleaned.substring(0, max);
    }

    private List<Map<String, Object>> inferSubtasks(String transcript) {
        List<Map<String, Object>> subtasks = new ArrayList<>();
        String[] parts = transcript.split("(?i)\\b(và|roi|rồi|sau đó|xong|tiếp theo|then|and then|,|;|\\.)\\b");
        for (String part : parts) {
            String title = part.trim();
            if (title.length() < 4) continue;
            Map<String, Object> subtask = new HashMap<>();
            subtask.put("id", java.util.UUID.randomUUID().toString());
            subtask.put("note", title);
            subtask.put("title", title);
            subtask.put("completed", false);
            subtasks.add(subtask);
        }
        return subtasks;
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
