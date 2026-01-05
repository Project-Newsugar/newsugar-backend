package newsugar.Newsugar_Back.domain.summary.service;

import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.ai.GeminiService;
import newsugar.Newsugar_Back.domain.news.controller.NewsApiController;
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
        // DeepSearch API에서 뉴스 5개 가져오기 (최근 3일 데이터로 제한)
        LocalDate dateFrom = LocalDate.now();
        DeepSearchResponseDTO response =
                newsService.getNewsByCategory(List.of(category), dateFrom, 1, 5);



        // 뉴스 summary 추출
        List<String> summaries = response.data()
                .stream()
                .map(ArticleDTO::summary)
                .toList();

        // Gemmini로 요약
        String categorySummary = geminiService.summarize(category,summaries);

        // 캐시에 저장
        cache.put(category, categorySummary);
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
        String key = "category_summary:" + category;
        categorySummaryRedis.saveSummary(key, summary);
    }

}
