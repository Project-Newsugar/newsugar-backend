package newsugar.Newsugar_Back.domain.quiz.repository;

import newsugar.Newsugar_Back.domain.quiz.model.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {
    Optional<QuizSubmission> findTopByQuiz_IdOrderByCreatedAtDesc(Long quizId);
    Optional<QuizSubmission> findTopByQuiz_IdAndUserIdOrderByCreatedAtDesc(Long quizId, Long userId);
    List<QuizSubmission> findByUserId(Long userId);
    List<QuizSubmission> findByQuiz_Id(Long quizId);
}
