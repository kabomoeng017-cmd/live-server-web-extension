package bw.gov.dtef.auth;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OtpServiceTest {
    @SuppressWarnings("unchecked")
    @Test void issuesHashedOtpWithTtlAndDeliversIt() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment(anyString())).thenReturn(1L);
        when(values.setIfAbsent(eq("dtef:otp:lock:action:student:APPLICATION_SUBMIT"), anyString(), any(Duration.class))).thenReturn(true);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("hashed-otp");
        OtpDeliveryService delivery = mock(OtpDeliveryService.class);
        OtpService service = new OtpService(redis, encoder, delivery, 300, 5);
        service.issue("action:student:APPLICATION_SUBMIT", "student", "APPLICATION_SUBMIT");
        verify(values).set(eq("dtef:otp:value:action:student:APPLICATION_SUBMIT"), eq("hashed-otp"), any(Duration.class));
        verify(delivery).deliver(eq("student"), matches("\\d{6}"), eq("APPLICATION_SUBMIT"));
    }

    @SuppressWarnings("unchecked")
    @Test void rejectsConcurrentOtpConsumptionWhenLockIsHeld() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("dtef:otp:lock:scope"), anyString(), any(Duration.class))).thenReturn(false);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        OtpService service = new OtpService(redis, encoder, mock(OtpDeliveryService.class), 300, 5);
        assertFalse(service.verifyAndConsume("scope", "123456"));
        verify(values, never()).get(anyString());
        verifyNoInteractions(encoder);
    }

    @SuppressWarnings("unchecked")
    @Test void consumesValidOtpOnlyOnce() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(eq("dtef:otp:lock:scope"), anyString(), any(Duration.class))).thenReturn(true);
        when(values.get("dtef:otp:value:scope")).thenReturn("hashed");
        when(values.increment("dtef:otp:attempts:scope")).thenReturn(1L);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.matches("123456", "hashed")).thenReturn(true);
        OtpService service = new OtpService(redis, encoder, mock(OtpDeliveryService.class), 300, 5);
        assertTrue(service.verifyAndConsume("scope", "123456"));
        verify(redis).delete("dtef:otp:value:scope");
    }
}
