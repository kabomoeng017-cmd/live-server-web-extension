package bw.gov.dtef.identity;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.UUID;

@Service
public class IdentityVerificationService {
    private final StringRedisTemplate redis;
    public IdentityVerificationService(StringRedisTemplate redis) { this.redis = redis; }
    public String createDemoVerification(String username) {
        String id = UUID.randomUUID().toString();
        redis.opsForValue().set("dtef:identity:" + id, username, Duration.ofMinutes(15));
        return id;
    }
    public boolean belongsTo(String verificationId, String username) {
        if (verificationId == null) return false;
        return username.equals(redis.opsForValue().get("dtef:identity:" + verificationId));
    }
}
