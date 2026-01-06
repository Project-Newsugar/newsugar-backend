package newsugar.Newsugar_Back.domain.user.dto;


public record JwtTokenDTO (
        String grantType,
        String accessToken,
        String refreshToken
){}