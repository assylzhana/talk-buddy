package kz.diploma.talk_buddy.service;

import kz.diploma.talk_buddy.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AudioService {

    private final OpenAiProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public String generateAudio(String text) {

        try {
            String url = "https://api.openai.com/v1/audio/speech";

            String body = """
            {
              "model": "gpt-4o-mini-tts",
              "voice": "alloy",
              "input": "%s"
            }
            """.formatted(text.replace("\"", "\\\""));

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(props.getApiKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            String fileName = UUID.randomUUID() + ".mp3";

            Path path = Path.of("src/main/resources/static/audio/" + fileName);
            Files.createDirectories(path.getParent());

            try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
                fos.write(response.getBody());
            }

            return "/audio/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Audio generation failed", e);
        }
    }
}