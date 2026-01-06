package newsugar.Newsugar_Back.domain.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class GeminiResponseDTO {

    private List<Candidate> candidates;

    public String getText() {
        if (candidates == null || candidates.isEmpty()) return null;
        return candidates.get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();
    }

    @Getter
    static class Candidate {
        private Content content;
    }

    @Getter
    static class Content {
        private List<Part> parts;
    }

    @Getter
    static class Part {
        private String text;
    }
}