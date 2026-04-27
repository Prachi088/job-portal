package com.jobportal.job_portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Autowired
    private RestTemplate restTemplate;

    private static final String URL =
            "https://api.groq.com/openai/v1/chat/completions";

    public String getResponse(String userMessage) {

        // Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // Build system prompt
        Map<String, Object> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a helpful assistant for a job portal. Help users with job applications, resume tips, interview preparation, and career advice. Keep responses concise and relevant.");

        // Build user message
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);

        // Add both to messages list
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(message);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("messages", messages);
        body.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    URL,
                    entity,
                    Map.class
            );

            if (response == null) return "No response from AI";

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            if (choices == null || choices.isEmpty())
                return "No AI response";

            Map<String, Object> firstChoice = choices.get(0);

            @SuppressWarnings("unchecked")
            Map<String, Object> messageResponse =
                    (Map<String, Object>) firstChoice.get("message");

            Object content = messageResponse.get("content");
            return content != null ? content.toString() : "Empty response";

        } catch (HttpClientErrorException.TooManyRequests e) {
            return "AI is busy, please try again in a few seconds 🙏";
        } catch (Exception e) {
            e.printStackTrace();
            return "AI error occurred: " + e.getMessage();
        }
    }
}