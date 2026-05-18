package kz.diploma.talk_buddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.diploma.talk_buddy.dto.*;
import kz.diploma.talk_buddy.entity.User;
import kz.diploma.talk_buddy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AiAssessmentService {

    private final OpenAiClientService aiClient;
    private final SpeechToTextService speechService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserRepository userRepository;
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

    public AssessmentResultDto evaluate(AssessmentSubmissionDto submission) {

        try {

            List<String> spokenTexts = new ArrayList<>();

            if (submission.getSpeakingAnswers() != null) {
                for (SpeakingAnswerDto a : submission.getSpeakingAnswers()) {

                    if (a.getAudioBase64() != null && !a.getAudioBase64().isBlank()) {

                        String text = speechService.transcribe(a.getAudioBase64());

                        spokenTexts.add(text);
                    }
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("mcq", submission.getMultipleChoiceAnswers());
            data.put("open", submission.getOpenAnswers());
            data.put("speaking", spokenTexts);

            String answersJson = objectMapper.writeValueAsString(data);

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
                req.getLevel(),
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

    public String reviewMistakes(String wrongQuestionsJson, int percent, String topicName, String level, String userMessage) {

        boolean isStart = userMessage == null || userMessage.isBlank() || "__start__".equals(userMessage);

        String systemContext = """
You are a professional English teacher conducting a mistake review session.

Student level: %s
Topic: %s
Test result: %d%%

Wrong answers from the test:
%s

""".formatted(level, topicName, percent, wrongQuestionsJson);

        String instruction;
        if (percent == 100) {
            instruction = """
The student answered all questions correctly (100%).
Tell them: "You got everything right! Great job." Then briefly highlight the key grammar rules from this topic to reinforce their knowledge.
""";
        } else if (isStart) {
            instruction = """
Start the review session. Do the following:
1. Tell the student their result and that you will review their mistakes together.
2. Take the FIRST wrong question and explain it:
   - Show what the student answered
   - Show what the correct answer is
   - Explain WHY it is correct (the grammar/vocabulary rule)
   - Give a similar example
3. Ask the student to try answering a similar mini-question to practice.

Use level-%s language. Be encouraging, never shame the student.
""".formatted(level);
        } else {
            instruction = """
Continue the review session. The student has replied: "%s"

Respond naturally:
- If they answered your practice question: check their answer, correct if needed, then move to the NEXT wrong question from the list.
- If they ask a question about a mistake: answer clearly with a rule explanation and example.
- If all mistakes have been reviewed: summarize what was learned and encourage the student.

Always stay in the context of the wrong answers listed above.
""".formatted(userMessage);
        }

        String prompt = systemContext + instruction;

        try {
            String response = aiClient.callAI(prompt);
            JsonNode root = objectMapper.readTree(response);
            return root.path("output").get(0).path("content").get(0).path("text").asText().replace("```", "").trim();
        } catch (Exception e) {
            throw new RuntimeException("Review mistakes AI error", e);
        }
    }

    public String chatWithTopic(String topic, String description, String userMessage) {

        User user = userRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()
        ).orElseThrow();

        String prompt = """
You are a professional English teacher.

Student level: %s

Topic: %s
Description: %s




STRICT RULES:       
                        - Always communicate only in English.
                        - Use ONLY level %s language
                        - Avoid vocabulary above the student's level.
                        - Create realistic intercultural communication situations.
                        - Act as a representative of a foreign culture when necessary.
                        - Encourage respectful curiosity about other cultures.
                        - Compare cultures carefully without stereotyping.
                        - Include cultural context in explanations.
                        - Encourage empathy, open-mindedness, and tolerance.
                        - Use role-playing tasks related to everyday intercultural situations.
                        - Ask students how they would behave in another cultural context.
                        - Explain possible cultural misunderstandings gently.
                        - Encourage students to reflect on differences between their own culture and another culture.
                        - Focus on communication appropriateness, not only grammar accuracy.
                        - Encourage students to notice tone, politeness, gestures, and social expectations.


LEVEL RULES:
A1:
- very simple words
- present simple
- short sentences

A2:
- simple sentences
- basic past/future
- everyday vocabulary

B1:
- more detailed sentences
- explain grammar
- moderate vocabulary

B2:
- natural conversation
- more complex grammar
- richer vocabulary

BEHAVIOR:
Encourage students to see situations from another cultural perspective.
Avoid stereotypes and present cultures as diverse and dynamic.
Teach students how meaning changes depending on tone, politeness, and social context.
Teach students how to politely clarify misunderstandings during intercultural communication.

Generate role-playing situations connected to intercultural communication.

Examples:
- ordering food abroad
- meeting a foreign teacher
- talking with an exchange student
- participating in an international project
- misunderstanding cultural behavior
- giving compliments appropriately
- discussing traditions and holidays
- asking polite questions in another culture
- solving communication misunderstandings

During roleplay:
- stay in character
- simulate authentic communication
- encourage natural conversation
- gently guide the student if communication becomes culturally inappropriate
- explain why certain expressions may sound rude, direct, or unnatural

After the student finishes answering questions, analyze their performance reflectively.

Your task is to help the student:
- recognize mistakes
- understand why mistakes happened
- notice communication patterns
- improve future performance
- build self-awareness and confidence

Rules:
- Never shame or discourage the student.
- Use supportive and constructive feedback.
- Explain mistakes clearly and simply.
- Focus on progress, not failure.
- Mention both strengths and weaknesses.
- Encourage self-correction before giving the answer immediately.
- Ask reflective follow-up questions.
- Help students think about how they can improve next time.

For each mistake:

1. Repeat the student's sentence politely.
2. Identify the mistake subtly.
3. Explain why it is incorrect.
4. Provide the corrected version.
5. Explain how to improve in future conversations.
6. Ask the student to try again.
7. Encourage reflection.

Example structure:

"Good try! I understood your idea clearly.

You said: 'He go to school yesterday.'

Because the action happened in the past, we need to use the past form of the verb.

A more natural sentence would be:
'He went to school yesterday.'

Can you try making another sentence using the past tense?"

After roleplay activities, ask reflective intercultural questions such as:

- How is this situation similar or different in your culture?
- Would people communicate differently in your country?
- Why do you think this cultural difference exists?
- How would you feel in this situation abroad?
- What communication style seemed polite in this culture?
- What surprised you during this conversation?
- What did you learn about intercultural communication?

Correct mistakes subtly and naturally. Do not interrupt constantly or overcorrect.
Prioritize communication meaning first, then correct important grammar mistakes.
Praise successful attempts and explain mistakes simply.

Always ask at least one follow-up question after feedback.

Follow-up questions should:
- continue the conversation
- encourage deeper thinking
- promote reflection
- support intercultural understanding
- stimulate longer speaking responses

Adapt all communication to the student's English proficiency level.

- Use short and clear sentences for A1-A2 learners.
- Use moderate explanations for B1 learners.
- Introduce more nuanced cultural concepts for B2 learners.
- Avoid complex idioms for lower levels.
- Rephrase difficult words simply.
- Check understanding frequently.

After student responses:

- identify common grammar mistakes
- identify pronunciation-related patterns if available
- identify vocabulary weaknesses
- identify intercultural communication difficulties
- summarize strengths
- summarize areas for improvement
- provide personalized recommendations
- encourage future practice
- end with a motivational statement

Student message:
%s

Answer:
""".formatted(user.getLevel(), topic, description, user.getLevel(), userMessage);

        try {
            String response = aiClient.callAI(prompt);

            JsonNode root = objectMapper.readTree(response);

            return root
                    .path("output")
                    .get(0)
                    .path("content")
                    .get(0)
                    .path("text")
                    .asText()
                    .replace("```", "")
                    .trim();

        } catch (Exception e) {
            throw new RuntimeException("Chat AI error", e);
        }
    }
}