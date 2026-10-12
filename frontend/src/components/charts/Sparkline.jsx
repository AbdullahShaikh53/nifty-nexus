import { memo } from 'react';

/** Tiny SVG sparkline. Values come from the caller (history or recent ticks). */
function Sparkline({ values = [], width = 96, height = 28, positive }) {
  if (values.length < 2) return <div style={{ width, height }} />;
  const min = Math.min(...values), max = Math.max(...values); const span = max - min || 1;
  const pts = values.map((v, i) => `${(i / (values.length - 1)) * width},${height - ((v - min) / span) * (height - 4) - 2}`).join(' ');
  const up = positive ?? values[values.length - 1] >= values[0];
  const color = up ? 'rgb(var(--up))' : 'rgb(var(--down))';
  const id = `sg-${Math.round(values[0] * 100)}-${values.length}-${up}`;
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} aria-hidden>
      <defs><linearGradient id={id} x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor={color} stopOpacity=".28" /><stop offset="100%" stopColor={color} stopOpacity="0" /></linearGradient></defs>
      <polygon points={`0,${height} ${pts} ${width},${height}`} fill={`url(#${id})`} />
      <polyline points={pts} fill="none" stroke={color} strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}
export default memo(Sparkline);
