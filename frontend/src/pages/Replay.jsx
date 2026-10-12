import { useEffect, useRef, useState } from 'react';
import { createChart } from 'lightweight-charts';
import { History, Pause, Play, RotateCcw, SkipForward } from 'lucide-react';
import { api, errorMessage } from '../lib/api';
import { fmtINR, fmtPct, tone } from '../lib/format';
import { Badge, Button, EmptyState, PageHeader, Panel, SimBadge, Skeleton, Tabs } from '../components/ui';
import SymbolPicker from '../components/trade/SymbolPicker';
import { chartTheme, cssColor } from '../components/charts/themeColors';
import { useTheme } from '../context/ThemeContext';
import { useToast } from '../context/ToastContext';

/** Historical replay (FR-45): trade a past session bar-by-bar. Replay trades are sandboxed and never touch the wallet. */
export default function Replay() {
  const [symbol, setSymbol] = useState('NIFTY 50'); const [days, setDays] = useState([]); const [date, setDate] = useState('');
  const [bars, setBars] = useState(null); const [idx, setIdx] = useState(1); const [playing, setPlaying] = useState(false); const [speed, setSpeed] = useState('2');
  const [pos, setPos] = useState(null); const [log, setLog] = useState([]); const [qty, setQty] = useState(10);
  const box = useRef(null); const series = useRef(null); const { theme } = useTheme(); const toast = useToast();

  useEffect(() => { api.get(`/market/replay-days/${encodeURIComponent(symbol)}`).then(({ data }) => { setDays(data); setDate(data[0]?.date || ''); }).catch(() => setDays([])); }, [symbol]);
  useEffect(() => {
    if (!date) return; setBars(null); setPlaying(false); setIdx(1); setPos(null); setLog([]);
    api.get(`/market/replay/${encodeURIComponent(symbol)}`, { params: { date } }).then(({ data }) => setBars(data.candles)).catch((e) => toast.error('Replay unavailable', errorMessage(e)));
  }, [symbol, date]); // eslint-disable-line

  useEffect(() => {
    if (!box.current || !bars) return;
    const chart = createChart(box.current, { ...chartTheme(), height: 420, autoSize: true });
    const s = chart.addCandlestickSeries({ upColor: cssColor('up'), downColor: cssColor('down'), borderUpColor: cssColor('up'), borderDownColor: cssColor('down'), wickUpColor: cssColor('up'), wickDownColor: cssColor('down') });
    s.setData(bars.slice(0, idx).map((c) => ({ time: c.time, open: +c.open, high: +c.high, low: +c.low, close: +c.close })));
    chart.timeScale().fitContent(); series.current = s;
    return () => chart.remove();
  }, [bars, theme]); // eslint-disable-line

  useEffect(() => { if (series.current && bars && idx <= bars.length) { const c = bars[idx - 1]; series.current.update({ time: c.time, open: +c.open, high: +c.high, low: +c.low, close: +c.close }); } }, [idx, bars]);
  useEffect(() => {
    if (!playing || !bars) return;
    const t = setInterval(() => setIdx((i) => { if (i >= bars.length) { setPlaying(false); return i; } return i + 1; }), 1000 / Number(speed));
    return () => clearInterval(t);
  }, [playing, speed, bars]);

  const price = bars ? Number(bars[Math.min(idx, bars.length) - 1].close) : 0;
  const time = bars ? new Date(bars[idx - 1].time * 1000).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kolkata' }) : '';
  const buy = () => { if (pos) return; setPos({ entry: price, qty: Number(qty) }); setLog((l) => [...l, { t: time, side: 'BUY', price, qty: Number(qty) }]); };
  const sell = () => { if (!pos) return; const pnl = (price - pos.entry) * pos.qty; setLog((l) => [...l, { t: time, side: 'SELL', price, qty: pos.qty, pnl }]); setPos(null); };
  const realised = log.filter((x) => x.pnl !== undefined).reduce((a, b) => a + b.pnl, 0);
  const open = pos ? (price - pos.entry) * pos.qty : 0;

  return (
    <>
      <PageHeader icon={History} title="Historical Replay" subtitle="Relive a volatile session bar by bar and practise decisions"><SimBadge /></PageHeader>
      <div className="flex flex-wrap gap-2 items-end mb-3">
        <SymbolPicker value={symbol} onChange={setSymbol} includeIndices className="w-56" />
        <select className="input w-auto" value={date} onChange={(e) => setDate(e.target.value)} aria-label="Replay day">
          {days.map((d) => <option key={d.date} value={d.date}>{d.date} · {Number(d.changePct) > 0 ? '+' : ''}{d.changePct}%</option>)}
        </select>
        <span className="text-xs text-faint">Most volatile days are listed first</span>
      </div>
      <div className="grid xl:grid-cols-[1fr_320px] gap-3">
        <section className="card p-2 min-w-0">
          <div className="flex flex-wrap items-center gap-2 p-2 border-b border-line mb-2">
            <Button variant="ghost" className="!px-3" onClick={() => setPlaying((p) => !p)} disabled={!bars}>{playing ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}{playing ? 'Pause' : 'Play'}</Button>
            <Button variant="ghost" className="!px-3" onClick={() => setIdx((i) => Math.min(bars.length, i + 1))} disabled={!bars} aria-label="Step"><SkipForward className="w-4 h-4" /></Button>
            <Button variant="ghost" className="!px-3" onClick={() => { setIdx(1); setPlaying(false); setPos(null); setLog([]); setBars((b) => b && [...b]); }} disabled={!bars} aria-label="Restart"><RotateCcw className="w-4 h-4" /></Button>
            <Tabs tabs={[{ value: '1', label: '1×' }, { value: '2', label: '2×' }, { value: '5', label: '5×' }, { value: '10', label: '10×' }]} value={speed} onChange={setSpeed} />
            <div className="flex-1" /><Badge tone="cyan">{time} IST · bar {idx}/{bars?.length || 0}</Badge>
          </div>
          {!date ? <EmptyState title="No historical data" /> : !bars ? <Skeleton className="h-[420px]" /> : <div ref={box} style={{ height: 420 }} />}
          {bars && <div className="h-1 bg-raised rounded mx-2 mt-2"><div className="h-full bg-cyan rounded transition-all" style={{ width: `${(idx / bars.length) * 100}%` }} /></div>}
        </section>
        <Panel title="Replay trading" subtitle="Sandbox — does not affect your wallet">
          <div className="text-center mb-4"><div className="label">Replay price</div><div className="num text-2xl font-bold mt-1">{fmtINR(price)}</div></div>
          <label className="label block mb-1.5">Quantity</label><input className="input num mb-3" type="number" min="1" value={qty} onChange={(e) => setQty(e.target.value)} />
          <div className="grid grid-cols-2 gap-2"><Button onClick={buy} disabled={!bars || !!pos}>Buy</Button><Button variant="danger" onClick={sell} disabled={!pos}>Sell</Button></div>
          <div className="mt-4 space-y-1.5 text-sm">
            <div className="flex justify-between"><span className="text-muted">Open P&L</span><span className={`num ${tone(open)}`}>{fmtINR(open)}</span></div>
            <div className="flex justify-between"><span className="text-muted">Realised P&L</span><span className={`num ${tone(realised)}`}>{fmtINR(realised)}</span></div>
            {pos && <div className="flex justify-between"><span className="text-muted">Entry</span><span className="num">{fmtINR(pos.entry)} ({fmtPct(((price - pos.entry) / pos.entry) * 100)})</span></div>}
          </div>
          <ul className="mt-4 max-h-48 overflow-auto text-xs space-y-1">{log.map((l, i) => <li key={i} className="flex justify-between border-b border-line/60 py-1"><span>{l.t} <b className={l.side === 'BUY' ? 'text-up' : 'text-down'}>{l.side}</b> {l.qty}</span><span className="num">{fmtINR(l.price)}{l.pnl !== undefined && <span className={`ml-2 ${tone(l.pnl)}`}>{fmtINR(l.pnl)}</span>}</span></li>)}</ul>
        </Panel>
      </div>
      <p className="text-[11px] text-faint mt-3">Intraday bars are reconstructed deterministically from each stored daily candle (open, high, low and close are honoured); the path between them is simulated.</p>
    </>
  );
}
