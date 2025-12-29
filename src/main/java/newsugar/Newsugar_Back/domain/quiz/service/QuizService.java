package newsugar.Newsugar_Back.domain.quiz.service;

import newsugar.Newsugar_Back.domain.quiz.dto.SubmitResult;
import newsugar.Newsugar_Back.domain.quiz.dto.UserQuizStats;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import java.util.List;
import java.time.Instant;

public interface QuizService {
    Quiz create(Quiz quiz);
    Quiz get(Long id);
    SubmitResult score(Long id, Long userId, List<Integer> answers);
    SubmitResult score(Long id, List<Integer> answers);
    List<Quiz> listToday();
    List<Quiz> listByPeriod(Instant from, Instant to);
    SubmitResult lastResult(Long quizId, Long userId);
    void ensurePlayable(Long id);
    SubmitResult resultOrThrow(Long quizId);
    Quiz generateFromSummary(Long summaryId);
    UserQuizStats statsForUser(Long userId);
    boolean hasSubmission(Long quizId, Long userId);
}
