import { useState } from 'react';
import { FlaskConical, Play } from 'lucide-react';
import { api, errorMessage } from '../lib/api';
import { useApi } from '../lib/useApi';
import { fmtDate, fmtINR, fmtPct, tone } from '../lib/format';
import { Button, DataTable, EmptyState, Field, Metric, PageHeader, Panel, SimBadge, Tabs } from '../components/ui';
import LineSeriesChart from '../components/charts/LineSeriesChart';
import SymbolPicker from '../components/trade/SymbolPicker';
import { useToast } from '../context/ToastContext';

const iso = (d) => d.toISOString().slice(0, 10);

export default function StrategyLab() {
  const today = new Date(); const yearAgo = new Date(Date.now() - 365 * 864e5);
  const [f, setF] = useState({ symbol: 'NIFTY 50', strategy: 'SMA', start: iso(yearAgo), end: iso(today), capital: 1000000, fast: 20, slow: 50, rsiPeriod: 14, buyBelow: 30, sellAbove: 70 });
  const [res, setRes] = useState(null); const [busy, setBusy] = useState(false); const hist = useApi('/backtest/history'); const toast = useToast();
  const run = async () => {
    setBusy(true); setRes(null);
    try { const { data } = await api.post('/backtest', { ...f, capital: Number(f.capital), fast: Number(f.fast), slow: Number(f.slow), rsiPeriod: Number(f.rsiPeriod), buyBelow: Number(f.buyBelow), sellAbove: Number(f.sellAbove) }); setRes(data); hist.reload(); }
    catch (e) { toast.error('Backtest failed', errorMessage(e)); } finally { setBusy(false); }
  };
  const num = (k, l) => <Field label={l}><input className="input num" type="number" value={f[k]} onChange={(e) => setF({ ...f, [k]: e.target.value })} /></Field>;
  return (
    <>
      <PageHeader icon={FlaskConical} title="Strategy Lab" subtitle="Backtest on stored daily candles · next-day-open fills · 0.05% cost per side"><SimBadge /></PageHeader>
      <div className="grid xl:grid-cols-[340px_1fr] gap-3">
        <Panel title="Configure">
          <div className="space-y-3">
            <Field label="Instrument"><SymbolPicker value={f.symbol} onChange={(s) => setF({ ...f, symbol: s })} includeIndices /></Field>
            <Tabs tabs={[{ value: 'SMA', label: 'SMA crossover' }, { value: 'RSI', label: 'RSI reversal' }]} value={f.strategy} onChange={(s) => setF({ ...f, strategy: s })} />
            <div className="grid grid-cols-2 gap-3">
              <Field label="From"><input className="input" type="date" value={f.start} onChange={(e) => setF({ ...f, start: e.target.value })} /></Field>
              <Field label="To"><input className="input" type="date" value={f.end} onChange={(e) => setF({ ...f, end: e.target.value })} /></Field>
              {num('capital', 'Capital (₹)')}
              {f.strategy === 'SMA' ? <>{num('fast', 'Fast SMA')}{num('slow', 'Slow SMA')}</> : <>{num('rsiPeriod', 'RSI period')}{num('buyBelow', 'Buy when crosses up')}{num('sellAbove', 'Sell when crosses down')}</>}
            </div>
            <Button className="w-full" onClick={run} loading={busy}><Play className="w-4 h-4" />Run backtest</Button>
          </div>
        </Panel>
        <div className="space-y-3 min-w-0">
          {!res ? <Panel><EmptyState icon={FlaskConical} title={busy ? 'Running…' : 'No results yet'} body="Results appear only after the calculation completes on the server." /></Panel> : (<>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
              <Metric label="Total return" accent="up" value={<span className={`num ${tone(res.totalReturnPct)}`}>{fmtPct(res.totalReturnPct)}</span>} sub={`Buy & hold ${fmtPct(res.benchmarkReturnPct)}`} subTone={tone(res.benchmarkReturnPct)} />
              <Metric label="Win rate" accent="cyan" value={<span className="num">{res.winRatePct}%</span>} sub={`${res.tradeCount} closed trades`} />
              <Metric label="Max drawdown" accent="amber" value={<span className="num text-down">{res.maxDrawdownPct}%</span>} />
              <Metric label="Final capital" accent="violet" value={<span className="num">{fmtINR(res.finalCapital, true)}</span>} sub={`Avg win ${fmtINR(res.avgWin, true)} · loss ${fmtINR(res.avgLoss, true)}`} />
            </div>
            <Panel title={`Equity curve · ${res.params}`} subtitle="Strategy vs buy-and-hold benchmark">
              <LineSeriesChart area height={300} series={[{ name: 'Strategy', color: 'up', data: res.equityCurve.map((p) => ({ time: p.time, value: Number(p.equity) })) }, { name: 'Buy & hold', color: 'violet', dashed: true, data: res.equityCurve.map((p) => ({ time: p.time, value: Number(p.benchmark) })) }]} />
            </Panel>
            <Panel title="Trades" bodyClass="p-0 max-h-80 overflow-auto"><DataTable rows={res.trades.map((t, i) => ({ ...t, id: i }))} columns={[
              { key: 'date', label: 'Date', render: (t) => fmtDate(t.date) }, { key: 'side', label: 'Side', render: (t) => <span className={t.side === 'BUY' ? 'text-up' : 'text-down'}>{t.side}</span> },
              { key: 'price', label: 'Price', align: 'right', render: (t) => <span className="num">{fmtINR(t.price)}</span> }, { key: 'quantity', label: 'Qty', align: 'right', render: (t) => <span className="num">{t.quantity}</span> },
              { key: 'pnl', label: 'P&L', align: 'right', render: (t) => t.pnl === null ? '—' : <span className={`num ${tone(t.pnl)}`}>{fmtINR(t.pnl)}</span> }]} /></Panel>
          </>)}
          <Panel title="Strategy comparison" subtitle="Your previous runs" bodyClass="p-0">
            <DataTable rows={hist.data || []} empty={<EmptyState title="No runs yet" />} columns={[
              { key: 'symbol', label: 'Symbol' }, { key: 'params', label: 'Strategy', render: (r) => <span className="text-xs">{r.params}</span> },
              { key: 'totalReturnPct', label: 'Return', align: 'right', render: (r) => <span className={`num ${tone(r.totalReturnPct)}`}>{fmtPct(r.totalReturnPct)}</span> },
              { key: 'winRatePct', label: 'Win %', align: 'right', render: (r) => <span className="num">{Number(r.winRatePct).toFixed(1)}</span> },
              { key: 'maxDrawdownPct', label: 'Max DD', align: 'right', render: (r) => <span className="num">{Number(r.maxDrawdownPct).toFixed(2)}%</span> },
              { key: 'tradeCount', label: 'Trades', align: 'right' }]} />
          </Panel>
        </div>
      </div>
    </>
  );
}
