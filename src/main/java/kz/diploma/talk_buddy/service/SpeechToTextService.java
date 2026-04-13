package kz.diploma.talk_buddy.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.diploma.talk_buddy.config.OpenAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Service
@RequiredArgsConstructor
public class SpeechToTextService {

    private final OpenAiProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public String transcribe(String base64) {

        try {
            byte[] audioBytes = Base64.getDecoder()
                    .decode(base64.split(",")[1]);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(properties.getApiKey());
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "audio.webm";
                }
            });

            body.add("model", "gpt-4o-mini-transcribe");

            HttpEntity<MultiValueMap<String, Object>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.openai.com/v1/audio/transcriptions",
                    HttpMethod.POST,
                    request,
                    String.class
            );

            return new ObjectMapper()
                    .readTree(response.getBody())
                    .get("text")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Speech-to-text error", e);
        }
    }
}