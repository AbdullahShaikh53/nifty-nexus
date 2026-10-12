import { Link } from 'react-router-dom';
import { Activity, ArrowDownRight, ArrowUpRight, Brain, Gauge, LayoutDashboard, Wallet } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { fmtINR, fmtInt, fmtPct, fmtTime, fmtDate, tone } from '../lib/format';
import { AnimatedNumber, Badge, Change, ErrorState, FlashPrice, Metric, PageHeader, Panel, SimBadge, Skeleton } from '../components/ui';
import { useQuote } from '../context/MarketContext';
import Sparkline from '../components/charts/Sparkline';
import { useTickHistory } from '../components/charts/useTickHistory';
import { useAuth } from '../context/AuthContext';

export function IndexCard({ symbol, label }) {
  const q = useQuote(symbol); const hist = useTickHistory(symbol);
  return (
    <div className="card p-4">
      <div className="flex items-center justify-between"><span className="label">{label || symbol}</span>{q?.source === 'MOCK' && <span className="text-[10px] text-amber">SIM</span>}</div>
      {!q ? <Skeleton className="h-7 w-32 mt-2" /> : (
        <div className="flex items-end justify-between mt-1.5 gap-2">
          <div><FlashPrice value={Number(q.ltp)} format={(v) => Number(v).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} className="text-xl font-semibold" />
            <div><Change value={q.change} pct={q.changePct} /></div></div>
          <Sparkline values={hist} />
        </div>)}
    </div>
  );
}

function MoverRow({ r }) {
  const q = useQuote(r.symbol) || r;
  return (
    <Link to={`/app/charts?symbol=${encodeURIComponent(r.symbol)}`} className="flex items-center justify-between py-2 px-1 rounded-md hover:bg-raised/60">
      <div className="min-w-0"><div className="text-sm font-semibold">{r.symbol}</div><div className="text-[11px] text-faint truncate">{r.sector}</div></div>
      <div className="text-right"><div className="num text-sm">{Number(q.ltp).toFixed(2)}</div><div className={`num text-[11px] ${tone(q.changePct)}`}>{fmtPct(q.changePct)}</div></div>
    </Link>
  );
}

