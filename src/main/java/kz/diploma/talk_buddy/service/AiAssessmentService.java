package kz.diploma.talk_buddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.diploma.talk_buddy.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAssessmentService {

    private final OpenAiClientService aiClient;
    private final SpeechToTextService speechService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 🔥 1. ГЕНЕРАЦИЯ
    public AssessmentDto generateAssessment() {

        String prompt = """
        Generate English placement test.

        Requirements:
        - 20 multiple choice questions
        - 10 open questions
        - 5 speaking questions

        IMPORTANT:
        - speaking = ONLY questions
        - NO listening
        - NO answers

        Return ONLY JSON:

        {
          "mcq":[
            {"id":"1","question":"...","options":["A","B","C","D"]}
          ],
          "open":[
            {"id":"1","question":"..."}
          ],
          "speaking":[
            {"id":"1","question":"..."}
          ]
        }
        """;

        try {
            String response = aiClient.callAI(prompt);

            JsonNode root = objectMapper.readTree(response);

            String json = root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            return objectMapper.readValue(json, AssessmentDto.class);

        } catch (Exception e) {
            throw new RuntimeException("AI generation error", e);
        }
    }

    // 🔥 2. ОЦЕНКА (ПРАВИЛЬНАЯ)
    public AssessmentResultDto evaluate(AssessmentSubmissionDto submission) {

        try {

            // 🔥 1. SPEAKING → TEXT
            List<String> spokenTexts = new ArrayList<>();

            if (submission.getSpeakingAnswers() != null) {
                for (SpeakingAnswerDto a : submission.getSpeakingAnswers()) {

                    if (a.getAudioBase64() != null && !a.getAudioBase64().isBlank()) {

                        String text = speechService.transcribe(a.getAudioBase64());

                        spokenTexts.add(text);
                    }
                }
            }

            // 🔥 2. СОБИРАЕМ ВСЕ ОТВЕТЫ
            Map<String, Object> data = new HashMap<>();
            data.put("mcq", submission.getMultipleChoiceAnswers());
            data.put("open", submission.getOpenAnswers());
            data.put("speaking", spokenTexts);

            String answersJson = objectMapper.writeValueAsString(data);

            // 🔥 3. PROMPT
            String prompt = """
            You are a professional English teacher.

            Evaluate student level (A1, A2, B1, B2).

            Evaluate:
            - grammar
            - vocabulary
            - speaking quality
            - fluency

            Answers:
            %s

            Return ONLY JSON:
            {
              "level": "B1",
              "totalScore": 70,
              "feedback": "Detailed feedback about mistakes"
            }
            """.formatted(answersJson);

            String response = aiClient.callAI(prompt);

            JsonNode root = objectMapper.readTree(response);

            String json = root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            JsonNode result = objectMapper.readTree(json);

            return new AssessmentResultDto(
                    result.get("level").asText(),
                    0,
                    0,
                    0,
                    result.get("totalScore").asInt(),
                    result.get("feedback").asText()
            );

        } catch (Exception e) {
            throw new RuntimeException("AI evaluation error", e);
        }
    }
}