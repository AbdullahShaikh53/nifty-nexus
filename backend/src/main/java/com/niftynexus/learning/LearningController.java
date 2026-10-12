package com.niftynexus.learning;

import com.niftynexus.auth.CurrentUser;
import com.niftynexus.behaviour.BehaviourService;
import com.niftynexus.common.ApiException;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.OrderRepository;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.user.User;
import com.niftynexus.user.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/** Learning hub: lessons, F&O risk quiz gate (FR-31), daily challenges, badges and skill level (FR-40). */
@RestController
@RequestMapping("/api/learning")
public class LearningController {
    private final UserRepository users; private final OrderRepository orders; private final TradeRepository trades; private final UserBadgeRepository badges; private final BehaviourService behaviour;

    public LearningController(UserRepository users, OrderRepository orders, TradeRepository trades, UserBadgeRepository badges, BehaviourService behaviour) {
        this.users = users; this.orders = orders; this.trades = trades; this.badges = badges; this.behaviour = behaviour;
    }

    private static final List<Map<String, Object>> QUIZ = List.of(
            q(1, "What is the maximum loss for an option buyer?", List.of("Unlimited", "The premium paid", "The strike price", "Zero"), 1),
            q(2, "A futures position of 1 lot of Nifty (lot size 25) at 24,000 has a notional value of:", List.of("Rs 24,000", "Rs 6,00,000", "Rs 2,40,000", "Rs 60,000"), 1),
            q(3, "Theta measures how an option price changes with:", List.of("Volatility", "Interest rates", "Time", "Underlying price"), 2),
            q(4, "What does SEBI data (2023) say about retail F&O traders?", List.of("Most profit", "About 9 in 10 lose money", "Half profit", "No data"), 1),
            q(5, "Selling a naked option exposes you to:", List.of("Limited loss", "No loss", "Potentially unlimited loss", "Only premium loss"), 2),
            q(6, "A margin call happens when:", List.of("You make a profit", "Available margin falls below requirement", "Market closes", "You square off"), 1));

    private static Map<String, Object> q(int id, String question, List<String> options, int answer) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", id); m.put("question", question); m.put("options", options); m.put("answer", answer); return m;
    }

    @GetMapping("/quiz")
    public List<Map<String, Object>> quiz() {
        return QUIZ.stream().map(x -> { Map<String, Object> m = new LinkedHashMap<>(x); m.remove("answer"); return m; }).toList();
    }

    @PostMapping("/quiz")
    public Map<String, Object> submit(@RequestBody Map<String, Integer> answers) {
        int correct = 0;
        for (Map<String, Object> qq : QUIZ) { Integer a = answers.get(String.valueOf(qq.get("id"))); if (a != null && a.equals(qq.get("answer"))) correct++; }
        boolean passed = correct >= 5;
        User u = users.findById(CurrentUser.id()).orElseThrow();
        if (passed && !u.isFnoUnlocked()) { u.setFnoUnlocked(true); users.save(u); award(u.getId(), "FNO_READY"); }
        Map<String, Object> m = new LinkedHashMap<>(); m.put("correct", correct); m.put("total", QUIZ.size()); m.put("passed", passed); m.put("fnoUnlocked", u.isFnoUnlocked()); return m;
    }

    @GetMapping("/fno")
    public Map<String, Object> fno() {
        User u = users.findById(CurrentUser.id()).orElseThrow();
        if (!u.isFnoUnlocked()) throw ApiException.forbidden("F&O practice is locked. Pass the risk quiz in the Learning Hub first.");
        return Map.of("unlocked", true, "note", "Option-chain data is not configured in this build; the payoff and margin calculators below work with values you enter.");
    }

    private void award(Long userId, String code) { if (!badges.existsByUserIdAndBadgeCode(userId, code)) { UserBadge b = new UserBadge(); b.setUserId(userId); b.setBadgeCode(code); badges.save(b); } }

    @GetMapping("/progress")
    public Map<String, Object> progress() {
        Long uid = CurrentUser.id();
        List<Order> os = orders.findByUserIdOrderByPlacedAtDesc(uid);
        long filled = os.stream().filter(o -> o.getStatus() == Order.Status.FILLED).count();
        long withSl = os.stream().filter(o -> o.getSide() == Order.Side.BUY && o.getStopLoss() != null).count();
        long withReason = os.stream().filter(o -> o.getReasonNote() != null && !o.getReasonNote().isBlank()).count();
        Map<String, Object> ins = behaviour.analyse(uid);
        if (filled >= 1) award(uid, "FIRST_TRADE");
        if (withSl >= 5) award(uid, "DISCIPLINED_EXITS");
        if (withReason >= 10) award(uid, "JOURNAL_KEEPER");
        if (filled >= 25) award(uid, "ACTIVE_LEARNER");
        if ((int) ins.get("disciplineScore") >= 90 && filled >= 10) award(uid, "ICE_COLD");
        long todayOrders = os.stream().filter(o -> o.getPlacedAt().toLocalDate().equals(LocalDate.now())).count();
        long todaySl = os.stream().filter(o -> o.getPlacedAt().toLocalDate().equals(LocalDate.now()) && o.getStopLoss() != null).count();
        List<Map<String, Object>> challenges = List.of(
                ch("Place one trade with a written reason", os.stream().anyMatch(o -> o.getPlacedAt().toLocalDate().equals(LocalDate.now()) && o.getReasonNote() != null && !o.getReasonNote().isBlank())),
                ch("Every buy today carries a stop-loss", todayOrders > 0 && todaySl == os.stream().filter(o -> o.getPlacedAt().toLocalDate().equals(LocalDate.now()) && o.getSide() == Order.Side.BUY).count()),
                ch("Stay under 3 orders today", todayOrders <= 3),
                ch("Read one lesson in the Learning Hub", false));
        String level = filled < 5 ? "Beginner" : filled < 25 ? "Apprentice" : filled < 100 ? "Practitioner" : "Veteran";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("level", level); m.put("filledOrders", filled); m.put("badges", badges.findByUserId(uid)); m.put("challenges", challenges);
        m.put("disciplineScore", ins.get("disciplineScore")); m.put("fnoUnlocked", users.findById(uid).orElseThrow().isFnoUnlocked());
        return m;
    }

    private static Map<String, Object> ch(String title, boolean done) { Map<String, Object> m = new LinkedHashMap<>(); m.put("title", title); m.put("done", done); return m; }
}
