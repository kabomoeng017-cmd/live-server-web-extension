package bw.gov.dtef.auth;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Profile("!demo")
public class UnconfiguredOtpDeliveryService implements OtpDeliveryService {
    @Override
    public void deliver(String destination, String code, String purpose) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "OTP delivery provider is not configured. Connect an approved SMS/email provider before production use.");
    }
}
