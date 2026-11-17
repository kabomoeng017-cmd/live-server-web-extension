package bw.gov.dtef.welfare;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/welfare")
public class WelfareController {
    @GetMapping("/queue")
    public Map<String, Object> queue() { return Map.of("items", java.util.List.of(), "count", 0, "source", "WELFARE_INTEGRATION_REQUIRED"); }
    @PostMapping("/assess")
    public Map<String, Object> assess(@Valid @RequestBody AssessmentRequest request) {
        return Map.of("accepted", false, "mode", "ASSESSMENT_ENGINE_NOT_CONFIGURED",
            "message", "No automated vulnerability score is generated. A qualified welfare officer must assess evidence.");
    }
    public record AssessmentRequest(@NotBlank String applicationReference) {}
}
