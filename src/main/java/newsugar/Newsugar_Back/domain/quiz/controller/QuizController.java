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

    // 퀴즈 서비스랑 뉴스 서비스들 엮여있습니다. AI 클라이언트도 여기서 부릅니다.
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

    // 퀴즈 목록 조회하는 API입니다.
    // scope=period로 기간 주면 그 사이꺼 다 주고, 아니면 그냥 오늘꺼 줍니다.
    // playable 플래그 계산해서 같이 내려주니까 프론트에서 시간 계산하지 마세요.
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

    // 퀴즈 하나만 딱 집어서 가져옵니다.
    // 없는 ID 달라고 하면 404 뱉거나 null 줄 겁니다.
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<QuizResponse>> get(@PathVariable Long id) {
        Quiz quiz = quizService.get(id);
        java.time.Instant now = java.time.Instant.now();
        boolean playable = (quiz != null) && ((quiz.getStartAt() == null || !now.isBefore(quiz.getStartAt()))
                && (quiz.getEndAt() == null || !now.isAfter(quiz.getEndAt())));
        QuizResponse res = toResponse(quiz, !playable);
        return ResponseEntity.ok(ApiResult.ok(res));
    }

    // 특정 요약문 ID 가지고 퀴즈 생성 요청하는 겁니다.
    // AI가 문제 만드느라 시간 좀 걸리니까 로딩바 돌리세요.
    // 토큰 검사해서 유저 정보도 같이 넘깁니다.
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


    // 메인 화면에 띄울 오늘의 퀴즈 생성합니다.
    // 이미 만들어둔 거 있으면 그거 주고, 없으면 새로 만듭니다.
    // 최근 6시간 내에 만든 거 찾아서 재탕합니다. AI 비용 아껴야죠.
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
