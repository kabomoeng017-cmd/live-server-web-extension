package bw.gov.dtef.notification;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationQueueService queue;
    private final Environment environment;
    public NotificationController(NotificationQueueService queue, Environment environment) { this.queue = queue; this.environment = environment; }
    @GetMapping
    public List<Map<String, String>> list(@AuthenticationPrincipal Jwt jwt) { return List.of(); }
    @PostMapping("/send")
    public Map<String, Object> send(@Valid @RequestBody NotificationRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (!environment.acceptsProfiles(Profiles.of("demo"))) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Production notification delivery is not configured; no message was queued.");
        String id = queue.enqueue(request.recipientSubject(), request.templateId(), request.channel());
        return Map.of("queued", true, "messageId", id, "mode", "QUEUED_DEMO_NOT_DELIVERED");
    }
    public record NotificationRequest(@NotBlank @Size(max = 120) String recipientSubject,
                                       @NotBlank @Pattern(regexp = "[A-Z0-9_]{2,60}") String templateId,
                                       @NotBlank @Pattern(regexp = "IN_APP|SMS|EMAIL|WHATSAPP") String channel) {}
}
