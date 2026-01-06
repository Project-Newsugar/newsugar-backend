package newsugar.Newsugar_Back.domain.user.dto.response;

public record UserCategoryResponseDTO(
        Long id,
        Long userId,
        Long categoryId,
        String name

) {}
