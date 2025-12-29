package newsugar.Newsugar_Back.domain.news.controller;

import newsugar.Newsugar_Back.common.ApiResult;
import newsugar.Newsugar_Back.domain.ai.GeminiService;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import newsugar.Newsugar_Back.domain.news.service.NewsService;
import newsugar.Newsugar_Back.domain.news.service.RssNewsService;
import newsugar.Newsugar_Back.domain.summary.model.Summary;
import newsugar.Newsugar_Back.domain.summary.repository.CategorySummaryRedis;
import newsugar.Newsugar_Back.domain.summary.repository.SummaryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/v1/news")
public class MainNewsController {

    private final RssNewsService rssNewsService;
    private final NewsService newsService;
    private final GeminiService geminiService;
    private final CategorySummaryRedis categorySummaryRedis;
    private final SummaryRepository summaryRepository;

    public MainNewsController(RssNewsService rssNewsService,
                              NewsService newsService,
                              GeminiService geminiService,
                              CategorySummaryRedis categorySummaryRedis,
                              SummaryRepository summaryRepository) {
        this.rssNewsService = rssNewsService;
        this.newsService = newsService;
        this.geminiService = geminiService;
        this.categorySummaryRedis = categorySummaryRedis;
        this.summaryRepository = summaryRepository;
    }

    @GetMapping("/today-summary")
    public ResponseEntity<ApiResult<DeepSearchResponseDTO>> getTodayMainNewsSummary(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer page_size
    ) {
        DeepSearchResponseDTO response;
        try {
            response = newsService.getNewsByCategory(null, page, page_size);
            boolean empty = (response == null) || (response.data() == null) || response.data().isEmpty();
            if (empty) {
                response = rssNewsService.getTopHeadlines(page, page_size);
            }
        } catch (Exception e) {
            response = rssNewsService.getTopHeadlines(page, page_size);
        }
        return ResponseEntity.ok(ApiResult.ok(response));
    }

    @GetMapping("/today-main-summary")
    public ResponseEntity<ApiResult<String>> getTodayMainSummaryText(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer page_size
    ) {

        java.util.Optional<Summary> latest = summaryRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst();
        if (latest.isPresent()) {
            String txt = latest.get().getSummaryText();
            if (txt != null && !txt.isBlank()) {
                return ResponseEntity.ok(ApiResult.ok(txt));
            }
        }
        DeepSearchResponseDTO response;
        try {
            response = newsService.getNewsByCategory(null, page, page_size);
            boolean empty = (response == null) || (response.data() == null) || response.data().isEmpty();
            if (empty) {
                response = rssNewsService.getTopHeadlines(page, page_size);
            }
        } catch (Exception e) {
            response = rssNewsService.getTopHeadlines(page, page_size);
        }
        List<ArticleDTO> articles = (response != null && response.data() != null) ? response.data() : List.of();
        List<String> summaries = articles.stream()
                .map(ArticleDTO::summary)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.replaceAll("<[^>]*>", " ").replaceAll("&[^;]+;", " ").trim())
                .filter(s -> !s.isBlank())
                .toList();
        if (summaries.isEmpty()) {
            return ResponseEntity.ok(ApiResult.ok(""));
        }
        String todaySummary = geminiService.summarize("오늘 주요", summaries);
        return ResponseEntity.ok(ApiResult.ok(todaySummary));
    }

    @GetMapping("/today-main-summary-by-time")
    public ResponseEntity<ApiResult<String>> getTodayMainSummaryByTime(
            @RequestParam Integer hour
    ) {
        int target = hour != null ? hour : 0;
        if (target == 24) target = 0;
        if (target != 0 && target != 6 && target != 12 && target != 18) {
            java.util.Optional<Summary> latest = summaryRepository.findAll().stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .findFirst();
            if (latest.isPresent()) {
                String txt = latest.get().getSummaryText();
                if (txt != null && !txt.isBlank()) {
                    return ResponseEntity.ok(ApiResult.ok(txt));
                }
            }
            return ResponseEntity.ok(ApiResult.ok(""));
        }

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDateTime start = today.atTime(target, 0);
        Instant from = start.atZone(zone).toInstant();
        Instant to = start.plusHours(6).atZone(zone).toInstant();

        java.util.Optional<Summary> slot = summaryRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(from) && s.getCreatedAt().isBefore(to))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst();

        if (slot.isPresent()) {
            String txt = slot.get().getSummaryText();
            if (txt != null && !txt.isBlank()) {
                return ResponseEntity.ok(ApiResult.ok(txt));
            }
        }

        java.util.Optional<Summary> latest = summaryRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .findFirst();
        if (latest.isPresent()) {
            String txt = latest.get().getSummaryText();
            if (txt != null && !txt.isBlank()) {
                return ResponseEntity.ok(ApiResult.ok(txt));
            }
        }

        return ResponseEntity.ok(ApiResult.ok(""));
    }
}
