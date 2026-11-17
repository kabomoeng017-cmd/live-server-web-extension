package bw.gov.dtef.analytics;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return Map.of("applications", 0, "approved", 0, "pending", 0, "paymentsBwp", 0,
            "illustrative", false, "source", "LIVE_REPORTING_PIPELINE_NOT_CONFIGURED");
    }
    @GetMapping("/districts")
    public List<Map<String, Object>> districts() { return List.of(); }
}
