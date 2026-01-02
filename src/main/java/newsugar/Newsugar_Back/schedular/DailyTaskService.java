package newsugar.Newsugar_Back.schedular;

import newsugar.Newsugar_Back.domain.ai.GeminiService;
import newsugar.Newsugar_Back.domain.ai.clients.AiQuizClient;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.ArticleDTO;
import newsugar.Newsugar_Back.domain.news.dto.deepservicedto.DeepSearchResponseDTO;
import newsugar.Newsugar_Back.domain.news.service.NewsService;
import newsugar.Newsugar_Back.domain.news.service.RssNewsService;
import newsugar.Newsugar_Back.domain.quiz.model.Question;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import newsugar.Newsugar_Back.domain.quiz.service.QuizService;
import newsugar.Newsugar_Back.domain.summary.model.Summary;
import newsugar.Newsugar_Back.domain.summary.repository.CategorySummaryRedis;
import newsugar.Newsugar_Back.domain.summary.repository.SummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class DailyTaskService {

    private final NewsService newsService;
    private final RssNewsService rssNewsService;
    private final CategorySummaryRedis categorySummaryRedis;
    private final GeminiService geminiService;
    private final SummaryRepository summaryRepository;
    private final AiQuizClient aiQuizClient;
    private final QuizService quizService;

    public DailyTaskService(NewsService newsService,
                            RssNewsService rssNewsService,
                            CategorySummaryRedis categorySummaryRedis,
                            GeminiService geminiService,
                            SummaryRepository summaryRepository,
                            AiQuizClient aiQuizClient,
                            QuizService quizService) {
        this.newsService = newsService;
        this.rssNewsService = rssNewsService;
        this.categorySummaryRedis = categorySummaryRedis;
        this.geminiService = geminiService;
        this.summaryRepository = summaryRepository;
        this.aiQuizClient = aiQuizClient;
        this.quizService = quizService;
    }

    // @Transactional // 트랜잭션 롤백 문제 방지를 위해 주석 처리 (개별 저장 로직에서 처리됨)
    public void executeDailyRoutine() {
        // 중복 실행 방지 (이미 오늘 날짜의 요약이 있으면 스킵하거나 덮어쓰기 정책 결정 필요)
        // 현재는 매시간 실행되므로 중복 생성이 발생할 수 있음.
        // 하지만 MainNewsController에서 최신 요약 하나만 가져다 쓰므로 큰 문제는 아님.
        // 다만 DB 데이터가 쌓이는 것을 방지하려면 여기서 체크 로직 추가 가능.

        // 뉴스 가져오기 및 요약 생성
        Summary summary = generateSummary();

        // 생성된 요약을 바탕으로 퀴즈 생성
        if (summary != null) {
            generateQuiz(summary);
        }
    }

    private Summary generateSummary() {
        DeepSearchResponseDTO news;
        try {
            // 뉴스 조회 시 정렬 파라미터(sort=date) 추가는 NewsService 내부 로직 개선 필요
            news = newsService.getNewsByCategory(null, LocalDate.now().minusDays(1), 1, 5);
            boolean empty = (news == null) || (news.data() == null) || news.data().isEmpty();
            if (empty) {
                news = rssNewsService.getTopHeadlines(1, 5);
            }
        } catch (Exception e) {
            news = rssNewsService.getTopHeadlines(1, 5);
        }

        List<ArticleDTO> items = (news != null && news.data() != null) ? news.data() : List.of();
        List<String> summaries = new ArrayList<>();
        for (int i = 0; i < Math.min(5, items.size()); i++) {
            String s = items.get(i).summary();
            if (s != null) {
                s = s.replaceAll("<[^>]*>", " ").replaceAll("&[^;]+;", " ").trim();
                if (!s.isBlank()) summaries.add(s);
            }
        }

        if (summaries.isEmpty()) {
            return null;
        }

        // Gemini API 호출 (최대 3회 재시도 및 실패 시 단순 연결로 대체)
        String todaySummary;
        try {
            todaySummary = geminiService.summarize("오늘 주요", summaries);
        } catch (Exception e) {
            System.err.println("AI 요약 실패 (API Quota 등): " + e.getMessage());
            // Fallback: AI 실패 시 수집된 뉴스 요약들을 단순 연결하여 사용
            todaySummary = String.join("\n", summaries);
            // Fallback 데이터도 너무 길면 자름
            if (todaySummary.length() > 2000) {
                todaySummary = todaySummary.substring(0, 2000) + "...";
            }
        }

        // DB 컬럼 용량(TEXT) 초과 방지를 위한 안전장치 (최대 3000자)
        if (todaySummary != null && todaySummary.length() > 3000) {
            System.out.println("요약문 길이가 너무 길어서 자릅니다. 원래 길이: " + todaySummary.length());
            todaySummary = todaySummary.substring(0, 3000) + "...[내용 중략]";
        }

        Summary summary = Summary.builder()
                .summaryText(todaySummary)
                .build();
        
        try {
            return summaryRepository.save(summary);
        } catch (Exception e) {
            System.err.println("요약문 저장 실패 (DB 컬럼 용량 초과 등): " + e.getMessage());
            // 저장은 실패했지만 퀴즈 생성은 계속 진행하기 위해 객체 반환
            return summary;
        }
    }

    private void generateQuiz(Summary baseSummary) {
        if (baseSummary == null || baseSummary.getSummaryText() == null || baseSummary.getSummaryText().isBlank()) {
            return;
        }

        // 이미 퀴즈가 너무 많이 생성되었는지 체크 (선택 사항)
        // List<Quiz> todays = quizService.listToday();
        // if (todays.size() > 5) return; 

        // API 쿼터 제한을 피하기 위해 요약 생성 후 잠시 대기
        try {
            Thread.sleep(10000); // 10초 대기
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String aggregated = baseSummary.getSummaryText();

        List<AiQuizClient.QuestionData> gen;
        try {
            // 외부 AI 서비스 대신 GeminiService 직접 사용
            gen = geminiService.generateQuiz(aggregated);
        } catch (Exception ex) {
            System.err.println("Quiz generation failed: " + ex.getMessage());
            gen = List.of();
        }

        if (gen == null || gen.isEmpty()) {
            String src = (aggregated != null && !aggregated.isBlank()) ? aggregated : null;
            if (src == null) src = "";
            String base = src.replaceAll("[^가-힣A-Za-z0-9 ]", " ").trim();
            String[] toks = base.split("\\s+");
            LinkedHashSet<String> uniq = new LinkedHashSet<>();
            for (String t : toks) {
                if (t != null && t.length() >= 2) {
                    uniq.add(t);
                    if (uniq.size() >= 4) break;
                }
            }
            List<String> opts = new ArrayList<>(uniq);
            if (opts.size() < 2) {
                opts = List.of("예", "아니오");
            }
            AiQuizClient.QuestionData fd = new AiQuizClient.QuestionData();
            fd.text = "요약의 핵심 키워드로 가장 적합한 것은 무엇인가요?";
            fd.options = opts;
            fd.correctIndex = 0;
            fd.explanation = null;
            gen = List.of(fd);
        }

        List<Question> questions = new ArrayList<>();
        if (gen != null && !gen.isEmpty()) {
            AiQuizClient.QuestionData d = gen.get(0);
            Question q = new Question();
            q.setText(d.text);
            q.setOptions(d.options != null ? d.options : List.of());
            q.setCorrectIndex(d.correctIndex);
            q.setExplanation(d.explanation);
            questions.add(q);
        }

        Quiz quiz = new Quiz();
        quiz.setTitle("오늘의 주요뉴스 퀴즈");
        quiz.setQuestions(questions);
        Instant now = Instant.now();
        quiz.setStartAt(now);
        quiz.setEndAt(now.plus(Duration.ofHours(6)));

        try {
            quizService.create(quiz);
        } catch (Exception e) {
            System.err.println("퀴즈 저장 실패: " + e.getMessage());
        }
    }
}
