import { forwardRef, useEffect, useRef, useState } from 'react';
import { AnimatePresence, animate, motion, useReducedMotion } from 'framer-motion';
import { AlertTriangle, Inbox, Loader2, X } from 'lucide-react';
import { tone } from '../../lib/format';

/* ---------- Primitive building blocks of the Nifty Nexus design system ---------- */

export const Card = ({ className = '', children, ...p }) => <div className={`card ${className}`} {...p}>{children}</div>;

export function Panel({ title, subtitle, action, children, className = '', bodyClass = 'p-4' }) {
  return (
    <section className={`card flex flex-col min-w-0 ${className}`}>
      {(title || action) && (
        <header className="flex items-center justify-between gap-3 px-4 pt-3.5 pb-3 border-b border-line/70">
          <div className="min-w-0">
            <h2 className="text-sm font-semibold tracking-tight truncate">{title}</h2>
            {subtitle && <p className="text-xs text-faint mt-0.5 truncate">{subtitle}</p>}
          </div>
          {action}
        </header>
      )}
      <div className={`flex-1 min-h-0 ${bodyClass}`}>{children}</div>
    </section>
  );
}

export const Button = forwardRef(function Button({ variant = 'primary', loading, children, className = '', ...p }, ref) {
  const cls = { primary: 'btn-primary', ghost: 'btn-ghost', danger: 'btn-danger', plain: 'btn text-muted hover:text-ink hover:bg-raised' }[variant];
  return (
    <button ref={ref} className={`${cls} ${className}`} disabled={loading || p.disabled} {...p}>
      {loading && <Loader2 className="w-4 h-4 animate-spin" />}{children}
    </button>
  );
});

export function Badge({ children, tone: t = 'neutral', dot, className = '' }) {
  const tones = {
    neutral: 'bg-raised text-muted border border-line', up: 'bg-up/12 text-up border border-up/25', down: 'bg-down/12 text-down border border-down/25',
    cyan: 'bg-cyan/12 text-cyan border border-cyan/25', violet: 'bg-violet/12 text-violet border border-violet/25', amber: 'bg-amber/12 text-amber border border-amber/30',
  };
  return <span className={`chip ${tones[t]} ${className}`}>{dot && <span className="w-1.5 h-1.5 rounded-full bg-current animate-pulseDot" />}{children}</span>;
}

export const Skeleton = ({ className = 'h-4 w-full' }) => <div className={`skeleton ${className}`} aria-hidden />;

