import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Activity, Search } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { fmtInt, fmtNum, fmtPct, tone } from '../lib/format';
import { DataTable, ErrorState, FlashPrice, PageHeader, Panel, SimBadge, Skeleton, Tabs } from '../components/ui';
import { useAllQuotesVersion, getQuote } from '../context/MarketContext';

export default function Markets() {
  const { data, error, reload } = useApi('/market/stocks');
  const v = useAllQuotesVersion(); const nav = useNavigate();
  const [q, setQ] = useState(''); const [view, setView] = useState('table'); const [sector, setSector] = useState('All');
  const rows = useMemo(() => (data || []).filter((s) => !s.isIndex).map((s) => ({ ...s, ...(getQuote(s.symbol) || {}) })), [data, v]); // eslint-disable-line
  const sectors = ['All', ...new Set(rows.map((r) => r.sector))];
  const filtered = rows.filter((r) => (sector === 'All' || r.sector === sector) && `${r.symbol} ${r.name}`.toLowerCase().includes(q.toLowerCase()));
  const heat = useMemo(() => { const m = {}; filtered.forEach((r) => { (m[r.sector] ||= []).push(r); }); return m; }, [filtered]);
  return (
    <>
      <PageHeader icon={Activity} title="Markets" subtitle="Nifty 50 constituents with live-streamed quotes"><SimBadge /></PageHeader>
      <div className="flex flex-wrap gap-2 mb-3 items-center">
        <div className="relative w-full sm:w-64"><Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-faint" /><input className="input pl-9" placeholder="Filter…" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Filter stocks" /></div>
        <select className="input w-auto" value={sector} onChange={(e) => setSector(e.target.value)} aria-label="Sector">{sectors.map((s) => <option key={s}>{s}</option>)}</select>
        <div className="flex-1" /><Tabs tabs={[{ value: 'table', label: 'Table' }, { value: 'heat', label: 'Heatmap' }]} value={view} onChange={setView} />
      </div>
      {error ? <ErrorState message={error} onRetry={reload} /> : !data ? <Skeleton className="h-96" /> : view === 'table' ? (
        <Panel bodyClass="p-0">
          <DataTable rows={filtered} onRowClick={(r) => nav(`/app/charts?symbol=${encodeURIComponent(r.symbol)}`)} initialSort={{ key: 'changePct', dir: 'desc' }} columns={[
            { key: 'symbol', label: 'Symbol', render: (r) => <div><div className="font-semibold">{r.symbol}</div><div className="text-[11px] text-faint">{r.name}</div></div> },
            { key: 'sector', label: 'Sector', render: (r) => <span className="text-muted text-xs">{r.sector}</span> },
            { key: 'ltp', label: 'LTP', align: 'right', render: (r) => <FlashPrice value={Number(r.ltp)} format={fmtNum} /> },
            { key: 'changePct', label: 'Change', align: 'right', render: (r) => <span className={`num ${tone(r.changePct)}`}>{fmtPct(r.changePct)}</span> },
            { key: 'high', label: 'High', align: 'right', render: (r) => <span className="num text-muted">{fmtNum(r.high)}</span> },
            { key: 'low', label: 'Low', align: 'right', render: (r) => <span className="num text-muted">{fmtNum(r.low)}</span> },
            { key: 'volume', label: 'Volume', align: 'right', render: (r) => <span className="num text-muted">{fmtInt(r.volume)}</span> },
          ]} />
        </Panel>
      ) : (
        <div className="grid sm:grid-cols-2 xl:grid-cols-3 gap-3">{Object.entries(heat).map(([sec, list]) => (
          <Panel key={sec} title={sec} bodyClass="p-2 grid grid-cols-3 gap-1.5">
            {list.map((r) => { const c = Number(r.changePct) || 0; const a = Math.min(0.85, 0.15 + Math.abs(c) / 3);
              return <button key={r.symbol} onClick={() => nav(`/app/charts?symbol=${encodeURIComponent(r.symbol)}`)} className="rounded-md p-2 text-left transition hover:scale-[1.03]" style={{ background: `rgb(var(--${c >= 0 ? 'up' : 'down'}) / ${a})` }}>
                <div className="text-[11px] font-bold truncate text-white drop-shadow">{r.symbol}</div><div className="num text-[10px] text-white/90">{fmtPct(c)}</div></button>; })}
          </Panel>))}</div>
      )}
    </>
  );
}
