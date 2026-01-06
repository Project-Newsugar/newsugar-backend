package newsugar.Newsugar_Back.domain.summary.repository;

import newsugar.Newsugar_Back.domain.summary.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SummaryRepository extends JpaRepository<Summary, Long> {
}

