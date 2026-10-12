import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { CornerDownLeft, Search } from 'lucide-react';
import { NAV } from './nav';
import { api } from '../../lib/api';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';

/** Ctrl/Cmd+K palette: pages, stocks and actions. */
export default function CommandPalette({ open, setOpen }) {
  const [q, setQ] = useState(''); const [stocks, setStocks] = useState([]); const [idx, setIdx] = useState(0);
  const nav = useNavigate(); const { isAdmin, logout } = useAuth(); const { toggle } = useTheme(); const input = useRef(null);

  useEffect(() => { if (open) { setQ(''); setIdx(0); setTimeout(() => input.current?.focus(), 30); if (!stocks.length) api.get('/market/stocks').then(({ data }) => setStocks(data)).catch(() => {}); } }, [open]); // eslint-disable-line

  const items = useMemo(() => {
    const s = q.toLowerCase();
    const pages = NAV.filter((n) => !n.admin || isAdmin).map((n) => ({ kind: 'Page', label: n.label, icon: n.icon, run: () => nav(n.to) }));
    const actions = [
      { kind: 'Action', label: 'Toggle theme', run: toggle },
      { kind: 'Action', label: 'Quick trade', run: () => nav('/app/trade') },
      { kind: 'Action', label: 'Behavioural insights', run: () => nav('/app/insights') },
      { kind: 'Action', label: 'Log out', run: logout },
    ];
    const st = stocks.filter((x) => !x.isIndex).map((x) => ({ kind: 'Stock', label: x.symbol, sub: x.name, run: () => nav(`/app/charts?symbol=${encodeURIComponent(x.symbol)}`) }));
    const all = [...pages, ...actions, ...st];
    return (s ? all.filter((i) => i.label.toLowerCase().includes(s) || i.sub?.toLowerCase().includes(s)) : [...pages, ...actions]).slice(0, 12);
  }, [q, stocks, isAdmin, nav, toggle, logout]);

  const run = (i) => { i?.run(); setOpen(false); };
  const onKey = (e) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setIdx((x) => Math.min(x + 1, items.length - 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); setIdx((x) => Math.max(x - 1, 0)); }
    if (e.key === 'Enter') run(items[idx]);
    if (e.key === 'Escape') setOpen(false);
  };

  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-[90] flex items-start justify-center pt-[12vh] px-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <div className="absolute inset-0 bg-black/55" onClick={() => setOpen(false)} />
          <motion.div initial={{ y: -12, scale: 0.98 }} animate={{ y: 0, scale: 1 }} exit={{ y: -8, opacity: 0 }} transition={{ type: 'spring', stiffness: 420, damping: 34 }}
            className="relative w-full max-w-xl card overflow-hidden" role="dialog" aria-label="Command palette">
            <div className="flex items-center gap-3 px-4 border-b border-line">
              <Search className="w-4 h-4 text-faint" />
              <input ref={input} value={q} onChange={(e) => { setQ(e.target.value); setIdx(0); }} onKeyDown={onKey} placeholder="Search pages, stocks, actions…"
                className="flex-1 bg-transparent py-3.5 text-sm outline-none placeholder:text-faint" aria-label="Search" />
              <kbd className="text-[10px] text-faint border border-line rounded px-1.5 py-0.5">ESC</kbd>
            </div>
            <ul className="max-h-[50vh] overflow-auto p-1.5">
              {items.length === 0 && <li className="text-sm text-muted p-4 text-center">No results</li>}
              {items.map((i, k) => (
                <li key={i.kind + i.label}>
                  <button onMouseEnter={() => setIdx(k)} onClick={() => run(i)} className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-left text-sm ${k === idx ? 'bg-raised' : ''}`}>
                    <span className="text-[10px] uppercase tracking-wider text-faint w-12">{i.kind}</span>
                    <span className="font-medium">{i.label}</span>
                    {i.sub && <span className="text-xs text-muted truncate">{i.sub}</span>}
                    {k === idx && <CornerDownLeft className="w-3.5 h-3.5 ml-auto text-faint" />}
                  </button>
                </li>
              ))}
            </ul>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
