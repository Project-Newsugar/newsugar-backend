package newsugar.Newsugar_Back.domain.quiz.dto;

import java.time.Instant;
import java.util.List;

public record QuizResponse(Long id, String title, List<QuestionView> questions, Instant startAt, Instant endAt) {
    public static record QuestionView(String text, List<String> options, Integer correctIndex, String explanation) {}
}
