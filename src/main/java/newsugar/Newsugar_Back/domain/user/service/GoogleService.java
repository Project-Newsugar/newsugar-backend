package newsugar.Newsugar_Back.domain.user.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.http.javanet.NetHttpTransport;
import io.github.cdimascio.dotenv.Dotenv;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import newsugar.Newsugar_Back.domain.user.dto.GoogleUserInfoDTO;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;


@Service
public class GoogleService {
    private final String GOOGLE_CLIENT_ID;
    private final String GOOGLE_CLIENT_SECRET;

    public GoogleService(){

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        String googleId = System.getenv("GOOGLE_CLIENT_ID");
        if(googleId == null) googleId = dotenv.get("GOOGLE_CLIENT_ID");
        this.GOOGLE_CLIENT_ID = googleId;

        String googleSecret = System.getenv("GOOGLE_CLIENT_SECRET");
        if(googleSecret == null) googleSecret = dotenv.get("GOOGLE_CLIENT_SECRET");
        this.GOOGLE_CLIENT_SECRET = googleSecret;
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
