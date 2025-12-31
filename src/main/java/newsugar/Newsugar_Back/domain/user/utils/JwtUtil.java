package newsugar.Newsugar_Back.domain.user.utils;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import newsugar.Newsugar_Back.common.CustomException;
import newsugar.Newsugar_Back.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Component
@Getter
public class JwtUtil {
    private final String JWT_SECRET;
    private final long JWT_EXPIRATION;
    private final String JWT_REFRESH_SECRET;
    private final long JWT_REFRESH_EXPIRATION;
    private final Key key;
    private final Key refreshKey;

    public JwtUtil() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String secret = System.getenv("JWT_SECRET");
        if (secret == null) secret = dotenv.get("JWT_SECRET");
        this.JWT_SECRET = secret;

        String exp = System.getenv("JWT_EXPIRATION");
        if (exp == null) exp = dotenv.get("JWT_EXPIRATION");
        this.JWT_EXPIRATION = Long.parseLong(Objects.requireNonNull(exp));

        String refSecret = System.getenv("JWT_REFRESH_SECRET");
        if (refSecret == null) refSecret = dotenv.get("JWT_REFRESH_SECRET");
        this.JWT_REFRESH_SECRET = refSecret;

        String refExp = System.getenv("JWT_REFRESH_EXPIRATION");
        if (refExp == null) refExp = dotenv.get("JWT_REFRESH_EXPIRATION");
        this.JWT_REFRESH_EXPIRATION = Long.parseLong(Objects.requireNonNull(refExp));

        this.key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes());
        this.refreshKey = Keys.hmacShaKeyFor(JWT_REFRESH_SECRET.getBytes());
    }

    public String generateToken (Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken (Long userId){
        Date now = new Date();
        Date expiry = new Date(now.getTime() + JWT_REFRESH_EXPIRATION);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Long validateToken(String token){
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

    public Long validateRefresh(String token){
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(refreshKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody()
                        .getSubject()
        );
    }

}