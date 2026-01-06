package newsugar.Newsugar_Back.domain.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiRequestDTO {

    private List<Content> contents;

    public static GeminiRequestDTO of(String prompt) {
        return new GeminiRequestDTO(
                List.of(
                        new Content(
                                List.of(new Part(prompt))
                        )
                )
        );
    }

    @Getter
    @AllArgsConstructor
    static class Content {
        private List<Part> parts;
    }

    @Getter
    @AllArgsConstructor
    static class Part {
        private String text;
    }
}