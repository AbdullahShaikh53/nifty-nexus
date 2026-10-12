import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { CandlestickChart, Maximize2, Minimize2, Star } from 'lucide-react';
import { api, errorMessage } from '../lib/api';
import { fmtTime } from '../lib/format';
import { Badge, ErrorState, PageHeader, Panel, SimBadge, Skeleton, Tabs } from '../components/ui';
import PriceChart from '../components/charts/PriceChart';
import SymbolPicker from '../components/trade/SymbolPicker';
import TradeTicket from '../components/trade/TradeTicket';
import { WatchRow } from './Watchlists';
import { useApi } from '../lib/useApi';
import { useMarket, useQuote } from '../context/MarketContext';
import { useToast } from '../context/ToastContext';

/** Charting terminal: watchlist | chart | compact trade panel (prompt section 6). */
export default function Charts() {
  const [params, setParams] = useSearchParams();
  const symbol = params.get('symbol') || 'RELIANCE';
  const [tf, setTf] = useState('1D'); const [mode, setMode] = useState('candles'); const [sma, setSma] = useState('20,50'); const [rsi, setRsi] = useState(true);
  const [data, setData] = useState(null); const [err, setErr] = useState(null); const [full, setFull] = useState(false);
  const wl = useApi('/watchlists'); const { status, connected } = useMarket(); const q = useQuote(symbol); const toast = useToast();

  useEffect(() => {
    let alive = true; setData(null); setErr(null);
    api.get(`/market/chart/${encodeURIComponent(symbol)}`, { params: { timeframe: tf, sma, rsi: true } }).then(({ data: d }) => alive && setData(d)).catch((e) => alive && setErr(errorMessage(e)));
    return () => { alive = false; };
  }, [symbol, tf, sma]);

  useEffect(() => { const h = (e) => e.key === 'Escape' && setFull(false); window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h); }, []);
  const addToWatch = async () => { const first = wl.data?.[0]; if (!first) return; try { await api.post(`/watchlists/${first.id}/items`, { symbol }); wl.reload(); toast.success('Added to watchlist', symbol); } catch (e) { toast.error('Failed', errorMessage(e)); } };
  const stale = !connected || status?.stale;

  return (
    <>
      <PageHeader icon={CandlestickChart} title="Charts" subtitle="Interactive candlesticks with SMA, RSI and live updates"><SimBadge /></PageHeader>
      <div className="grid xl:grid-cols-[260px_1fr_320px] gap-3">
        <Panel title="Watchlist" bodyClass="p-0 max-h-[640px] overflow-auto" className="order-2 xl:order-1">
          {!wl.data ? <Skeleton className="h-60 m-3" /> : (wl.data[0]?.items || []).map((i) => <WatchRow key={i.symbol} symbol={i.symbol} compact active={i.symbol === symbol} onSelect={() => setParams({ symbol: i.symbol })} />)}
        </Panel>
        <section className={`card order-1 xl:order-2 min-w-0 ${full ? 'fixed inset-2 z-[60] overflow-auto' : ''}`}>
          <div className="flex flex-wrap items-center gap-2 p-3 border-b border-line">
            <SymbolPicker value={symbol} onChange={(s) => setParams({ symbol: s })} includeIndices className="w-48" />
            <Tabs tabs={['1D', '1W', '1M']} value={tf} onChange={setTf} />
            <Tabs tabs={[{ value: 'candles', label: 'Candles' }, { value: 'line', label: 'Line' }, { value: 'area', label: 'Area' }]} value={mode} onChange={setMode} />
            <select className="input w-auto !py-1.5 text-xs" value={sma} onChange={(e) => setSma(e.target.value)} aria-label="SMA overlays">
              <option value="20,50">SMA 20 / 50</option><option value="9,21">SMA 9 / 21</option><option value="50,200">SMA 50 / 200</option><option value="">No SMA</option></select>
            <label className="text-xs flex items-center gap-1.5"><input type="checkbox" checked={rsi} onChange={(e) => setRsi(e.target.checked)} />RSI</label>
            <div className="flex-1" />
            <Badge tone={stale ? 'down' : 'cyan'} dot={!stale}>{stale ? 'Stale' : 'Updating'} · {q ? fmtTime(q.tickTime) : '—'}</Badge>
            <button onClick={addToWatch} className="p-2 rounded-lg hover:bg-raised text-muted" aria-label="Add to watchlist" title="Add to watchlist"><Star className="w-4 h-4" /></button>
            <button onClick={() => setFull((f) => !f)} className="p-2 rounded-lg hover:bg-raised text-muted" aria-label="Full screen">{full ? <Minimize2 className="w-4 h-4" /> : <Maximize2 className="w-4 h-4" />}</button>
          </div>
          <div className="p-2">
            {err ? <ErrorState message={err} /> : !data ? <div className="p-3 space-y-2"><Skeleton className="h-[340px]" /><Skeleton className="h-[100px]" /></div> :
              <PriceChart data={data} mode={mode} showRsi={rsi} symbol={symbol} height={full ? window.innerHeight - 120 : window.innerWidth < 640 ? 380 : 520} />}
          </div>
          <p className="px-4 pb-3 text-[11px] text-faint">Historical candles are simulated for offline use; the latest candle updates from the live quote stream. Scroll to zoom, drag to pan.</p>
        </section>
        <Panel title="Trade" className="order-3"><TradeTicket symbol={symbol} compact /></Panel>
      </div>
    </>
  );
}
