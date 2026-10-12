package com.niftynexus.risk;

import com.niftynexus.auth.CurrentUser;
import com.niftynexus.wallet.WalletRepository;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/risk")
public class RiskController {
    private final RiskService risk; private final WalletRepository wallets;
    public RiskController(RiskService risk, WalletRepository wallets) { this.risk = risk; this.wallets = wallets; }

    @GetMapping public Map<String, Object> dashboard() { return risk.dashboard(CurrentUser.id(), wallets.findByUserId(CurrentUser.id()).orElseThrow()); }

    @GetMapping("/position-size")
    public Map<String, Object> positionSize(@RequestParam BigDecimal capital, @RequestParam BigDecimal riskPct, @RequestParam BigDecimal entry,
                                            @RequestParam BigDecimal stop, @RequestParam(required = false) BigDecimal target) {
        return risk.positionSize(capital, riskPct, entry, stop, target);
    }
}
