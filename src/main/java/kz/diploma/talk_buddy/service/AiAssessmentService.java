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
        - 15 multiple choice questions
        - 5 open questions
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

    public List<CreateQuestionRequest> generateQuestions(GenerateRequest req) {

        int total = req.getCount();
        int testCount = (int) (total * 0.7);
        int fillCount = total - testCount;

        String prompt = """
Generate %d English learning questions.

Topic: %s
Description: %s

LEVEL: %s

You MUST strictly follow this level.

Level rules:
A1 → very simple words, short sentences, present simple
A2 → simple sentences, basic grammar
B1 → intermediate grammar, more vocabulary
B2 → complex sentences, advanced grammar

DO NOT go above or below the level.

Rules:
- %d TEST questions (4 options, 1 correct)
- %d FILL_GAP questions

Return ONLY JSON:
[
 {
  "type": "TEST",
  "questionText": "...",
  "answers": ["a","b","c","d"],
  "correctIndex": 1
 },
 {
  "type": "FILL_GAP",
  "questionText": "...",
  "correctAnswer": "..."
 }
]
""".formatted(
                total,
                req.getTopic(),
                req.getDescription(),
                req.getLevel(),   // 🔥 LEVEL ТУТ
                testCount,
                fillCount
        );

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

            CreateQuestionRequest[] arr =
                    objectMapper.readValue(json, CreateQuestionRequest[].class);

            List<CreateQuestionRequest> list = Arrays.asList(arr);

            // 🔥 МАППИНГ answers → answer1..4
            for (CreateQuestionRequest q : list) {
                if ("TEST".equals(q.getType()) && q.getAnswers() != null) {

                    List<String> a = q.getAnswers();

                    if (a.size() > 0) q.setAnswer1(a.get(0));
                    if (a.size() > 1) q.setAnswer2(a.get(1));
                    if (a.size() > 2) q.setAnswer3(a.get(2));
                    if (a.size() > 3) q.setAnswer4(a.get(3));
                }
            }

            return list;

        } catch (Exception e) {
            throw new RuntimeException("AI generate error", e);
        }
    }

    private List<CreateQuestionRequest> parseJson(String response) {

        try {
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

            CreateQuestionRequest[] arr =
                    objectMapper.readValue(json, CreateQuestionRequest[].class);

            return Arrays.asList(arr);

        } catch (Exception e) {
            throw new RuntimeException("Parse error", e);
        }
    }
}