package newsugar.Newsugar_Back.domain.user.dto.response;

public record UserLoginResponseDTO (
    Long userId,
    String accessToken,
    String refreshToken

){}
