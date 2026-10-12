import { useEffect, useMemo, useState } from 'react';
import { Search } from 'lucide-react';
import { api } from '../../lib/api';

/** Searchable symbol selector backed by /api/market/stocks. */
export default function SymbolPicker({ value, onChange, includeIndices = false, className = '' }) {
  const [stocks, setStocks] = useState([]); const [q, setQ] = useState(''); const [open, setOpen] = useState(false);
  useEffect(() => { api.get('/market/stocks').then(({ data }) => setStocks(data)).catch(() => {}); }, []);
  const list = useMemo(() => stocks.filter((s) => (includeIndices || !s.isIndex) && (`${s.symbol} ${s.name}`).toLowerCase().includes(q.toLowerCase())).slice(0, 50), [stocks, q, includeIndices]);
  return (
    <div className={`relative ${className}`}>
      <div className="relative">
        <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-faint" />
        <input className="input pl-9" value={open ? q : value || ''} placeholder="Search symbol…" aria-label="Symbol"
          onFocus={() => { setOpen(true); setQ(''); }} onBlur={() => setTimeout(() => setOpen(false), 150)} onChange={(e) => setQ(e.target.value)} />
      </div>
      {open && (
        <ul className="absolute z-30 mt-1 w-full max-h-72 overflow-auto card p-1">
          {list.map((s) => (
            <li key={s.symbol}><button onMouseDown={() => { onChange(s.symbol); setOpen(false); }} className="w-full flex justify-between gap-2 px-3 py-2 rounded-md hover:bg-raised text-left">
              <span><span className="text-sm font-semibold">{s.symbol}</span> <span className="text-xs text-muted">{s.name}</span></span>
              <span className="text-[11px] text-faint">{s.sector}</span></button></li>))}
          {list.length === 0 && <li className="text-xs text-muted p-3">No match</li>}
        </ul>)}
    </div>
  );
}
