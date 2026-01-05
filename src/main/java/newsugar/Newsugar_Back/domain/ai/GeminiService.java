package newsugar.Newsugar_Back.domain.ai;

import lombok.RequiredArgsConstructor;
import newsugar.Newsugar_Back.domain.ai.clients.GeminiClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient; // 이미 있거나 만들 예정

    public String summarize(String category, List<String> summaries) {

        String joinedSummaries = summaries.stream()
                .map(s -> "- " + s)
                .collect(Collectors.joining("\n"));

        String prompt = """
        다음은 최근 %s 카테고리 뉴스들의 요약이다.
        %s

        이 뉴스들을 종합하여 아래 조건을 반드시 지켜서 요약을 생성하라.

        [요약 조건]
        - 중복되는 내용은 제거할 것
        - 객관적인 서술체를 사용할 것
        - 이모티콘, 감정 표현, 의견은 포함하지 말 것
        - 하나의 문단으로 작성할 것 (줄바꿈 금지)
        - 최소 5문장, 최대 7문장으로 작성할 것
        - 중요하다고 판단되는 핵심 단어 또는 문장은 Markdown 문법의 ==이렇게== 표시할 것
        - 리스트, 번호, 제목, 코드블록을 사용하지 말 것
        - HTML 태그를 절대 사용하지 말 것
        - React에서 바로 렌더링 가능한 순수 Markdown 텍스트만 출력할 것

        [출력 규칙]
        - 요약 내용만 출력하고 그 외 설명은 포함하지 말 것
        """.formatted(category, joinedSummaries);

        return geminiClient.generateContent(prompt);
    }

    public List<newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient.QuestionData> generateQuiz(String summaryText) {
        String prompt = """
        다음 요약문을 바탕으로 1개의 객관식 퀴즈를 생성하라.
        반드시 아래 JSON 형식으로만 출력하라. 마크다운 코드 블록(```json)이나 다른 설명은 절대 포함하지 말고 순수 JSON 문자열만 출력하라.
        
        정답 인덱스(correctIndex)는 1부터 시작하는 1-based index를 사용하라. (예: 첫 번째 보기가 정답이면 1)

        {
          "questions": [
            {
              "text": "질문 내용",
              "options": ["보기1", "보기2", "보기3", "보기4"],
              "correctIndex": 1,
              "explanation": "해설"
            }
          ]
        }
        
        [요약문]
        %s
        """.formatted(summaryText);

        try {
            String response = geminiClient.generateContent(prompt);
            if (response == null) return java.util.Collections.emptyList();

            // Clean up response
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            } else if (response.startsWith("```")) {
                response = response.substring(3);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(response);
            com.fasterxml.jackson.databind.JsonNode qs = root.get("questions");

            List<newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient.QuestionData> out = new java.util.ArrayList<>();
            if (qs != null && qs.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode q : qs) {
                    newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient.QuestionData d = new newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient.QuestionData();
                    d.text = q.has("text") ? q.get("text").asText() : "";
                    d.correctIndex = q.has("correctIndex") ? q.get("correctIndex").asInt() : 0;
                    d.explanation = q.has("explanation") ? q.get("explanation").asText() : "";

                    List<String> opts = new java.util.ArrayList<>();
                    if (q.has("options") && q.get("options").isArray()) {
                        for (com.fasterxml.jackson.databind.JsonNode o : q.get("options")) {
                            opts.add(o.asText());
                        }
                    }
                    d.options = opts;
                    out.add(d);
                }
            }
            return out;
        } catch (Exception e) {
            System.err.println("Quiz generation/parsing failed: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
}