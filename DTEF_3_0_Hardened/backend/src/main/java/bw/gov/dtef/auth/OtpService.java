package bw.gov.dtef.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class OtpService {
    private final StringRedisTemplate redis;
    private final PasswordEncoder encoder;
    private final OtpDeliveryService delivery;
    private final SecureRandom random = new SecureRandom();
    private static final DefaultRedisScript<Long> RELEASE_LOCK = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);
    private final long ttlSeconds;
    private final int maxAttempts;

    public OtpService(StringRedisTemplate redis, PasswordEncoder encoder, OtpDeliveryService delivery,
                      @Value("${security.otp.ttl-seconds:300}") long ttlSeconds,
                      @Value("${security.otp.max-attempts:5}") int maxAttempts) {
        this.redis = redis; this.encoder = encoder; this.delivery = delivery;
        this.ttlSeconds = ttlSeconds; this.maxAttempts = maxAttempts;
    }

    public void issue(String scope, String destination, String purpose) { issue(scope, destination, purpose, scope); }

    public void issue(String scope, String destination, String purpose, String rateIdentity) {
        String rateKey = "dtef:otp:rate:" + rateIdentity;
        Long count = redis.opsForValue().increment(rateKey);
        if (count != null && count == 1L) redis.expire(rateKey, Duration.ofMinutes(15));
        if (count != null && count > 3L) throw new IllegalStateException("OTP rate limit reached. Try again later.");
        String lockKey = "dtef:otp:lock:" + scope;
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(30));
        if (!Boolean.TRUE.equals(locked)) throw new IllegalStateException("An OTP operation is already in progress. Try again shortly.");
        try {
            String code = String.format("%06d", random.nextInt(1_000_000));
            redis.opsForValue().set("dtef:otp:value:" + scope, encoder.encode(code), Duration.ofSeconds(ttlSeconds));
            redis.delete("dtef:otp:attempts:" + scope);
            try { delivery.deliver(destination, code, purpose); }
            catch (RuntimeException ex) { redis.delete("dtef:otp:value:" + scope); throw ex; }
        } finally {
            redis.execute(RELEASE_LOCK, List.of(lockKey), lockToken);
        }
    }

    public boolean verifyAndConsume(String scope, String suppliedCode) {
        if (suppliedCode == null || !suppliedCode.matches("\\d{6}")) return false;
        String lockKey = "dtef:otp:lock:" + scope;
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(lockKey, lockToken, Duration.ofSeconds(30));
        if (!Boolean.TRUE.equals(locked)) return false;
        try {
            String valueKey = "dtef:otp:value:" + scope;
            String attemptsKey = "dtef:otp:attempts:" + scope;
            String encoded = redis.opsForValue().get(valueKey);
            if (encoded == null) return false;
            Long attempts = redis.opsForValue().increment(attemptsKey);
            if (attempts != null && attempts == 1L) redis.expire(attemptsKey, Duration.ofSeconds(ttlSeconds));
            if (attempts != null && attempts > maxAttempts) {
                redis.delete(valueKey); redis.delete(attemptsKey); return false;
            }
            if (!encoder.matches(suppliedCode, encoded)) return false;
            redis.delete(valueKey); redis.delete(attemptsKey); return true;
        } finally {
            redis.execute(RELEASE_LOCK, List.of(lockKey), lockToken);
        }
    }
}
