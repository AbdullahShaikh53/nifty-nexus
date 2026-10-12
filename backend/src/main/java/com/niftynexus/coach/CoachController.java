package com.niftynexus.coach;

import com.niftynexus.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/coach")
public class CoachController {
    private final CoachService coach;
    public CoachController(CoachService coach) { this.coach = coach; }
    public record Ask(String question) {}
    @PostMapping("/ask") public Map<String, Object> ask(@RequestBody Ask a) { return coach.ask(CurrentUser.id(), a.question()); }
}
