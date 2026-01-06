package newsugar.Newsugar_Back.domain.quiz.dto;

import java.util.List;

public record SubmitRequest(Long userId, List<Integer> answers) {}
