import { Brain, CheckCircle2, Timer } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useApi } from '../lib/useApi';
import { fmtINR, tone } from '../lib/format';
import { Badge, Disclaimer, EmptyState, ErrorState, Metric, PageHeader, Panel, Skeleton } from '../components/ui';

const LESSONS = {
  OVERTRADING: 'More trades ≠ more profit. Every trade pays STT and brokerage; impulsive entries rarely have an edge.',
  REVENGE_TRADING: 'Trying to “win it back” right after a loss usually means bigger size with less analysis.',
  LOSS_STREAK: 'Streaks happen. Halving size after two losses protects capital while you reassess.',
  HOLDING_LOSER: 'The disposition effect: we hold losers hoping for recovery and cut winners too early.',
  NO_STOP_LOSS: 'A stop-loss is your plan for being wrong. Without it, the market decides your loss.',
  POSITION_SIZE: 'Risk 1–2% of capital per trade so no single trade can seriously damage your account.',
};

export default function Insights() {
  const { data, error, reload } = useApi('/insights');
  if (error) return <ErrorState message={error} onRetry={reload} />;
  const d = data; const score = d?.disciplineScore ?? 0; const circ = 2 * Math.PI * 52;
  const maxTrades = Math.max(1, ...(d?.consistency || []).map((c) => c.trades));
  return (
    <>
      <PageHeader icon={Brain} title="Behavioural Insights" subtitle="Rule-based findings, each linked to the trades that support it"><Disclaimer>Rule-based analysis of recorded trades. Not a psychological assessment.</Disclaimer></PageHeader>
      {!d ? <Skeleton className="h-96" /> : (<>
        <div className="grid lg:grid-cols-[320px_1fr] gap-3">
          <div className="card p-6 flex flex-col items-center justify-center">
            <svg width="140" height="140" viewBox="0 0 120 120" role="img" aria-label={`Discipline score ${score}`}>
              <circle cx="60" cy="60" r="52" fill="none" stroke="rgb(var(--raised))" strokeWidth="10" />
              <circle cx="60" cy="60" r="52" fill="none" stroke={`rgb(var(--${score >= 80 ? 'up' : score >= 60 ? 'amber' : 'down'}))`} strokeWidth="10" strokeLinecap="round"
                strokeDasharray={circ} strokeDashoffset={circ * (1 - score / 100)} transform="rotate(-90 60 60)" style={{ transition: 'stroke-dashoffset 1s ease' }} />
              <text x="60" y="58" textAnchor="middle" className="num" fontSize="30" fontWeight="700" fill="rgb(var(--ink))">{score}</text>
              <text x="60" y="78" textAnchor="middle" fontSize="10" fill="rgb(var(--faint))">/ 100</text>
            </svg>
            <div className="label mt-3">Discipline score</div><Badge tone={score >= 80 ? 'up' : score >= 60 ? 'amber' : 'down'} className="mt-2">{d.riskProfile}</Badge>
            {d.cooldownActive && <div className="mt-4 text-xs text-down flex items-center gap-1.5"><Timer className="w-3.5 h-3.5" />Cooldown suggested ({d.currentLossStreak} losses in a row)</div>}
          </div>
          <div className="grid grid-cols-2 md:grid-cols-3 gap-3">
            <Metric label="Total fills" value={<span className="num">{d.stats.trades}</span>} />
            <Metric label="Closed trades" value={<span className="num">{d.stats.closedTrades}</span>} accent="violet" />
            <Metric label="Win rate" value={<span className="num">{d.stats.winRatePct}%</span>} accent="up" />
            <Metric label="Average win" value={<span className="num text-up">{fmtINR(d.stats.avgWin)}</span>} accent="up" />
            <Metric label="Average loss" value={<span className="num text-down">{fmtINR(d.stats.avgLoss)}</span>} accent="amber" />
            <Metric label="Trading days" value={<span className="num">{d.stats.tradingDays}</span>} />
          </div>
        </div>
        <div className="grid lg:grid-cols-2 gap-3 mt-3">
          <Panel title="Detected patterns" subtitle="Evidence = trade IDs from your Orders page">
            {d.insights.length === 0 ? <EmptyState icon={CheckCircle2} title="No risky patterns detected" body="Keep trading with a written reason and a stop-loss. Insights appear as your history grows." /> : (
              <ul className="space-y-2.5">{d.insights.map((i, k) => (
                <li key={k} className={`rounded-lg border p-3 ${i.severity === 'high' ? 'border-down/30 bg-down/5' : 'border-amber/30 bg-amber/5'}`}>
                  <div className="flex items-center gap-2"><Badge tone={i.severity === 'high' ? 'down' : 'amber'}>{i.code.replace('_', ' ')}</Badge><span className="text-sm font-semibold">{i.title}</span></div>
                  <p className="text-xs text-muted mt-1.5">{i.detail}</p>
                  <p className="text-xs mt-1.5"><b>Lesson:</b> {i.lesson}</p>
                  <p className="text-[11px] text-faint mt-1.5 num">Evidence: #{i.evidenceTradeIds.join(', #')}</p>
                </li>))}</ul>)}
          </Panel>
          <Panel title="Trading consistency" subtitle="Trades and realised P&L per day">
            {d.consistency.length === 0 ? <EmptyState title="No trading days yet" /> : (
              <div className="flex items-end gap-1.5 h-48 overflow-x-auto pb-6">
                {d.consistency.map((c) => (
                  <div key={c.date} className="flex flex-col items-center gap-1 min-w-[26px]" title={`${c.date}: ${c.trades} trades, P&L ${c.pnl}`}>
                    <div className={`w-4 rounded-t ${Number(c.pnl) >= 0 ? 'bg-up' : 'bg-down'}`} style={{ height: `${(c.trades / maxTrades) * 140 + 4}px` }} />
                    <span className="text-[9px] text-faint num -rotate-45 origin-top-left translate-y-2">{c.date.slice(5)}</span>
                  </div>))}
              </div>)}
          </Panel>
        </div>
        <Panel title="Mistake library" subtitle="Short lessons linked to each pattern" className="mt-3">
          <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-3">{Object.entries(LESSONS).map(([k, v]) => (
            <div key={k} className={`rounded-lg border p-3 ${d.insights.some((i) => i.code === k) ? 'border-amber/40' : 'border-line'}`}>
              <div className="text-xs font-semibold">{k.replace('_', ' ')}</div><p className="text-xs text-muted mt-1">{v}</p></div>))}</div>
          <p className="text-xs text-muted mt-3">Want a narrative review? Ask the <Link to="/app/coach" className="text-cyan">AI Trade Coach</Link>.</p>
        </Panel>
      </>)}
    </>
  );
}
