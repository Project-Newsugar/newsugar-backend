package newsugar.Newsugar_Back.domain.ai.clients;

import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.domain.ai.dto.GeminiRequestDTO;
import newsugar.Newsugar_Back.domain.ai.dto.GeminiResponseDTO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiClient {

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public GeminiClient() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("QUIZ_AI_API_KEY");
        if (k == null) k = env.get("QUIZ_AI_API_KEY");
        this.apiKey = k;
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new RuntimeException("Gemini API Key가 설정되지 않았습니다.");
        }
    }

    public String generateContent(String prompt) {
        // User explicitly requested Gemma 3 27B IT model.
        // Based on 2026-01-09 context, this model is available.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemma-3-27b-it:generateContent?key=" + apiKey;
        GeminiRequestDTO request = GeminiRequestDTO.of(prompt);

        int maxRetries = 3; // 개발 테스트를 위해 재시도 횟수 감소
        int retryCount = 0;
        long waitTime = 5000; // 5초로 시작

        while (retryCount < maxRetries) {
            try {
                GeminiResponseDTO response = restTemplate.postForObject(url, request, GeminiResponseDTO.class);
                if (response != null && response.getText() != null) {
                    return response.getText();
                }
            } catch (Exception e) {
                if (e.getMessage().contains("429")) {
                    System.out.println("Gemini API Quota Exceeded. Retrying in " + (waitTime / 1000) + "s...");
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry wait", ie);
                    }
                    retryCount++;
                    waitTime *= 2; // 지수 백오프
                    continue;
                }
                // 429 에러가 아니면 재시도하지 않고 바로 예외를 던져서 Fallback 로직이 동작하게 함
                throw e; 
            }
        }
        System.out.println("Gemini API 호출 실패: 재시도 횟수 초과 (429 Too Many Requests) - null 반환하고 넘어갑니다.");
        return null;
        // throw new RuntimeException("Gemini API 호출 실패: 재시도 횟수 초과 (429 Too Many Requests)");
    }
}
