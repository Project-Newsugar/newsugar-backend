package newsugar.Newsugar_Back.domain.quiz.controller;

import newsugar.Newsugar_Back.common.ApiResult;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import newsugar.Newsugar_Back.domain.quiz.model.Question;
import newsugar.Newsugar_Back.domain.quiz.model.QuizSubmission;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizRepository;
import newsugar.Newsugar_Back.domain.quiz.repository.QuizSubmissionRepository;
import newsugar.Newsugar_Back.schedular.Schedular;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/dev/quizzes")
// @Profile("dev")
public class DevQuizController {

    private final Schedular schedular;
    private final QuizRepository quizRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    public DevQuizController(Schedular schedular, QuizRepository quizRepository, QuizSubmissionRepository quizSubmissionRepository) {
        this.schedular = schedular;
        this.quizRepository = quizRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<ApiResult<Map<String, Object>>> validateQuiz(@PathVariable Long id) {
        Quiz quiz = quizRepository.findById(id).orElse(null);
        if (quiz == null) {
            return ResponseEntity.ok(ApiResult.error("NOT_FOUND", "Quiz not found"));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", quiz.getId());
        result.put("title", quiz.getTitle());
        
        List<Map<String, Object>> questions = new ArrayList<>();
        if (quiz.getQuestions() != null) {
            int qIdx = 0;
            for (Question q : quiz.getQuestions()) {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("index", qIdx++);
                qMap.put("text", q.getText());
                qMap.put("correctIndex_0_based", q.getCorrectIndex());
                qMap.put("correctIndex_1_based_display", (q.getCorrectIndex() != null ? q.getCorrectIndex() + 1 : null));
                
                List<Map<String, Object>> options = new ArrayList<>();
                if (q.getOptions() != null) {
                    int oIdx = 0;
                    for (String opt : q.getOptions()) {
                        Map<String, Object> oMap = new HashMap<>();
                        oMap.put("index_0_based", oIdx);
                        oMap.put("index_1_based", oIdx + 1);
                        oMap.put("text", opt);
                        oMap.put("isCorrect", (q.getCorrectIndex() != null && q.getCorrectIndex() == oIdx));
                        options.add(oMap);
                        oIdx++;
                    }
                }
                qMap.put("options", options);
                questions.add(qMap);
            }
        }
        result.put("questions", questions);
        
        return ResponseEntity.ok(ApiResult.ok(result));
    }

    @PostMapping("/{id}/validate-score")
    public ResponseEntity<ApiResult<Map<String, Object>>> validateScore(@PathVariable Long id, @RequestBody List<Integer> answers) {
        Quiz quiz = quizRepository.findById(id).orElse(null);
        if (quiz == null) {
            return ResponseEntity.ok(ApiResult.error("NOT_FOUND", "Quiz not found"));
        }

        Map<String, Object> debugLog = new HashMap<>();
        List<String> logs = new ArrayList<>();
        
        List<Question> qs = quiz.getQuestions() != null ? quiz.getQuestions() : new ArrayList<>();
        int total = qs.size();
        int correct = 0;
        
        logs.add("Total Questions: " + total);
        logs.add("Received Answers (Raw from Frontend): " + answers);

        for (int i = 0; i < total; i++) {
            logs.add("--- Checking Question " + i + " ---");
            Integer rawAnswer = (answers != null && i < answers.size()) ? answers.get(i) : null;
            Integer answer = rawAnswer;
            
            logs.add("Raw Answer: " + rawAnswer);
            
            // Logic replication from QuizServiceImpl
            if (answer != null && answer > 0) {
                answer = answer - 1;
                logs.add("Converted 1-based to 0-based: " + answer);
            } else {
                logs.add("No valid conversion (answer is null or <= 0)");
            }

            Question q = qs.get(i);
            Integer expected = q.getCorrectIndex();
            int optionSize = q.getOptions() != null ? q.getOptions().size() : 0;
            
            logs.add("Expected Correct Index (0-based): " + expected);
            logs.add("Option Count: " + optionSize);
            
            if (q.getOptions() != null && expected != null && expected < q.getOptions().size()) {
                logs.add("Correct Option Text: " + q.getOptions().get(expected));
            }

            boolean inRange = (answer != null && answer >= 0 && answer < optionSize);
            boolean ok = (inRange && expected != null && answer.equals(expected));
            
            logs.add("Is In Range: " + inRange);
            logs.add("Is Correct: " + ok);
            
            if (ok) correct++;
        }
        
        debugLog.put("finalScore", correct);
        debugLog.put("total", total);
        debugLog.put("logs", logs);
        
        return ResponseEntity.ok(ApiResult.ok(debugLog));
    }

    @PostMapping("/today-main/generate")
    public ResponseEntity<ApiResult<Void>> generateTodayMainQuizDev() {
        schedular.generateTodayMainContent();
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @PostMapping("/category-summary/generate")
    public ResponseEntity<ApiResult<Void>> generateCategorySummaryDev() {
        // 비동기로 실행하여 API 타임아웃 방지
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                schedular.runDailyTask();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @DeleteMapping("/{quizId}")
    @Transactional
    public ResponseEntity<ApiResult<Void>> deleteQuizWithSubmissions(@PathVariable Long quizId) {
        List<QuizSubmission> submissions = quizSubmissionRepository.findByQuiz_Id(quizId);
        if (submissions != null && !submissions.isEmpty()) {
            quizSubmissionRepository.deleteAllInBatch(submissions);
        }
        if (quizRepository.existsById(quizId)) {
            quizRepository.deleteById(quizId);
        }
        return ResponseEntity.ok(ApiResult.ok(null));
    }

    @DeleteMapping("/submissions/all")
    @Transactional
    public ResponseEntity<ApiResult<Void>> deleteAllSubmissions() {
        quizSubmissionRepository.deleteAllInBatch();
        return ResponseEntity.ok(ApiResult.ok(null));
    }
}
