import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { RotateCcw, Trophy, Wallet } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtINR, fmtPct, tone } from '../lib/format';
import { AnimatedNumber, Badge, Button, DataTable, EmptyState, ErrorState, Metric, Modal, PageHeader, Panel, Skeleton, Tabs } from '../components/ui';
import LineSeriesChart from '../components/charts/LineSeriesChart';
import { useToast } from '../context/ToastContext';
import { useAuth } from '../context/AuthContext';

const PALETTE = ['cyan', 'up', 'violet', 'amber', 'down'];

function Donut({ items }) {
  const total = items.reduce((a, b) => a + Number(b.value), 0) || 1; let acc = 0;
  return (
    <div className="flex items-center gap-5">
      <svg viewBox="0 0 42 42" className="w-36 h-36 -rotate-90" role="img" aria-label="Sector allocation">
        <circle cx="21" cy="21" r="15.9" fill="none" stroke="rgb(var(--raised))" strokeWidth="5" />
        {items.map((s, i) => { const pct = (Number(s.value) / total) * 100; const el = <circle key={s.sector} cx="21" cy="21" r="15.9" fill="none" stroke={`rgb(var(--${PALETTE[i % 5]}))`} strokeWidth="5" strokeDasharray={`${pct} ${100 - pct}`} strokeDashoffset={-acc} />; acc += pct; return el; })}
      </svg>
      <ul className="space-y-1.5 text-xs flex-1">{items.map((s, i) => <li key={s.sector} className="flex items-center gap-2"><span className={`w-2.5 h-2.5 rounded-sm bg-${PALETTE[i % 5]}`} /><span className="flex-1 truncate">{s.sector}</span><span className="num text-muted">{Number(s.pct).toFixed(1)}%</span></li>)}</ul>
    </div>
  );
}

