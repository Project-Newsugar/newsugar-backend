package newsugar.Newsugar_Back.domain.summary.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class CategorySummaryRedis {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration TTL = Duration.ofHours(6); // 6시간 TTL

    public CategorySummaryRedis(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Redis에 카테고리 요약 저장
    public void saveSummary(String key, String summary) {
        try {
            redisTemplate.opsForValue().set(key, summary, TTL);
        } catch (Exception e) {
            System.err.println("Redis 저장 실패 (Redis가 꺼져있을 수 있음): " + e.getMessage());
        }
    }

    // Redis에서 카테고리 요약 조회
    public String getSummary(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            System.err.println("Redis 조회 실패 (Redis가 꺼져있을 수 있음): " + e.getMessage());
            return null;
        }
    }

    // Redis에서 삭제
    public void deleteSummary(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            System.err.println("Redis 삭제 실패 (Redis가 꺼져있을 수 있음): " + e.getMessage());
        }
    }
}
