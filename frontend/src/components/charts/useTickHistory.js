import { useEffect, useRef, useState } from 'react';
import { useQuote } from '../../context/MarketContext';

/** Keeps the last N LTPs for a symbol from the live stream (for sparklines). */
export function useTickHistory(symbol, n = 40) {
  const q = useQuote(symbol);
  const buf = useRef([]);
  const [, force] = useState(0);
  useEffect(() => {
    if (!q) return;
    const v = Number(q.ltp);
    if (buf.current[buf.current.length - 1] !== v) { buf.current = [...buf.current, v].slice(-n); force((x) => x + 1); }
  }, [q, n]);
  return buf.current;
}
