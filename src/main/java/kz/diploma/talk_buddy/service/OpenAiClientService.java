package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OpenAiClientService {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String URL = "https://api.openai.com/v1/responses";

    public String callAI(String prompt) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4.1-mini");

        List<Object> content = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "input_text");
        textPart.put("text", prompt);

        content.add(textPart);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", content);

        body.put("input", List.of(message));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                URL,
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }

    public String callAIWithAudio(List<String> audioBase64List, String prompt) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(properties.getApiKey());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4.1-mini");

        List<Object> input = new ArrayList<>();

        // 🔥 текстовая часть
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "input_text");
        textPart.put("text", prompt);

        input.add(textPart);

        // 🔥 аудио часть
        for (String audio : audioBase64List) {

            Map<String, Object> audioPart = new HashMap<>();

            audioPart.put("type", "input_audio");

            Map<String, Object> audioData = new HashMap<>();
            audioData.put("data", audio.split(",")[1]); // base64 без prefix
            audioData.put("format", "webm");

            audioPart.put("input_audio", audioData);

            input.add(audioPart);
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", input);

        body.put("input", List.of(message));

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/responses",
                HttpMethod.POST,
                request,
                String.class
        );

        return response.getBody();
    }
}