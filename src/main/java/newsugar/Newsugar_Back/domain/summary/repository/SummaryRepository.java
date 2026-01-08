package newsugar.Newsugar_Back.domain.summary.repository;

import newsugar.Newsugar_Back.domain.summary.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
    Optional<Summary> findTopByOrderByCreatedAtDesc();
}

