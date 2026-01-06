package newsugar.Newsugar_Back.domain.summary.service;

import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.ai.GeminiService;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import newsugar.Newsugar_Back.domain.news.service.NewsService;
import newsugar.Newsugar_Back.domain.summary.repository.CategorySummaryRedis;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CategorySummaryService {

    private final NewsService newsService;
    private final GeminiService geminiService;
    private final CategorySummaryRedis  categorySummaryRedis;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public CategorySummaryService(NewsService newsService, GeminiService geminiService, CategorySummaryRedis categorySummaryRedis) {
        this.newsService = newsService;
        this.geminiService = geminiService;
        this.categorySummaryRedis = categorySummaryRedis;
    }

    public String generateCategorySummary(String category) {
        // DeepSearch API에서 뉴스 5개 가져오기 (최근 1일 데이터로 제한하여 최신성 보장)
        DeepSearchResponseDTO response = newsService.getNewsByCategory(
                List.of(category), // 단일 카테고리
                LocalDate.now(java.time.ZoneId.of("Asia/Seoul")).minusDays(1), // 최근 1일(어제~오늘)
                1,                // 첫 페이지
                5                 // 5개만
        );

        if (response == null || response.data() == null || response.data().isEmpty()) {
            return "최근 24시간 내 해당 카테고리의 주요 뉴스가 충분하지 않아 요약을 생성할 수 없습니다.";
        }

        // 뉴스 summary 추출
        List<String> summaries = response.data()
                .stream()
                .map(ArticleDTO::summary)
                .toList();
        
        if (summaries.isEmpty()) {
             return "최근 24시간 내 해당 카테고리의 주요 뉴스가 충분하지 않아 요약을 생성할 수 없습니다.";
        }

        // Gemmini로 요약
        String categorySummary = geminiService.summarize(category,summaries);

        // 캐시에 저장 (메모리 캐시 및 Redis 캐시 동시 갱신 권장)
        cache.put(category, categorySummary);
        saveInRedis(category, categorySummary); // Redis에도 즉시 저장하여 일관성 유지
        
        return categorySummary;
    }

    public String getSummary(String key){
       String summary = categorySummaryRedis.getSummary(key);

       if(summary == null){
           throw new CustomException(ErrorCode.NOT_FOUND, "생성된 카테고리 요약이 없습니다.");
       }

       return summary;
    }

    public void saveInRedis(String category, String summary) {
        String key = "category_summary:v2:" + category;
        categorySummaryRedis.saveSummary(key, summary);
    }

}
