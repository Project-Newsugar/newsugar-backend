package newsugar.Newsugar_Back.domain.ai.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class AiQuizClient {
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client;
    private final long timeoutMs;

    public static class QuestionData {
        public String text;
        public List<String> options;
        public Integer correctIndex;
        public String explanation;
    }

    public AiQuizClient() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        
        String u = System.getenv("QUIZ_AI_BASE_URL");
        if (u == null) u = env.get("QUIZ_AI_BASE_URL");
        this.baseUrl = u;

        String k = System.getenv("QUIZ_AI_API_KEY");
        if (k == null) k = env.get("QUIZ_AI_API_KEY");
        this.apiKey = k;

        long tm = 0L;
        try {
            String v = System.getenv("QUIZ_AI_TIMEOUT_MS");
            if (v == null) v = env.get("QUIZ_AI_TIMEOUT_MS");
            if (v != null && !v.isBlank()) tm = Long.parseLong(v.trim());
        } catch (Exception ignored) {}
        this.timeoutMs = tm > 0 ? tm : 0L;
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (timeoutMs > 0) builder = builder.connectTimeout(Duration.ofMillis(timeoutMs));
        this.client = builder.build();
    }

    public List<QuestionData> generate(String summaryText) {
        if (baseUrl == null || apiKey == null) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 설정이 없습니다");
        }
        try {
            String body = mapper.createObjectNode()
                    .put("summary", summaryText)
                    .toString();
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/generate-quiz"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (timeoutMs > 0) rb = rb.timeout(Duration.ofMillis(timeoutMs));
            HttpRequest req = rb.build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 300) {
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 호출 실패");
            }
            JsonNode root = mapper.readTree(res.body());
            JsonNode qs = root.get("questions");
            List<QuestionData> out = new ArrayList<>();
            if (qs != null && qs.isArray()) {
                for (JsonNode q : qs) {
                    if (q == null || q.isNull()) continue;

                    JsonNode textNode = q.get("text");
                    if (textNode == null || !textNode.isTextual()) continue;
                    String text = textNode.asText();
                    if (text == null || text.isBlank()) continue;

                    List<String> opts = new ArrayList<>();
                    JsonNode arr = q.get("options");
                    if (arr != null && arr.isArray()) {
                        for (JsonNode o : arr) {
                            if (o != null && o.isTextual()) {
                                String v = o.asText();
                                if (v != null && !v.isBlank()) {
                                    opts.add(v);
                                }
                            }
                        }
                    }
                    if (opts.size() < 2) continue;

                    JsonNode ciNode = q.get("correctIndex");
                    if (ciNode == null || !ciNode.isInt()) continue;
                    int idx = ciNode.asInt();
                    if (idx < 0 || idx >= opts.size()) continue;

                    QuestionData d = new QuestionData();
                    d.text = text;
                    d.options = opts;
                    d.correctIndex = idx;
                    d.explanation = q.has("explanation") && !q.get("explanation").isNull() && q.get("explanation").isTextual() ? q.get("explanation").asText() : null;
                    out.add(d);
                }
            }
            return out;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 처리 오류");
        }
    }

    public String summarize(String content) {
        if (baseUrl == null || apiKey == null) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 설정이 없습니다");
        }
        try {
            String body = mapper.createObjectNode()
                    .put("content", content)
                    .toString();
            HttpRequest.Builder rb = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/summarize"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            if (timeoutMs > 0) rb = rb.timeout(Duration.ofMillis(timeoutMs));
            HttpRequest req = rb.build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() >= 300) {
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 호출 실패");
            }
            JsonNode root = mapper.readTree(res.body());
            JsonNode summaryNode = root.get("summary");
            if (summaryNode == null || !summaryNode.isTextual()) {
                throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 처리 오류");
            }
            return summaryNode.asText();
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "AI 처리 오류");
        }
    }
}
