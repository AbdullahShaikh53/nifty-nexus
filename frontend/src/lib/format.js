// Indian number formatting (lakh/crore grouping) and IST time helpers.
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 2 });
const inr0 = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });
const num2 = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 2, minimumFractionDigits: 2 });
const int = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });

const n = (v) => (v === null || v === undefined || v === '' ? null : Number(v));
export const fmtINR = (v, whole = false) => (n(v) === null || Number.isNaN(n(v)) ? '—' : (whole ? inr0 : inr).format(n(v)));
export const fmtNum = (v) => (n(v) === null ? '—' : num2.format(n(v)));
export const fmtInt = (v) => (n(v) === null ? '—' : int.format(n(v)));
export const fmtPct = (v, sign = true) => (n(v) === null ? '—' : `${sign && n(v) > 0 ? '+' : ''}${num2.format(n(v))}%`);
export const fmtSigned = (v) => (n(v) === null ? '—' : `${n(v) > 0 ? '+' : ''}${num2.format(n(v))}`);
export const fmtCompact = (v) => {
  const x = n(v); if (x === null) return '—';
  const a = Math.abs(x);
  if (a >= 1e7) return `${(x / 1e7).toFixed(2)} Cr`;
  if (a >= 1e5) return `${(x / 1e5).toFixed(2)} L`;
  if (a >= 1e3) return `${(x / 1e3).toFixed(1)} K`;
  return x.toFixed(0);
};
export const tone = (v) => (n(v) > 0 ? 'text-up' : n(v) < 0 ? 'text-down' : 'text-muted');
export const fmtTime = (iso) => {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata', hour: '2-digit', minute: '2-digit', second: '2-digit', day: '2-digit', month: 'short' });
};
export const fmtDate = (iso) => (iso ? new Date(iso).toLocaleDateString('en-IN', { timeZone: 'Asia/Kolkata', day: '2-digit', month: 'short', year: 'numeric' }) : '—');
