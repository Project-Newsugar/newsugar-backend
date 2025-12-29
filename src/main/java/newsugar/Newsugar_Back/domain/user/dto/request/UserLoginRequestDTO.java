package newsugar.Newsugar_Back.domain.user.dto.request;

public record UserLoginRequestDTO(
        String email,
        String password
){}