package util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    public void setRefreshToken(String key, String token, long expireMillis) {
        redisTemplate.opsForValue().set(key, token, expireMillis, TimeUnit.MILLISECONDS);
    }

    public String getRefreshToken(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteRefreshToken(String email) {
        String key = email;
        Boolean result = redisTemplate.delete(key);
    }

    public void setTempDeactivate(String email, String value, int minutes) {
        redisTemplate.opsForValue().set(email + "_deactivate", value, Duration.ofMinutes(minutes));
    }
    public String getTempDeactivate(String email) {
        return redisTemplate.opsForValue().get(email + "_deactivate");
    }

}
