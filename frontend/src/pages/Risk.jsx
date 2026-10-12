import { useEffect, useState } from 'react';
import { AlertTriangle, Calculator, ShieldCheck } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtINR } from '../lib/format';
import { EmptyState, ErrorState, Field, Metric, PageHeader, Panel, Skeleton } from '../components/ui';

function PositionSizer() {
  const [f, setF] = useState({ capital: 1000000, riskPct: 1, entry: 2950, stop: 2890, target: 3100 }); const [r, setR] = useState(null); const [err, setErr] = useState(null);
  useEffect(() => {
    const t = setTimeout(() => api.get('/risk/position-size', { params: f }).then(({ data }) => { setR(data); setErr(null); }).catch((e) => setErr(errorMessage(e))), 250);
    return () => clearTimeout(t);
  }, [f]);
  const inp = (k, label, step = 1) => <Field label={label}><input className="input num" type="number" step={step} value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} /></Field>;
  return (
    <Panel title="Position-size & risk:reward calculator" subtitle="Shares such that (entry − stop) × shares = capital × risk%" action={<Calculator className="w-4 h-4 text-cyan" />}>
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">{inp('capital', 'Capital (₹)', 1000)}{inp('riskPct', 'Risk per trade %', 0.25)}{inp('entry', 'Entry', 0.05)}{inp('stop', 'Stop-loss', 0.05)}{inp('target', 'Target', 0.05)}</div>
      {err ? <p className="text-sm text-down mt-3">{err}</p> : r && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mt-4">
          <div className="rounded-lg bg-raised p-3"><div className="label">Shares</div><div className="num text-xl font-semibold mt-1">{r.shares}</div></div>
          <div className="rounded-lg bg-raised p-3"><div className="label">Position value</div><div className="num text-lg font-semibold mt-1">{fmtINR(r.positionValue, true)}</div><div className="text-[11px] text-faint">{r.positionPctOfCapital}% of capital</div></div>
          <div className="rounded-lg bg-raised p-3"><div className="label">Max loss</div><div className="num text-lg font-semibold mt-1 text-down">{fmtINR(r.maxLoss, true)}</div><div className="text-[11px] text-faint">risk/share {fmtINR(r.riskPerShare)}</div></div>
          <div className="rounded-lg bg-raised p-3"><div className="label">Risk : reward</div><div className="num text-lg font-semibold mt-1">1 : {r.riskReward ?? '—'}</div><div className="text-[11px] text-up">max gain {fmtINR(r.maxGain, true)}</div></div>
        </div>)}
    </Panel>
  );
}

export default function Risk() {
  const { data, error, reload } = useApi('/risk', { poll: 10000 });
  return (
    <>
      <PageHeader icon={ShieldCheck} title="Risk Analytics" subtitle="Concentration, exposure and pre-trade safety checks" />
      {error ? <ErrorState message={error} onRetry={reload} /> : !data ? <Skeleton className="h-60" /> : (<>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <Metric label="Total value" value={<span className="num">{fmtINR(data.totalValue, true)}</span>} />
          <Metric label="Invested" accent="violet" value={<span className="num">{Number(data.investedPct).toFixed(1)}%</span>} />
          <Metric label="Cash buffer" accent="up" value={<span className="num">{Number(data.cashPct).toFixed(1)}%</span>} />
          <Metric label="Active warnings" accent="amber" value={<span className="num">{data.alerts.length}</span>} />
        </div>
        {data.alerts.length > 0 && <div className="mt-3 space-y-2">{data.alerts.map((a) => <div key={a} className="rounded-lg border border-amber/30 bg-amber/10 text-amber text-sm px-3 py-2 flex gap-2"><AlertTriangle className="w-4 h-4 shrink-0" />{a}</div>)}</div>}
        <div className="grid lg:grid-cols-2 gap-3 mt-3">
          <Panel title="Sector exposure" subtitle="Warning above 40%">{data.sectors.length === 0 ? <EmptyState title="No exposure" body="You hold only cash." /> : data.sectors.map((s) => (
            <div key={s.sector} className="mb-3"><div className="flex justify-between text-xs"><span>{s.sector}</span><span className="num">{Number(s.pct).toFixed(1)}% · {fmtINR(s.value, true)}</span></div>
              <div className="h-2 bg-raised rounded mt-1 relative"><div className={`h-full rounded ${Number(s.pct) > 40 ? 'bg-down' : 'bg-cyan'}`} style={{ width: `${Math.min(100, s.pct)}%` }} /><div className="absolute top-0 bottom-0 w-px bg-amber" style={{ left: '40%' }} /></div></div>))}</Panel>
          <Panel title="Position concentration" subtitle="Warning above 30% · circuit band per stock">{data.positions.length === 0 ? <EmptyState title="No positions" /> : data.positions.map((p) => (
            <div key={p.symbol} className="mb-3"><div className="flex justify-between text-xs"><span className="font-semibold">{p.symbol} <span className="text-faint font-normal">±{Number(p.circuitLimitPct)}% circuit</span></span><span className="num">{Number(p.pct).toFixed(1)}%</span></div>
              <div className="h-2 bg-raised rounded mt-1"><div className={`h-full rounded ${Number(p.pct) > 30 ? 'bg-down' : 'bg-violet'}`} style={{ width: `${Math.min(100, p.pct)}%` }} /></div></div>))}</Panel>
        </div>
      </>)}
      <div className="mt-3"><PositionSizer /></div>
      <Panel title="Automatic order safety checks" className="mt-3">
        <ul className="grid md:grid-cols-2 gap-2 text-sm text-muted">
          {['Fat-finger: limit price > 20% away from LTP needs explicit confirmation', 'Quantity above 1,00,000 is rejected; above 5,000 is flagged', 'Limit price outside the circuit band is rejected', 'Single order > 25% of capital is flagged', 'Buy without a stop-loss is flagged', 'Sector concentration above 40% after the trade is flagged'].map((t) => <li key={t} className="flex gap-2"><ShieldCheck className="w-4 h-4 text-up shrink-0" />{t}</li>)}
        </ul>
        <p className="text-xs text-faint mt-3">Liquidity and pump-and-dump warnings require order-book depth data, which is not configured in this build.</p>
      </Panel>
    </>
  );
}
