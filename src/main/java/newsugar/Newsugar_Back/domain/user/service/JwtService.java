package newsugar.Newsugar_Back.domain.user.service;

import jakarta.transaction.Transactional;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.user.utils.JwtUtil;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class JwtService {
    private final JwtUtil jwtUtil;

    public JwtService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    public Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Authorization 헤더가 존재하지 않습니다.");
        }

        try {
            return jwtUtil.validateToken(token);
        } catch (io.jsonwebtoken.JwtException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }

    public String refreshToken (String token){
        if (token == null || token.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "Authorization 헤더가 존재하지 않습니다.");
        }

        try {
            Long userId = jwtUtil.validateRefresh(token);
            return jwtUtil.generateToken(userId);
        } catch (io.jsonwebtoken.JwtException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 토큰입니다.");
        }
    }


}