export default function Portfolio() {
  const pf = useApi('/portfolio', { poll: 5000 }); const lb = useApi('/portfolio/leaderboard'); const toast = useToast(); const { user } = useAuth();
  const nav = useNavigate(); const [tab, setTab] = useState('holdings'); const [confirm, setConfirm] = useState(false); const p = pf.data;
  const series = useMemo(() => {
    const s = p?.performance?.series || [];
    const toPts = (k) => { const seen = new Set(); return s.map((x) => ({ time: Math.floor(new Date(x.time + '+05:30').getTime() / 1000), value: Number(x[k]) })).filter((x) => !seen.has(x.time) && seen.add(x.time)); };
    return [{ name: 'Portfolio', color: 'up', data: toPts('portfolio') }, { name: 'Nifty 50', color: 'violet', dashed: true, data: toPts('nifty') }];
  }, [p?.performance?.series?.length]); // eslint-disable-line
  const reset = async () => { try { await api.post('/portfolio/reset'); setConfirm(false); pf.reload(); toast.success('Wallet reset', 'Your balance is back to ₹10,00,000'); } catch (e) { toast.error('Reset failed', errorMessage(e)); } };

  if (pf.error) return <ErrorState message={pf.error} onRetry={pf.reload} />;
  return (
    <>
      <PageHeader icon={Wallet} title="Portfolio" subtitle="Every number is calculated from your recorded trades"><Button variant="ghost" onClick={() => setConfirm(true)}><RotateCcw className="w-4 h-4" />Reset wallet</Button></PageHeader>
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        <Metric label="Total value" accent="up" value={p ? <AnimatedNumber value={p.totalValue} format={(v) => fmtINR(v)} /> : <Skeleton className="h-6 w-28" />} sub={p && `${fmtPct(p.overallReturnPct)} overall`} subTone={tone(p?.overallReturnPct)} />
        <Metric label="Invested" accent="cyan" value={p ? <span className="num">{fmtINR(p.invested)}</span> : <Skeleton className="h-6" />} sub={p && `Cash ${fmtINR(p.cash, true)}`} />
        <Metric label="Unrealised / Realised" accent="violet" value={p ? <span className={`num ${tone(p.unrealisedPnl)}`}>{fmtINR(p.unrealisedPnl)}</span> : <Skeleton className="h-6" />} sub={p && fmtINR(p.realisedPnl)} subTone={tone(p?.realisedPnl)} />
        <Metric label="Win rate · Drawdown" accent="amber" value={p ? <span className="num">{p.winRatePct === null ? '—' : `${p.winRatePct}%`}</span> : <Skeleton className="h-6" />} sub={p && `Max DD ${p.performance.maxDrawdownPct}% · charges ${fmtINR(p.chargesPaid, true)}`} />
      </div>
      <div className="grid lg:grid-cols-[1fr_360px] gap-3 mt-3">
        <Panel title="Performance vs Nifty 50" subtitle="Both rebased to 100 · snapshots every 5 minutes">
          {!p ? <Skeleton className="h-64" /> : series[0].data.length < 2 ? <EmptyState title="Building your history" body="Performance snapshots are recorded every 5 minutes. Check back shortly." /> : <LineSeriesChart series={series} height={260} />}
          {p && <div className="flex gap-4 mt-3 text-xs text-muted"><span>Volatility <b className="num text-ink">{p.performance.volatilityPct}%</b></span><span>Risk-adjusted score <b className="num text-ink">{p.performance.riskAdjustedScore}</b></span></div>}
        </Panel>
        <Panel title="Allocation by sector">{!p ? <Skeleton className="h-40" /> : p.allocationBySector.length === 0 ? <EmptyState title="Nothing invested" /> : <Donut items={p.allocationBySector} />}</Panel>
      </div>
      <div className="mt-3"><Tabs tabs={[{ value: 'holdings', label: 'Holdings' }, { value: 'leaderboard', label: 'Leaderboard' }]} value={tab} onChange={setTab} /></div>
      <Panel className="mt-3" bodyClass="p-0">
        {tab === 'holdings' ? (!p ? <Skeleton className="h-40 m-3" /> : (
          <DataTable rows={p.holdings} empty={<EmptyState title="No holdings" body="Buy a stock from Paper Trading to see it here." />} columns={[
            { key: 'symbol', label: 'Symbol', render: (h) => <div><div className="font-semibold">{h.symbol}</div><div className="text-[11px] text-faint">{h.sector}</div></div> },
            { key: 'quantity', label: 'Qty', align: 'right', render: (h) => <span className="num">{Number(h.quantity)}</span> },
            { key: 'avgPrice', label: 'Avg price', align: 'right', render: (h) => <span className="num">{fmtINR(h.avgPrice)}</span> },
            { key: 'ltp', label: 'LTP', align: 'right', render: (h) => <span className="num">{fmtINR(h.ltp)}</span> },
            { key: 'invested', label: 'Invested', align: 'right', render: (h) => <span className="num text-muted">{fmtINR(h.invested)}</span> },
            { key: 'marketValue', label: 'Market value', align: 'right', render: (h) => <span className="num">{fmtINR(h.marketValue)}</span> },
            { key: 'unrealisedPnl', label: 'P&L', align: 'right', render: (h) => <span className={`num ${tone(h.unrealisedPnl)}`}>{fmtINR(h.unrealisedPnl)} <span className="text-[11px]">({fmtPct(h.unrealisedPct)})</span></span> },
            { key: 'act', label: '', render: (h) => <div className="flex gap-1 justify-end">
              <Button variant="ghost" className="!py-1 !px-2.5 text-xs !text-up" onClick={() => nav(`/app/trade?symbol=${encodeURIComponent(h.symbol)}&side=BUY`)}>Buy</Button>
              <Button variant="ghost" className="!py-1 !px-2.5 text-xs !text-down" onClick={() => nav(`/app/trade?symbol=${encodeURIComponent(h.symbol)}&side=SELL`)}>Sell</Button></div> },
          ]} />)) : (!lb.data ? <Skeleton className="h-40 m-3" /> : (
          <DataTable rows={lb.data} columns={[
            { key: 'rank', label: 'Rank', render: (r) => <span className="flex items-center gap-1.5 num">{r.rank <= 3 && <Trophy className={`w-3.5 h-3.5 ${['text-amber', 'text-muted', 'text-down'][r.rank - 1]}`} />}{r.rank}</span> },
            { key: 'name', label: 'Trader', render: (r) => <span className="font-medium">{r.name} {r.userId === user?.id && <Badge tone="cyan">You</Badge>}</span> },
            { key: 'score', label: 'Risk-adj. score', align: 'right', render: (r) => <span className="num font-semibold">{Number(r.score).toFixed(2)}</span> },
            { key: 'returnPct', label: 'Return', align: 'right', render: (r) => <span className={`num ${tone(r.returnPct)}`}>{fmtPct(r.returnPct)}</span> },
            { key: 'maxDrawdownPct', label: 'Max DD', align: 'right', render: (r) => <span className="num text-muted">{r.maxDrawdownPct}%</span> },
            { key: 'trades', label: 'Trades', align: 'right', render: (r) => <span className="num">{r.trades}</span> },
          ]} />))}
      </Panel>
      <Modal open={confirm} onClose={() => setConfirm(false)} title="Reset virtual wallet?">
        <p className="text-sm text-muted">Your cash returns to ₹10,00,000 and all holdings are cleared. Order history and journal entries are kept.</p>
        <div className="flex gap-2 mt-5"><Button variant="ghost" className="flex-1" onClick={() => setConfirm(false)}>Cancel</Button><Button variant="danger" className="flex-1" onClick={reset}>Reset wallet</Button></div>
      </Modal>
    </>
  );
}
