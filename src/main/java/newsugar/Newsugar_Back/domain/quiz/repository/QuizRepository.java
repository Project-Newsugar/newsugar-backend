package newsugar.Newsugar_Back.domain.quiz.repository;

import newsugar.Newsugar_Back.domain.quiz.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}

