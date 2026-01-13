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
import newsugar.Newsugar_Back.schedular.Schedular;
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

    // QuizService, NewsService, AiQuizClient, Schedular 의존성 주입
    private final QuizService quizService;
    private final JwtService jwtService;
    private final NewsService newsService;
    private final RssNewsService rssNewsService;
    private final AiQuizClient aiQuizClient;
    private final Schedular schedular;

    public QuizController(QuizService quizService, JwtService jwtService, NewsService newsService, AiQuizClient aiQuizClient, RssNewsService rssNewsService, Schedular schedular) {
        this.quizService = quizService;
        this.jwtService = jwtService;
        this.newsService = newsService;
        this.aiQuizClient = aiQuizClient;
        this.rssNewsService = rssNewsService;
        this.schedular = schedular;
    }

    // 퀴즈 목록 조회 API (기간별/일별 조회 지원)
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

    // 단일 퀴즈 상세 조회 API
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<QuizResponse>> get(@PathVariable Long id) {
        Quiz quiz = quizService.get(id);
        java.time.Instant now = java.time.Instant.now();
        boolean playable = (quiz != null) && ((quiz.getStartAt() == null || !now.isBefore(quiz.getStartAt()))
                && (quiz.getEndAt() == null || !now.isAfter(quiz.getEndAt())));
        QuizResponse res = toResponse(quiz, !playable);
        return ResponseEntity.ok(ApiResult.ok(res));
    }

    // 요약문 기반 퀴즈 생성 API
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


    // 메인 화면용 오늘의 퀴즈 생성 API (최근 생성 이력 활용)
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

        // 오늘의 주요뉴스 퀴즈가 없으면 스케줄러를 강제로 돌려서라도 만들어냅니다.
        // 이게 없으면 프론트에서 빈 화면만 보고 있을 테니까요.
        System.out.println("오늘의 주요뉴스 퀴즈가 없어 즉시 생성을 시도합니다.");
        try {
            schedular.generateTodayMainContent(true);
            
            // 생성 후 다시 조회 (혹시나 생성되었는지 확인)
            quizzes = quizService.listByPeriod(from, now.plus(java.time.Duration.ofHours(6)));
            for (Quiz q : quizzes) {
                if ("오늘의 주요뉴스 퀴즈".equals(q.getTitle())) {
                    QuizResponse res = toResponse(q, false);
                    return ResponseEntity.ok(ApiResult.ok(res));
                }
            }
        } catch (Exception e) {
            System.err.println("퀴즈 강제 생성 중 오류 발생: " + e.getMessage());
            // 오류가 나도 일단 null 반환 (클라이언트에서 처리하도록)
        }

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
    public ResponseEntity<ApiResult<SubmitResult>> result(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        Long userId = null;
        if (token != null) {
            String actualToken = token.replace("Bearer ", "");
            try {
                userId = jwtService.getUserIdFromToken(actualToken);
            } catch (Exception e) {
                // 토큰이 유효하지 않거나 없으면 userId = null
            }
        }
        
        SubmitResult last = quizService.lastResult(id, userId);
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
