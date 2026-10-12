package com.niftynexus.coach;

import com.niftynexus.behaviour.BehaviourService;
import com.niftynexus.config.AppProperties;
import com.niftynexus.orders.Order;
import com.niftynexus.orders.Trade;
import com.niftynexus.orders.TradeRepository;
import com.niftynexus.portfolio.PortfolioService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * AI Trade Coach (FR-55, FR-58). Rule-based by default; answers are grounded in the user's own trades.
 * An LLM call can be enabled with COACH_LLM_ENABLED=true (key from env) - until then responses are
 * clearly labelled "rule-based".
 */
@Service
public class CoachService {
    private final TradeRepository trades; private final BehaviourService behaviour; private final PortfolioService portfolio; private final AppProperties props;

    public CoachService(TradeRepository trades, BehaviourService behaviour, PortfolioService portfolio, AppProperties props) {
        this.trades = trades; this.behaviour = behaviour; this.portfolio = portfolio; this.props = props;
    }

    private static final Map<String, String> GLOSSARY = Map.of(
            "rsi", "RSI (Relative Strength Index) measures how fast prices moved up vs down over 14 periods, on a 0-100 scale. Above 70 is often called overbought, below 30 oversold. It is a context tool, not a buy/sell signal on its own.",
            "sma", "A Simple Moving Average is the average closing price over N days. Price above a rising SMA suggests an uptrend; a short SMA crossing above a long SMA is the classic 'golden cross'.",
            "stop loss", "A stop-loss is a pre-decided exit price that limits how much you lose if the trade goes against you. Set it where your trade idea is proven wrong, then size the position so that loss is 1-2% of capital.",
            "slippage", "Slippage is the difference between the price you expected and the price you actually got. Nifty Nexus simulates a small slippage so your paper results are realistic.",
            "stt", "Securities Transaction Tax is charged by the government on every equity trade - 0.1% on delivery buys and sells. It is usually the single largest cost of a delivery trade.",
            "drawdown", "Drawdown is the fall from a portfolio's peak value to its trough. A 20% drawdown needs a 25% gain just to get back to even - which is why protecting capital matters more than chasing returns.",
            "limit order", "A limit order sets the maximum price you will pay (or minimum you will accept). It protects you from bad fills but may never execute. A market order fills immediately at the current price.",
            "risk reward", "Risk:reward compares what you could lose (entry minus stop) to what you could gain (target minus entry). At 1:2, you can be wrong half the time and still profit.");

    public Map<String, Object> ask(Long userId, String question) {
        String q = question == null ? "" : question.toLowerCase();
        Map<String, Object> ins = behaviour.analyse(userId);
        List<Trade> recent = trades.findByUserIdOrderByExecutedAtDesc(userId);
        StringBuilder sb = new StringBuilder();
        List<String> refs = new ArrayList<>();

        if (recent.isEmpty() && !q.contains("what") && !q.contains("explain")) {
            sb.append("You have not placed any paper trades yet. Start with one small market order with a stop-loss and a reason, then ask me to review it.");
        } else if (q.contains("why") && (q.contains("drop") || q.contains("fall") || q.contains("down") || q.contains("loss"))) {
            Map<String, Object> s = portfolio.summary(userId);
            @SuppressWarnings("unchecked") List<Map<String, Object>> hs = (List<Map<String, Object>>) s.get("holdings");
            hs.sort(Comparator.comparing(h -> (BigDecimal) h.get("unrealisedPnl")));
            sb.append("Your portfolio is worth ").append(s.get("totalValue")).append(" (").append(s.get("overallReturnPct")).append("% overall). ");
            if (!hs.isEmpty()) {
                Map<String, Object> worst = hs.get(0);
                sb.append("The biggest drag is ").append(worst.get("symbol")).append(" at ").append(worst.get("unrealisedPnl")).append(" (").append(worst.get("unrealisedPct")).append("%). ");
                refs.add((String) worst.get("symbol"));
            }
            sb.append("Charges paid so far: ").append(s.get("chargesPaid")).append(". Realised P&L: ").append(s.get("realisedPnl")).append(".");
        } else if (q.contains("mistake") || q.contains("review") || q.contains("analy") || q.contains("improve") || q.contains("summary") || q.contains("week")) {
            @SuppressWarnings("unchecked") List<BehaviourService.Insight> list = (List<BehaviourService.Insight>) ins.get("insights");
            sb.append("Discipline score ").append(ins.get("disciplineScore")).append("/100 (").append(ins.get("riskProfile")).append("). ");
            if (list.isEmpty()) sb.append("No behavioural patterns detected in your recorded trades - keep writing a reason before each order.");
            else { sb.append("Patterns found: "); for (BehaviourService.Insight i : list) { sb.append("\n- ").append(i.title()).append(": ").append(i.lesson()); } }
            Map<?, ?> st = (Map<?, ?>) ins.get("stats");
            sb.append("\nClosed trades: ").append(st.get("closedTrades")).append(", win rate ").append(st.get("winRatePct")).append("%, average win ").append(st.get("avgWin")).append(", average loss ").append(st.get("avgLoss")).append(".");
        } else if (q.contains("last trade") || q.contains("recent trade") || q.contains("poorly") || q.contains("bad trade")) {
            Trade t = recent.get(0);
            sb.append("Your most recent fill: ").append(t.getSide()).append(" ").append(t.getFillQuantity().stripTrailingZeros().toPlainString()).append(" ").append(t.getSymbol())
                    .append(" at ").append(t.getFillPrice()).append(" with charges ").append(t.getCharges().setScale(2, RoundingMode.HALF_UP)).append(". ");
            if (t.getSide() == Order.Side.SELL) sb.append(t.getRealisedPnl().signum() < 0 ? "It closed at a loss of " + t.getRealisedPnl().abs().setScale(2, RoundingMode.HALF_UP) + ". Check the journal: was the stop-loss respected, or did you exit on emotion?" : "It closed with a profit of " + t.getRealisedPnl().setScale(2, RoundingMode.HALF_UP) + ". Compare it to your target - did you exit early?");
            else sb.append("It is still open. Make sure a stop-loss is defined so the exit is not decided by emotion.");
            refs.add(t.getSymbol());
        } else {
            boolean matched = false;
            for (var e : GLOSSARY.entrySet()) if (q.contains(e.getKey())) { sb.append(e.getValue()); matched = true; break; }
            if (!matched) sb.append("I can review your recent trades, summarise recurring mistakes, explain why your portfolio moved, or explain indicators such as RSI, SMA, stop-loss, slippage, STT and drawdown. Try: 'Review my mistakes' or 'Why did my portfolio drop?'");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("answer", sb.toString()); m.put("mode", props.getCoach().isLlmEnabled() ? "llm" : "rule-based"); m.put("references", refs);
        m.put("disclaimer", "Educational information only, not investment advice.");
        m.put("suggestions", List.of("Review my mistakes", "Why did my portfolio drop?", "Analyse my last trade", "Explain RSI", "What is risk reward?"));
        return m;
    }
}
