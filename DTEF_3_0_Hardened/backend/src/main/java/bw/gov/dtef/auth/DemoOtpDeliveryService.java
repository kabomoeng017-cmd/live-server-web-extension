package bw.gov.dtef.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@Profile("demo")
public class DemoOtpDeliveryService implements OtpDeliveryService {
    private static final Logger log = LoggerFactory.getLogger(DemoOtpDeliveryService.class);
    private final boolean exposeInLog;
    public DemoOtpDeliveryService(@Value("${dtef.demo-otp-log-enabled:true}") boolean exposeInLog) { this.exposeInLog = exposeInLog; }
    @Override
    public void deliver(String destination, String code, String purpose) {
        // Local demonstration only. Production must use an approved SMS/email provider.
        if (exposeInLog) log.warn("DEMO OTP [{}] for {}: {} (never enable OTP logging in production)", purpose, destination, code);
        else log.info("Demo OTP generated for {} (code hidden; use a real delivery provider for this profile)", destination);
    }
}