export function EmptyState({ icon: Icon = Inbox, title, body, action }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-10 px-6">
      <div className="w-12 h-12 rounded-2xl bg-raised border border-line grid place-items-center mb-3"><Icon className="w-5 h-5 text-faint" /></div>
      <div className="text-sm font-semibold">{title}</div>
      {body && <p className="text-xs text-muted mt-1 max-w-sm">{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="flex flex-col items-center text-center py-8 px-6">
      <AlertTriangle className="w-6 h-6 text-down mb-2" />
      <div className="text-sm font-semibold">Couldn’t load this</div>
      <p className="text-xs text-muted mt-1 max-w-sm">{message}</p>
      {onRetry && <Button variant="ghost" className="mt-3" onClick={onRetry}>Try again</Button>}
    </div>
  );
}

/** Animated number that tweens between values (disabled for reduced motion). */
export function AnimatedNumber({ value, format = (v) => v, className = '' }) {
  const ref = useRef(null);
  const prev = useRef(Number(value) || 0);
  const reduce = useReducedMotion();
  useEffect(() => {
    const to = Number(value) || 0;
    if (reduce || !ref.current) { if (ref.current) ref.current.textContent = format(to); prev.current = to; return; }
    const ctl = animate(prev.current, to, { duration: 0.6, ease: [0.22, 1, 0.36, 1], onUpdate: (v) => { if (ref.current) ref.current.textContent = format(v); } });
    prev.current = to;
    return () => ctl.stop();
  }, [value]); // eslint-disable-line react-hooks/exhaustive-deps
  return <span ref={ref} className={`num ${className}`}>{format(Number(value) || 0)}</span>;
}

/** Price cell that flashes green/red on change (CSS only; cheap on rapid ticks). */
export function FlashPrice({ value, format, className = '' }) {
  const [dir, setDir] = useState(null);
  const prev = useRef(value);
  useEffect(() => {
    if (prev.current !== undefined && value !== undefined && value !== prev.current) {
      setDir(Number(value) > Number(prev.current) ? 'up' : 'down');
      const t = setTimeout(() => setDir(null), 900); prev.current = value; return () => clearTimeout(t);
    }
    prev.current = value;
  }, [value]);
  return <span className={`num rounded px-1 -mx-1 ${dir === 'up' ? 'animate-flashUp' : dir === 'down' ? 'animate-flashDown' : ''} ${className}`}>{format(value)}</span>;
}

export function Metric({ label, value, sub, subTone, icon: Icon, accent = 'cyan', children }) {
  return (
    <div className="card p-4 relative overflow-hidden group">
      <div className={`absolute -top-10 -right-10 w-28 h-28 rounded-full blur-2xl opacity-20 group-hover:opacity-35 transition bg-${accent}`} />
      <div className="flex items-center justify-between">
        <span className="label">{label}</span>
        {Icon && <Icon className={`w-4 h-4 text-${accent}`} />}
      </div>
      <div className="mt-2 text-xl font-semibold tracking-tight">{value}</div>
      {sub !== undefined && <div className={`text-xs mt-1 num ${subTone ?? tone(0)}`}>{sub}</div>}
      {children}
    </div>
  );
}

export function Tabs({ tabs, value, onChange, className = '' }) {
  return (
    <div role="tablist" className={`inline-flex p-1 rounded-lg bg-raised border border-line ${className}`}>
      {tabs.map((t) => {
        const id = typeof t === 'string' ? t : t.value; const label = typeof t === 'string' ? t : t.label;
        const active = id === value;
        return (
          <button key={id} role="tab" aria-selected={active} onClick={() => onChange(id)}
            className={`relative px-3 py-1.5 text-xs font-semibold rounded-md transition ${active ? 'text-ink' : 'text-muted hover:text-ink'}`}>
            {active && <motion.span layoutId={`tab-${tabs.map((x) => (typeof x === 'string' ? x : x.value)).join()}`} className="absolute inset-0 rounded-md bg-surface border border-line shadow-sm" transition={{ type: 'spring', stiffness: 500, damping: 38 }} />}
            <span className="relative">{label}</span>
          </button>
        );
      })}
    </div>
  );
}

export function Modal({ open, onClose, title, children, wide }) {
  useEffect(() => {
    if (!open) return; const h = (e) => e.key === 'Escape' && onClose?.(); window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h);
  }, [open, onClose]);
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-[80] grid place-items-center p-4" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <div className="absolute inset-0 bg-black/60" onClick={onClose} />
          <motion.div role="dialog" aria-modal="true" aria-label={title}
            initial={{ opacity: 0, y: 24, scale: 0.97 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, y: 12, scale: 0.98 }}
            transition={{ type: 'spring', stiffness: 380, damping: 32 }}
            className={`relative card w-full ${wide ? 'max-w-2xl' : 'max-w-md'} max-h-[90vh] overflow-auto`}>
            <div className="flex items-center justify-between px-5 py-4 border-b border-line">
              <h3 className="font-semibold">{title}</h3>
              <button onClick={onClose} className="text-faint hover:text-ink" aria-label="Close"><X className="w-4 h-4" /></button>
            </div>
            <div className="p-5">{children}</div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

export function Field({ label, hint, children, error }) {
  return (
    <label className="block">
      <span className="label block mb-1.5">{label}</span>
      {children}
      {error ? <span className="text-[11px] text-down mt-1 block">{error}</span> : hint ? <span className="text-[11px] text-faint mt-1 block">{hint}</span> : null}
    </label>
  );
}

export function Change({ value, pct, className = '' }) {
  return <span className={`num text-xs ${tone(value ?? pct)} ${className}`}>{value !== undefined && `${Number(value) > 0 ? '+' : ''}${Number(value).toFixed(2)} `}{pct !== undefined && `(${Number(pct) > 0 ? '+' : ''}${Number(pct).toFixed(2)}%)`}</span>;
}

/** Sortable table with optional row click. columns: [{key,label,render,align,sort}] */
export function DataTable({ columns, rows, onRowClick, empty, initialSort }) {
  const [sort, setSort] = useState(initialSort || null);
  const sorted = sort ? [...rows].sort((a, b) => {
    const col = columns.find((c) => c.key === sort.key); const get = col?.sort || ((r) => r[sort.key]);
    const x = get(a), y = get(b); const r = typeof x === 'number' || !Number.isNaN(Number(x)) ? Number(x) - Number(y) : String(x).localeCompare(String(y));
    return sort.dir === 'asc' ? r : -r;
  }) : rows;
  if (!rows.length) return empty || <EmptyState title="Nothing here yet" />;
  return (
    <div className="overflow-x-auto">
      <table className="table-base">
        <thead><tr>{columns.map((c) => (
          <th key={c.key} className={`${c.align === 'right' ? 'text-right' : ''} hover:text-ink cursor-pointer`} onClick={() => setSort((s) => ({ key: c.key, dir: s?.key === c.key && s.dir === 'desc' ? 'asc' : 'desc' }))}
            aria-sort={sort?.key === c.key ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
            {c.label}{sort?.key === c.key ? (sort.dir === 'asc' ? ' ↑' : ' ↓') : ''}
          </th>))}</tr></thead>
        <tbody>{sorted.map((r, i) => (
          <tr key={r.id ?? r.symbol ?? i} onClick={onRowClick ? () => onRowClick(r) : undefined} className={onRowClick ? 'cursor-pointer' : ''}>
            {columns.map((c) => <td key={c.key} className={c.align === 'right' ? 'text-right' : ''}>{c.render ? c.render(r) : r[c.key]}</td>)}
          </tr>))}</tbody>
      </table>
    </div>
  );
}

export function PageHeader({ title, subtitle, children, icon: Icon }) {
  return (
    <div className="flex flex-wrap items-end justify-between gap-4 mb-5">
      <div className="flex items-center gap-3">
        {Icon && <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-cyan/20 to-violet/20 border border-line grid place-items-center"><Icon className="w-5 h-5 text-cyan" /></div>}
        <div>
          <h1 className="text-xl md:text-2xl font-bold tracking-tight">{title}</h1>
          {subtitle && <p className="text-sm text-muted mt-0.5">{subtitle}</p>}
        </div>
      </div>
      {children && <div className="flex flex-wrap items-center gap-2">{children}</div>}
    </div>
  );
}

export const SimBadge = () => <Badge tone="amber">Simulated data</Badge>;
export const Disclaimer = ({ children = 'Educational information only. Not investment advice.' }) => <p className="text-[11px] text-faint flex items-center gap-1.5"><AlertTriangle className="w-3 h-3" />{children}</p>;
