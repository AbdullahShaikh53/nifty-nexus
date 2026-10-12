package com.niftynexus.behaviour;

import com.niftynexus.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {
    private final BehaviourService service;
    public InsightsController(BehaviourService service) { this.service = service; }
    @GetMapping public Map<String, Object> insights() { return service.analyse(CurrentUser.id()); }
}
