package com.niftynexus.charges;

import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/charges")
public class ChargesController {
    private final ChargesService charges; private final ChargesConfigRepository repo;
    public ChargesController(ChargesService charges, ChargesConfigRepository repo) { this.charges = charges; this.repo = repo; }

    @GetMapping("/estimate")
    public ChargesBreakdown estimate(@RequestParam String side, @RequestParam BigDecimal price, @RequestParam BigDecimal quantity) {
        return charges.compute(side, price, quantity);
    }

    @GetMapping("/rates") public List<ChargesConfig> rates() { return repo.findAll(); }
}
