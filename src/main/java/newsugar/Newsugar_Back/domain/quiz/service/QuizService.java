package newsugar.Newsugar_Back.domain.quiz.service;

import newsugar.Newsugar_Back.domain.quiz.dto.SubmitResult;
import newsugar.Newsugar_Back.domain.quiz.dto.UserQuizStats;
import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import java.util.List;
import java.time.Instant;

public interface QuizService {
    // 퀴즈 DB에 박습니다.
    Quiz create(Quiz quiz);

    // ID로 퀴즈 하나 가져옵니다.
    Quiz get(Long id);

    // 퀴즈 채점합니다. 정답 확인하고 점수 계산해서 저장합니다.
    SubmitResult score(Long id, Long userId, List<Integer> answers);
    SubmitResult score(Long id, List<Integer> answers);

    // 오늘 퀴즈 목록 가져옵니다.
    List<Quiz> listToday();

    // 기간별 퀴즈 목록 가져옵니다.
    List<Quiz> listByPeriod(Instant from, Instant to);

    // 유저가 마지막으로 푼 결과 가져옵니다.
    SubmitResult lastResult(Long quizId, Long userId);

    // 풀 수 있는 퀴즈인지 확인합니다. 기간 지났으면 에러 뱉습니다.
    void ensurePlayable(Long id);

    // 결과 없으면 에러 던집니다.
    SubmitResult resultOrThrow(Long quizId);

    // 요약문 기반으로 퀴즈 생성합니다. AI 씁니다.
    Quiz generateFromSummary(Long summaryId);

    // 유저 통계 냅니다.
    UserQuizStats statsForUser(Long userId);

    // 제출 이력 있는지 확인합니다.
    boolean hasSubmission(Long quizId, Long userId);
}
