package com.niftynexus.portfolio;

import com.niftynexus.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {
    private final PortfolioService service;
    public PortfolioController(PortfolioService service) { this.service = service; }

    @GetMapping public Map<String, Object> summary() { return service.summary(CurrentUser.id()); }
    @GetMapping("/leaderboard") public List<Map<String, Object>> leaderboard() { return service.leaderboard(); }
    @PostMapping("/reset") public Map<String, Object> reset() { return service.resetWallet(CurrentUser.id(), CurrentUser.get().email()); }
}
