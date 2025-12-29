package newsugar.Newsugar_Back.domain.user.dto.response;

import java.util.List;

public record UserPreferCategoryResponseDTO(
        Long id,
        Long userId,
        List<Long> categoryIdList
) {}