export default function Overview() {
  const { user } = useAuth();
  const ov = useApi('/market/overview', { poll: 5000 }); const tr = useApi('/orders/trades', { poll: 15000 }); const hol = useApi('/market/holidays'); const pf = useApi('/portfolio', { poll: 10000 }); const ins = useApi('/insights');
  const p = pf.data; const o = ov.data;
  const total = o ? o.advances + o.declines + o.unchanged : 0;
  return (
    <>
      <PageHeader icon={LayoutDashboard} title={`Good to see you, ${user?.name?.split(' ')[0] || 'trader'}`} subtitle="Your market, portfolio and discipline at a glance">{o?.simulated && <SimBadge />}</PageHeader>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <IndexCard symbol="NIFTY 50" /><IndexCard symbol="NIFTY BANK" /><IndexCard symbol="SENSEX" /><IndexCard symbol="INDIA VIX" />
      </div>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 mt-3">
        <Metric label="Portfolio value" icon={Wallet} accent="up" value={p ? <AnimatedNumber value={p.totalValue} format={(v) => fmtINR(v)} /> : <Skeleton className="h-6 w-28" />} sub={p && fmtPct(p.overallReturnPct)} subTone={tone(p?.overallReturnPct)} />
        <Metric label="Available cash" icon={Wallet} accent="cyan" value={p ? <span className="num">{fmtINR(p.available)}</span> : <Skeleton className="h-6 w-28" />} sub={p && `Reserved ${fmtINR(p.reserved, true)}`} />
        <Metric label="Unrealised P&L" icon={Activity} accent="violet" value={p ? <span className={`num ${tone(p.unrealisedPnl)}`}>{fmtINR(p.unrealisedPnl)}</span> : <Skeleton className="h-6 w-28" />} sub={p && `Realised ${fmtINR(p.realisedPnl)}`} subTone={tone(p?.realisedPnl)} />
        <Metric label="Discipline score" icon={Brain} accent="amber" value={ins.data ? <span className="num">{ins.data.disciplineScore}<span className="text-sm text-faint"> / 100</span></span> : <Skeleton className="h-6 w-20" />} sub={ins.data?.riskProfile} />
      </div>

      <div className="grid lg:grid-cols-3 gap-3 mt-3">
        <Panel title="Market breadth" subtitle="Nifty 50 constituents">
          {ov.error ? <ErrorState message={ov.error} onRetry={ov.reload} /> : !o ? <Skeleton className="h-24" /> : (<>
            <div className="flex justify-between text-sm mb-2"><span className="text-up flex items-center gap-1"><ArrowUpRight className="w-4 h-4" />{o.advances} advancing</span><span className="text-down flex items-center gap-1">{o.declines} declining<ArrowDownRight className="w-4 h-4" /></span></div>
            <div className="h-2.5 rounded-full overflow-hidden flex bg-raised" role="img" aria-label={`${o.advances} advances, ${o.declines} declines`}>
              <div className="bg-up transition-all duration-700" style={{ width: `${(o.advances / (total || 1)) * 100}%` }} />
              <div className="bg-faint/40" style={{ width: `${(o.unchanged / (total || 1)) * 100}%` }} />
              <div className="bg-down transition-all duration-700" style={{ width: `${(o.declines / (total || 1)) * 100}%` }} />
            </div>
            <div className="mt-3 text-xs text-muted">Advance / decline ratio <span className="num text-ink">{o.declines ? (o.advances / o.declines).toFixed(2) : '—'}</span></div>
            <div className="mt-4 label mb-2">Sector performance</div>
            <div className="space-y-1.5 max-h-56 overflow-auto pr-1">{o.sectors.map((s) => (
              <div key={s.sector} className="flex items-center gap-2 text-xs"><span className="w-28 truncate text-muted">{s.sector}</span>
                <div className="flex-1 h-1.5 bg-raised rounded relative"><div className={`absolute top-0 h-full rounded ${s.changePct >= 0 ? 'bg-up left-1/2' : 'bg-down right-1/2'}`} style={{ width: `${Math.min(50, Math.abs(s.changePct) * 20)}%` }} /></div>
                <span className={`num w-14 text-right ${tone(s.changePct)}`}>{fmtPct(s.changePct)}</span></div>))}</div>
          </>)}
        </Panel>
        <Panel title="Top gainers" action={<Badge tone="up">Today</Badge>}>{!o ? <Skeleton className="h-40" /> : o.gainers.map((r) => <MoverRow key={r.symbol} r={r} />)}</Panel>
        <Panel title="Top losers" action={<Badge tone="down">Today</Badge>}>{!o ? <Skeleton className="h-40" /> : o.losers.map((r) => <MoverRow key={r.symbol} r={r} />)}</Panel>
      </div>

      <div className="grid lg:grid-cols-3 gap-3 mt-3">
        <Panel title="Most active" subtitle="By volume">{!o ? <Skeleton className="h-40" /> : o.mostActive.map((r) => (
          <div key={r.symbol} className="flex justify-between py-1.5 text-sm"><span className="font-medium">{r.symbol}</span><span className="num text-muted">{fmtInt(r.volume)}</span></div>))}</Panel>
        <Panel title="Your holdings" action={<Link to="/app/portfolio" className="text-xs text-cyan">View all</Link>}>
          {!p ? <Skeleton className="h-40" /> : p.holdings.length === 0 ? <p className="text-sm text-muted py-6 text-center">No holdings yet. <Link to="/app/trade" className="text-up">Place your first trade →</Link></p> :
            p.holdings.slice(0, 6).map((h) => <div key={h.symbol} className="flex justify-between py-1.5 text-sm"><span className="font-medium">{h.symbol}</span><span className={`num ${tone(h.unrealisedPnl)}`}>{fmtINR(h.unrealisedPnl)}</span></div>)}
        </Panel>
        <Panel title="Discipline insights" action={<Link to="/app/insights" className="text-xs text-cyan">Details</Link>}>
          {!ins.data ? <Skeleton className="h-40" /> : ins.data.insights.length === 0 ? (
            <div className="text-center py-6"><Gauge className="w-6 h-6 text-up mx-auto" /><p className="text-sm mt-2">No risky patterns detected.</p><p className="text-xs text-muted">Keep writing a reason and stop-loss for each trade.</p></div>
          ) : ins.data.insights.slice(0, 3).map((i, k) => <div key={k} className="rounded-lg border border-line p-2.5 mb-2"><div className={`text-sm font-semibold ${i.severity === 'high' ? 'text-down' : 'text-amber'}`}>{i.title}</div><div className="text-xs text-muted">{i.lesson}</div></div>)}
        </Panel>
      </div>
      <div className="grid lg:grid-cols-3 gap-3 mt-3">
        <Panel title="Recent activity" subtitle="Your latest fills" className="lg:col-span-2" action={<Link to="/app/orders" className="text-xs text-cyan">All orders</Link>}>
          {!tr.data ? <Skeleton className="h-40" /> : tr.data.length === 0 ? <p className="text-sm text-muted py-6 text-center">No trades yet.</p> : (
            <ul className="divide-y divide-line/60">{tr.data.slice(0, 8).map((t) => (
              <li key={t.id} className="flex items-center gap-3 py-2 text-sm">
                <span className={`w-12 text-[11px] font-bold ${t.side === 'BUY' ? 'text-up' : 'text-down'}`}>{t.side}</span>
                <span className="font-semibold w-24 truncate">{t.symbol}</span>
                <span className="num text-muted hidden sm:inline">{Number(t.fillQuantity)} @ {Number(t.fillPrice).toFixed(2)}</span>
                <span className="flex-1" />
                {t.side === 'SELL' && <span className={`num text-xs ${tone(t.realisedPnl)}`}>{fmtINR(t.realisedPnl)}</span>}
                <span className="text-[11px] text-faint w-28 text-right">{fmtTime(t.executedAt)}</span>
              </li>))}</ul>)}
        </Panel>
        <Panel title="Upcoming NSE holidays" subtitle="From the admin-managed calendar">
          {!hol.data ? <Skeleton className="h-40" /> : hol.data.length === 0 ? <p className="text-sm text-muted">No upcoming holidays in the calendar.</p> :
            hol.data.slice(0, 6).map((h) => <div key={h.date} className="flex justify-between py-1.5 text-sm"><span>{h.description}</span><span className="num text-muted">{fmtDate(h.date)}</span></div>)}
          <p className="text-[11px] text-faint mt-3">News, corporate actions and FII/DII flows need a licensed data feed and are not configured.</p>
        </Panel>
      </div>
    </>
  );
}
