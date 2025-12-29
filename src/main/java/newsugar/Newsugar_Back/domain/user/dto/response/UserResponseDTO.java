package newsugar.Newsugar_Back.domain.user.dto.response;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String nickname,
        String phone
) {}
