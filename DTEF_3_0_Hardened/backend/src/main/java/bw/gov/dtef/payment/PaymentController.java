package bw.gov.dtef.payment;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    @GetMapping("/student/{reference}")
    public Map<String, Object> student(@PathVariable String reference, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("reference", reference, "tuition", "NOT_CONNECTED", "allowance", "NOT_CONNECTED",
            "mode", "READ_ONLY_PLACEHOLDER", "message", "Payment data requires an authorized FNBB integration.");
    }
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of("processedBwp", 0, "pending", 0, "failed", 0, "mode", "INTEGRATION_NOT_CONFIGURED");
    }
    @PostMapping("/process/{reference}")
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public Map<String, Object> process(@PathVariable String reference) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Payment execution is disabled until FNBB contract, signing, reconciliation and callback verification are implemented.");
    }
}
