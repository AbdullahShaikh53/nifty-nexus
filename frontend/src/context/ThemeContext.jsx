import { createContext, useContext, useEffect, useState } from 'react';

const ThemeCtx = createContext(null);
const read = (k, d) => { try { const v = localStorage.getItem(k); return v === null ? d : JSON.parse(v); } catch { return d; } };
const write = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch { /* storage unavailable */ } };

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => read('nn.theme', 'dark'));
  const [largeText, setLargeText] = useState(() => read('nn.largeText', false));
  const [cbSafe, setCbSafe] = useState(() => read('nn.cbSafe', false));
  useEffect(() => {
    const el = document.documentElement;
    el.classList.toggle('dark', theme === 'dark');
    el.classList.toggle('large-text', largeText);
    el.classList.toggle('cb-safe', cbSafe);
    write('nn.theme', theme); write('nn.largeText', largeText); write('nn.cbSafe', cbSafe);
  }, [theme, largeText, cbSafe]);
  const toggle = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'));
  return <ThemeCtx.Provider value={{ theme, toggle, setTheme, largeText, setLargeText, cbSafe, setCbSafe }}>{children}</ThemeCtx.Provider>;
}
export const useTheme = () => useContext(ThemeCtx);
