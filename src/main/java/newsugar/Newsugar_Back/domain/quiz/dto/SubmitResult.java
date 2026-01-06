package newsugar.Newsugar_Back.domain.quiz.dto;

import java.util.List;

public record SubmitResult(int total, int correct, List<Boolean> results, Long userId, java.time.Instant submittedAt) {}
