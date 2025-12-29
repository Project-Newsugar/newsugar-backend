package newsugar.Newsugar_Back.domain.quiz.controller;

import newsugar.Newsugar_Back.common.ApiResult;
import newsugar.Newsugar_Back.domain.quiz.dto.SubmitRequest;
import newsugar.Newsugar_Back.domain.quiz.dto.SubmitResult;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import newsugar.Newsugar_Back.domain.quiz.model.Question;
import newsugar.Newsugar_Back.domain.quiz.dto.QuizResponse;
import newsugar.Newsugar_Back.domain.quiz.dto.UserQuizStats;
import newsugar.Newsugar_Back.domain.quiz.service.QuizService;
import newsugar.Newsugar_Back.domain.news.service.NewsService;
import newsugar.Newsugar_Back.domain.news.service.RssNewsService;
import newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import newsugar.Newsugar_Back.domain.user.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quizzes")
@Validated
public class QuizController {
    private final QuizService quizService;
    private final JwtService jwtService;
    private final NewsService newsService;
    private final RssNewsService rssNewsService;
    private final AiQuizClient aiQuizClient;

    public QuizController(QuizService quizService, JwtService jwtService, NewsService newsService, AiQuizClient aiQuizClient, RssNewsService rssNewsService) {
        this.quizService = quizService;
        this.jwtService = jwtService;
        this.newsService = newsService;
        this.aiQuizClient = aiQuizClient;
        this.rssNewsService = rssNewsService;
    }

    

    @GetMapping
    public ResponseEntity<ApiResult<java.util.List<QuizResponse>>> list(
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "from", required = false) java.time.Instant from,
            @RequestParam(name = "to", required = false) java.time.Instant to
    ) {
        java.util.List<Quiz> quizzes;
        if ("period".equalsIgnoreCase(scope) && from != null && to != null) {
            quizzes = quizService.listByPeriod(from, to);
        } else {
            quizzes = quizService.listToday();
        }
        java.time.Instant now = java.time.Instant.now();
        java.util.List<QuizResponse> res = new java.util.ArrayList<>();
        for (Quiz q : quizzes) {
            boolean playable = (q.getStartAt() == null || !now.isBefore(q.getStartAt()))
                    && (q.getEndAt() == null || !now.isAfter(q.getEndAt()));
            res.add(toResponse(q, !playable));
        }
        return ResponseEntity.ok(ApiResult.ok(res));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<QuizResponse>> get(@PathVariable Long id) {
        Quiz quiz = quizService.get(id);
        java.time.Instant now = java.time.Instant.now();
        boolean playable = (quiz != null) && ((quiz.getStartAt() == null || !now.isBefore(quiz.getStartAt()))
                && (quiz.getEndAt() == null || !now.isAfter(quiz.getEndAt())));
        QuizResponse res = toResponse(quiz, !playable);
        return ResponseEntity.ok(ApiResult.ok(res));
    }

    @PostMapping("/summary/{summaryId}/generate")
    public ResponseEntity<ApiResult<QuizResponse>> generateFromSummary(
            @PathVariable Long summaryId,
            @RequestHeader("Authorization") String token
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        jwtService.getUserIdFromToken(actualToken);
        Quiz quiz = quizService.generateFromSummary(summaryId);
        QuizResponse res = toResponse(quiz, false);
        return ResponseEntity.ok(ApiResult.ok(res));
    }


    @PostMapping("/today-main/generate")
    public ResponseEntity<ApiResult<QuizResponse>> generateTodayMainQuiz(
            @RequestHeader("Authorization") String token
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        jwtService.getUserIdFromToken(actualToken);
        java.time.Instant now = java.time.Instant.now();
        java.time.Instant from = now.minus(java.time.Duration.ofHours(6));
        java.util.List<Quiz> quizzes = quizService.listByPeriod(from, now.plus(java.time.Duration.ofHours(6)));
        for (Quiz q : quizzes) {
            if ("오늘의 주요뉴스 퀴즈".equals(q.getTitle())) {
                QuizResponse res = toResponse(q, false);
                return ResponseEntity.ok(ApiResult.ok(res));
            }
        }

        // 기존 스케줄러에서 생성된 퀴즈가 없다면 빈 응답 처리
        return ResponseEntity.ok(ApiResult.ok(null));
    }


    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResult<SubmitResult>> submit(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestBody SubmitRequest req
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);
        SubmitResult result = quizService.score(
                id,
                userId,
                req != null ? req.answers() : null
        );
        return ResponseEntity.ok(ApiResult.ok(result));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResult<SubmitResult>> result(@PathVariable Long id) {
        SubmitResult last = quizService.lastResult(id);
        return ResponseEntity.ok(ApiResult.ok(last));
    }

    @GetMapping("/{id}/answers")
    public ResponseEntity<ApiResult<QuizResponse>> answers(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);
        boolean has = quizService.hasSubmission(id, userId);
        if (!has) {
            return ResponseEntity.status(403).body(ApiResult.error(newsugar.Newsugar_Back.common.ErrorCode.FORBIDDEN.name(), "제출 이력이 없습니다"));
        }
        Quiz quiz = quizService.get(id);
        QuizResponse res = toResponse(quiz, true);
        return ResponseEntity.ok(ApiResult.ok(res));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResult<UserQuizStats>> stats(
            @RequestHeader("Authorization") String token
    ) {
        String actualToken = token != null ? token.replace("Bearer ", "") : null;
        Long userId = jwtService.getUserIdFromToken(actualToken);
        UserQuizStats stats = quizService.statsForUser(userId);
        return ResponseEntity.ok(ApiResult.ok(stats));
    }

    private QuizResponse toResponse(Quiz quiz, boolean includeAnswers) {
        if (quiz == null) return null;
        java.util.List<QuizResponse.QuestionView> views = new java.util.ArrayList<>();
        if (quiz.getQuestions() != null) {
            for (Question q : quiz.getQuestions()) {
                views.add(new QuizResponse.QuestionView(
                        q.getText(),
                        q.getOptions(),
                        includeAnswers ? q.getCorrectIndex() : null,
                        includeAnswers ? q.getExplanation() : null
                ));
            }
        }
        return new QuizResponse(quiz.getId(), quiz.getTitle(), views, quiz.getStartAt(), quiz.getEndAt());
    }
}
