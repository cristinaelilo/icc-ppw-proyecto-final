package ec.edu.ups.icc.proyecto.common.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Rate limiting distribuido usando Redis (INCR atomico + TTL).
 * Tambien maneja el bloqueo TEMPORAL de IP+correo tras intentos fallidos de
 * login (distinto del bloqueo PERMANENTE administrativo en users.status).
 *
 * Prefijos de claves:
 *   rate:login:{ip}:{email}
 *   rate:register:{ip}
 *   rate:public:{ip}
 *   rate:auth:{email}
 *   rate:reports:{userId}
 *   failed-attempts:{ip}:{email}
 *   blocked-user:{ip}:{email}
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public RateLimitResult increment(String key, long limit, long windowSeconds) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            count = 1L;
        }
        if (count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        Long ttl = redisTemplate.getExpire(key);
        long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;
        boolean allowed = count <= limit;
        return new RateLimitResult(allowed, count, limit, retryAfter);
    }

    public boolean isBlocked(String ip, String email) {
        String key = "blocked-user:" + ip + ":" + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long registerFailedAttempt(String ip, String email, int maxAttempts, long blockDurationSeconds) {
        String failedKey = "failed-attempts:" + ip + ":" + email;
        Long attempts = redisTemplate.opsForValue().increment(failedKey);
        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(failedKey, Duration.ofSeconds(blockDurationSeconds));
        }
        if (attempts != null && attempts >= maxAttempts) {
            String blockedKey = "blocked-user:" + ip + ":" + email;
            redisTemplate.opsForValue().set(blockedKey, "1", Duration.ofSeconds(blockDurationSeconds));
        }
        return attempts == null ? 0 : attempts;
    }

    public void clearFailedAttempts(String ip, String email) {
        redisTemplate.delete("failed-attempts:" + ip + ":" + email);
    }

    public record RateLimitResult(boolean allowed, long currentCount, long limit, long retryAfterSeconds) {}
}
