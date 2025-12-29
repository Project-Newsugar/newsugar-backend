package newsugar.Newsugar_Back.domain.score.repository;

import newsugar.Newsugar_Back.domain.score.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScoreRepository extends JpaRepository<Score, Long> {
    Optional<Score> findByUserId(Long userId);
}
