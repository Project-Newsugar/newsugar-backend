package newsugar.Newsugar_Back.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-proxy")
public class AiProxyController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final String apiKey;

    public AiProxyController() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("QUIZ_AI_API_KEY");
        if (k == null) k = env.get("QUIZ_AI_API_KEY");
        this.apiKey = k;
    }

    public static class SummaryReq {
        public String summary;
    }

    public static class SummarizeReq {
        public String content;
    }

    @PostMapping("/generate-quiz")
    public ResponseEntity<String> generate(@RequestHeader(value = "Authorization", required = false) String auth,
                                           @RequestBody SummaryReq req) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(500).body(jsonError("AI 설정이 없습니다"));
        }
        if (auth == null || !auth.startsWith("Bearer ") || !apiKey.equals(auth.substring(7))) {
            return ResponseEntity.status(401).body(jsonError("unauthorized"));
        }
        try {
            String prompt = "다음 요약문을 기반으로 한국 뉴스 관련 4지선다 객관식 문제를 1개 생성하세요. " +
                    "반드시 순수 JSON만 반환하고, 코드블록이나 추가 텍스트는 금지합니다. " +
                    "출력 형식은 정확히 {\"questions\":[{\"text\":\"문제 문장\",\"options\":[\"선지1\",\"선지2\",\"선지3\",\"선지4\"],\"correctIndex\":정수,\"explanation\":\"해설\"}]} 입니다. " +
                    "모든 options 값은 명사형 또는 문장이여야 하며 서로 달라야 합니다. correctIndex는 0~3 범위여야 합니다. 요약문: " +
                    (req != null ? req.summary : "");

            Map<String, Object> content = new HashMap<>();
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", new Object[]{textPart});
            Map<String, Object> contents = new HashMap<>();
            contents.put("contents", new Object[]{parts});
            String body = mapper.writeValueAsString(contents);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey;
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("[AI-PROXY] /generate-quiz status=" + res.statusCode());
            String bodyStr = res.body();
            System.out.println("[AI-PROXY] /generate-quiz body-preview=" + (bodyStr != null ? bodyStr.substring(0, Math.min(bodyStr.length(), 500)) : "null"));
            if (res.statusCode() >= 300) {
                return ResponseEntity.status(500).body(jsonError("AI 호출 실패"));
            }
            JsonNode root = mapper.readTree(res.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                return ResponseEntity.status(500).body(jsonError("AI 응답 없음"));
            }
            JsonNode partsNode = candidates.get(0).path("content").path("parts");
            String text = null;
            if (partsNode.isArray() && partsNode.size() > 0) {
                JsonNode t = partsNode.get(0).path("text");
                if (t.isTextual()) text = t.asText();
            }
            if (text == null) {
                return ResponseEntity.status(500).body(jsonError("AI 텍스트 없음"));
            }
            String trimmed = text.trim();
            String jsonText = trimmed;
            if (!jsonText.startsWith("{")) {
                int start = jsonText.indexOf('{');
                int end = jsonText.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonText = jsonText.substring(start, end + 1).trim();
                }
            }
            if (!jsonText.startsWith("{")) {
                return ResponseEntity.status(500).body(jsonError("JSON 형식 아님"));
            }
            JsonNode json = mapper.readTree(jsonText);
            if (!json.has("questions") || !json.get("questions").isArray()) {
                return ResponseEntity.status(500).body(jsonError("필드 누락"));
            }
            return ResponseEntity.ok(mapper.writeValueAsString(json));
        } catch (Exception e) {
            System.out.println("[AI-PROXY] /generate-quiz exception=" + e.getMessage());
            return ResponseEntity.status(500).body(jsonError("AI 처리 오류"));
        }
    }

    @PostMapping("/summarize")
    public ResponseEntity<String> summarize(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @RequestBody SummarizeReq req) {
        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.status(500).body(jsonError("AI 설정이 없습니다"));
        }
        if (auth == null || !auth.startsWith("Bearer ") || !apiKey.equals(auth.substring(7))) {
            return ResponseEntity.status(401).body(jsonError("unauthorized"));
        }
        try {
            String prompt = "다음 텍스트를 한국어 뉴스 요약문으로 간결하게 정리하세요. " +
                    "반드시 순수 JSON만 반환하고, 코드블록이나 추가 텍스트는 금지합니다. " +
                    "출력 형식은 정확히 {\"summary\":\"요약문\"} 입니다. 텍스트: " +
                    (req != null ? req.content : "");

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", prompt);
            Map<String, Object> parts = new HashMap<>();
            parts.put("parts", new Object[]{textPart});
            Map<String, Object> contents = new HashMap<>();
            contents.put("contents", new Object[]{parts});
            String body = mapper.writeValueAsString(contents);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + apiKey;
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            System.out.println("[AI-PROXY] /summarize status=" + res.statusCode());
            String bodyStr = res.body();
            System.out.println("[AI-PROXY] /summarize body-preview=" + (bodyStr != null ? bodyStr.substring(0, Math.min(bodyStr.length(), 500)) : "null"));
            if (res.statusCode() >= 300) {
                return ResponseEntity.status(500).body(jsonError("AI 호출 실패"));
            }
            JsonNode root = mapper.readTree(res.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.size() == 0) {
                return ResponseEntity.status(500).body(jsonError("AI 응답 없음"));
            }
            JsonNode partsNode = candidates.get(0).path("content").path("parts");
            String text = null;
            if (partsNode.isArray() && partsNode.size() > 0) {
                JsonNode t = partsNode.get(0).path("text");
                if (t.isTextual()) text = t.asText();
            }
            if (text == null) {
                return ResponseEntity.status(500).body(jsonError("AI 텍스트 없음"));
            }
            String trimmed = text.trim();
            String jsonText = trimmed;
            if (!jsonText.startsWith("{")) {
                int start = jsonText.indexOf('{');
                int end = jsonText.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    jsonText = jsonText.substring(start, end + 1).trim();
                }
            }
            if (!jsonText.startsWith("{")) {
                return ResponseEntity.status(500).body(jsonError("JSON 형식 아님"));
            }
            JsonNode json = mapper.readTree(jsonText);
            if (!json.has("summary") || !json.get("summary").isTextual()) {
                return ResponseEntity.status(500).body(jsonError("필드 누락"));
            }
            return ResponseEntity.ok(mapper.writeValueAsString(json));
        } catch (Exception e) {
            System.out.println("[AI-PROXY] /summarize exception=" + e.getMessage());
            return ResponseEntity.status(500).body(jsonError("AI 처리 오류"));
        }
    }

    private String jsonError(String msg) {
        try {
            return mapper.writeValueAsString(Map.of("error", msg));
        } catch (Exception e) {
            return "{\"error\":\"" + msg + "\"}";
        }
    }
}
