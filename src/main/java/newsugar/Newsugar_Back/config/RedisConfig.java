package newsugar.Newsugar_Back.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    private final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private String getEnv(String key) {
        String val = System.getenv(key);
        if (val == null) {
            val = dotenv.get(key);
        }
        return val;
    }

    private final String host = getEnv("REDIS_HOST");
    private final int port = Integer.parseInt(getEnv("REDIS_PORT") != null ? getEnv("REDIS_PORT") : "6379");

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.setValidateConnection(false); // 연결 실패해도 빈 생성 자체는 되도록 설정
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new StringRedisSerializer());

        return redisTemplate;
    }
}