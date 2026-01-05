package newsugar.Newsugar_Back.domain.user.service;

import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.score.service.ScoreService;
import newsugar.Newsugar_Back.domain.user.dto.GoogleUserInfoDTO;
import newsugar.Newsugar_Back.domain.user.dto.response.UserLoginResponseDTO;
import newsugar.Newsugar_Back.domain.user.model.User;
import newsugar.Newsugar_Back.domain.user.repository.UserRepository;
import newsugar.Newsugar_Back.domain.user.utils.JwtUtil;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;



@Service
public class GoogleService {
    private final UserRepository userRepository;
    private final ScoreService scoreService;
    private final JwtUtil jwtUtil;

    private final String GOOGLE_CLIENT_ID;
    private final String GOOGLE_CLIENT_SECRET;

    public GoogleService(UserRepository userRepository, JwtUtil jwtUtil, ScoreService scoreService){
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.scoreService = scoreService;

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String googleId = System.getenv("GOOGLE_CLIENT_ID");
        if(googleId == null) googleId = dotenv.get("GOOGLE_CLIENT_ID");
        this.GOOGLE_CLIENT_ID = googleId;

        String googleSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        if(googleSecret == null) googleSecret = dotenv.get("GOOGLE_CLIENT_SECRET");
        this.GOOGLE_CLIENT_SECRET = googleSecret;
    }

    public UserLoginResponseDTO googleLogin(String accessToken) {

        GoogleUserInfoDTO googleUser = requestGoogleUserInfo(accessToken);

        User user = userRepository.findByEmail(googleUser.email())
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(googleUser.email())
                            .name(googleUser.name())
                            .build();
                    User savedUser = userRepository.save(newUser);

                    scoreService.createScore(savedUser.getId());

                    return savedUser;
                });


        String accessTokenJwt = jwtUtil.generateToken(user.getId());
        String refreshTokenJwt = jwtUtil.generateRefreshToken(user.getId());

        return new UserLoginResponseDTO(
                user.getId(),
                accessTokenJwt,
                refreshTokenJwt
        );
    }
    public GoogleUserInfoDTO requestGoogleUserInfo(String accessToken) {

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://openidconnect.googleapis.com/v1/userinfo",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new CustomException(
                        ErrorCode.GOOGLE_LOGIN_FAILED,
                        "Google userinfo 요청 실패"
                );
            }

            Map<String, Object> body = response.getBody();

            return new GoogleUserInfoDTO(
                    (String) body.get("sub"),     // google_sub
                    (String) body.get("email"),
                    (String) body.get("name")
            );

        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.GOOGLE_LOGIN_FAILED,
                    "Google userinfo 호출 실패"
            );
        }
    }
}
