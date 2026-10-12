package com.niftynexus.backtest;

import com.niftynexus.auth.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/backtest")
public class BacktestController {
    private final BacktestService service;
    public BacktestController(BacktestService service) { this.service = service; }
    @PostMapping public Map<String, Object> run(@RequestBody BacktestService.Request r) { return service.run(CurrentUser.id(), r); }
    @GetMapping("/history") public List<BacktestRun> history() { return service.history(CurrentUser.id()); }
}
