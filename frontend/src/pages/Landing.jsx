import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, useInView, useReducedMotion, useScroll, useTransform } from 'framer-motion';
import { ArrowRight, BadgeIndianRupee, Bot, Brain, CandlestickChart, ChevronDown, Clock, FlaskConical, GraduationCap, History, LineChart, Lock, PieChart, Receipt, ShieldCheck, Sparkles, Target, Trophy, Zap } from 'lucide-react';
import Logo from '../components/layout/Logo';
import { useTheme } from '../context/ThemeContext';
import { Moon, Sun } from 'lucide-react';

/* All numbers on this page are illustrative and labelled "Simulated preview" - nothing here is live market data. */

function useSimSeries(n = 70, start = 24500) {
  const [data, setData] = useState(() => {
    let p = start; const out = [];
    for (let i = 0; i < n; i++) { const o = p; const c = o * (1 + (Math.random() - 0.47) * 0.008); out.push({ o, c, h: Math.max(o, c) * (1 + Math.random() * 0.003), l: Math.min(o, c) * (1 - Math.random() * 0.003) }); p = c; }
    return out;
  });
  const reduce = useReducedMotion();
  useEffect(() => {
    if (reduce) return;
    const t = setInterval(() => setData((d) => {
      const last = d[d.length - 1]; const c = last.c * (1 + (Math.random() - 0.48) * 0.006);
      return [...d.slice(1), { o: last.c, c, h: Math.max(last.c, c) * 1.0015, l: Math.min(last.c, c) * 0.9985 }];
    }), 1400);
    return () => clearInterval(t);
  }, [reduce]);
  return data;
}

function HeroChart() {
  const data = useSimSeries();
  const W = 640, H = 300;
  const { min, max } = useMemo(() => ({ min: Math.min(...data.map((d) => d.l)), max: Math.max(...data.map((d) => d.h)) }), [data]);
  const y = (v) => H - ((v - min) / (max - min || 1)) * (H - 30) - 15;
  const cw = W / data.length;
  const last = data[data.length - 1]; const first = data[0];
  const pct = ((last.c - first.o) / first.o) * 100;
  const sma = data.map((_, i) => { const s = data.slice(Math.max(0, i - 9), i + 1); return s.reduce((a, b) => a + b.c, 0) / s.length; });
  return (
    <div className="glass p-4 md:p-5 shadow-card">
      <div className="flex items-start justify-between mb-3">
        <div>
          <div className="flex items-center gap-2"><span className="text-xs font-semibold tracking-wider">NIFTY 50</span><span className="chip bg-amber/10 text-amber border border-amber/30">Simulated preview</span></div>
          <div className="flex items-baseline gap-2 mt-1"><span className="num text-2xl font-bold">{last.c.toLocaleString('en-IN', { maximumFractionDigits: 2 })}</span>
            <span className={`num text-sm ${pct >= 0 ? 'text-up' : 'text-down'}`}>{pct >= 0 ? '+' : ''}{pct.toFixed(2)}%</span></div>
        </div>
        <div className="flex gap-1 text-[10px]">{['1D', '1W', '1M'].map((t, i) => <span key={t} className={`px-2 py-1 rounded ${i === 0 ? 'bg-raised text-ink' : 'text-faint'}`}>{t}</span>)}</div>
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-auto" aria-label="Simulated Nifty 50 candlestick chart">
        {[0.25, 0.5, 0.75].map((g) => <line key={g} x1="0" x2={W} y1={H * g} y2={H * g} stroke="rgb(var(--line))" strokeDasharray="3 5" />)}
        {data.map((d, i) => {
          const up = d.c >= d.o; const col = up ? 'rgb(var(--up))' : 'rgb(var(--down))';
          return (<g key={i}><line x1={i * cw + cw / 2} x2={i * cw + cw / 2} y1={y(d.h)} y2={y(d.l)} stroke={col} strokeWidth="1" />
            <rect x={i * cw + cw * 0.18} width={cw * 0.64} y={y(Math.max(d.o, d.c))} height={Math.max(1.5, Math.abs(y(d.o) - y(d.c)))} fill={col} rx="1" /></g>);
        })}
        <polyline fill="none" stroke="rgb(var(--amber))" strokeWidth="1.6" strokeOpacity=".9" points={sma.map((v, i) => `${i * cw + cw / 2},${y(v)}`).join(' ')} />
        <line x1="0" x2={W} y1={y(last.c)} y2={y(last.c)} stroke="rgb(var(--cyan))" strokeDasharray="2 4" strokeOpacity=".7" />
      </svg>
    </div>
  );
}

const CARDS = [{ s: 'RELIANCE', p: 2950 }, { s: 'TCS', p: 4100 }, { s: 'HDFCBANK', p: 1650 }, { s: 'INFY', p: 1850 }];
function FloatingCard({ s, p, i }) {
  const [v, setV] = useState({ p, ch: 0 }); const reduce = useReducedMotion();
  useEffect(() => { if (reduce) return; const t = setInterval(() => setV((x) => { const np = x.p * (1 + (Math.random() - 0.5) * 0.004); return { p: np, ch: ((np - p) / p) * 100 }; }), 1800 + i * 300); return () => clearInterval(t); }, [p, i, reduce]);
  return (
    <motion.div animate={reduce ? {} : { y: [0, -8, 0] }} transition={{ duration: 5 + i, repeat: Infinity, ease: 'easeInOut' }} className="glass px-3.5 py-2.5 shadow-card min-w-[150px]">
      <div className="text-[11px] font-semibold tracking-wide text-muted">{s}</div>
      <div className="flex items-baseline justify-between gap-3"><span className="num text-sm font-semibold">₹{v.p.toFixed(2)}</span>
        <span className={`num text-[11px] ${v.ch >= 0 ? 'text-up' : 'text-down'}`}>{v.ch >= 0 ? '▲' : '▼'} {Math.abs(v.ch).toFixed(2)}%</span></div>
    </motion.div>
  );
}

function Counter({ to, prefix = '', suffix = '', format = (v) => Math.round(v).toLocaleString('en-IN') }) {
  const ref = useRef(null); const inView = useInView(ref, { once: true }); const [v, setV] = useState(0);
  useEffect(() => { if (!inView) return; let raf; const t0 = performance.now(); const tick = (t) => { const k = Math.min(1, (t - t0) / 1400); setV(to * (1 - Math.pow(1 - k, 3))); if (k < 1) raf = requestAnimationFrame(tick); }; raf = requestAnimationFrame(tick); return () => cancelAnimationFrame(raf); }, [inView, to]);
  return <span ref={ref} className="num">{prefix}{format(v)}{suffix}</span>;
}

const Reveal = ({ children, delay = 0, className = '' }) => (
  <motion.div initial={{ opacity: 0, y: 28 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true, margin: '-80px' }} transition={{ duration: 0.6, delay, ease: [0.22, 1, 0.36, 1] }} className={className}>{children}</motion.div>
);

const FEATURES = [
  { icon: BadgeIndianRupee, t: 'Risk-free paper trading', d: '₹10,00,000 of virtual capital. Market, limit, stop-loss and bracket orders filled at the server price during real NSE hours.', c: 'up' },
  { icon: CandlestickChart, t: 'Advanced charting', d: 'Interactive candlesticks, volume, SMA and RSI with crosshair, zoom and live last-candle updates.', c: 'cyan' },
  { icon: PieChart, t: 'Portfolio intelligence', d: 'Realised and unrealised P&L, sector allocation, drawdown and a Nifty 50 benchmark — computed from your actual trades.', c: 'violet' },
  { icon: Brain, t: 'Behavioural insights', d: 'Detects overtrading, revenge trades, loss streaks and missing stop-losses — and shows the exact trades behind each finding.', c: 'amber' },
  { icon: Receipt, t: 'Transparent charges', d: 'Brokerage, STT, GST, stamp duty, SEBI and exchange fees with a break-even price before every confirmation.', c: 'up' },
  { icon: FlaskConical, t: 'Backtesting & replay', d: 'Test SMA crossover and RSI strategies, then replay a historical session candle by candle.', c: 'cyan' },
];

const FAQ = [
  ['Is any real money involved?', 'No. Nifty Nexus is a paper-trading platform. Every account gets virtual money and no orders reach a real exchange or broker.'],
  ['Are the prices live?', 'When the Angel One feed is configured, yes. Otherwise the platform runs a clearly-labelled simulated feed so you can learn the workflow any time. Simulated data is always badged.'],
  ['When can I trade?', 'Any time. Nifty Nexus runs in practice mode, so the market stays open 24×7 for buying and selling. An admin can switch back to real NSE hours (9:15 AM – 3:30 PM IST, Monday to Friday) from the Admin Console.'],
  ['How are charges calculated?', 'Using the official structure for delivery equity: brokerage, STT, exchange transaction charges, SEBI fees, stamp duty and GST. Rates are configurable by the admin.'],
  ['Is the AI coach giving investment advice?', 'No. The coach reviews your own recorded trades and explains concepts. It is educational only.'],
];

export default function Landing() {
  const { theme, toggle } = useTheme(); const [faq, setFaq] = useState(0);
  const { scrollY } = useScroll(); const yHero = useTransform(scrollY, [0, 600], [0, 80]); const yCards = useTransform(scrollY, [0, 600], [0, -60]);
  return (
    <div className="noise relative overflow-x-hidden">
      {/* atmospheric lighting */}
      <div className="pointer-events-none absolute inset-x-0 top-0 h-[900px] overflow-hidden">
        <div className="absolute -top-40 left-1/2 -translate-x-1/2 w-[1100px] h-[700px] rounded-full bg-up/10 blur-[120px]" />
        <div className="absolute top-40 -right-40 w-[600px] h-[500px] rounded-full bg-violet/10 blur-[120px]" />
        <div className="absolute inset-0 grid-bg animate-gridpan [mask-image:radial-gradient(ellipse_at_top,black_30%,transparent_70%)]" />
      </div>

      <header className="relative z-20 max-w-7xl mx-auto px-5 h-16 flex items-center justify-between">
        <Logo />
        <nav className="hidden md:flex items-center gap-7 text-sm text-muted">
          <a href="#features" className="hover:text-ink">Platform</a><a href="#charges" className="hover:text-ink">Charges</a>
          <a href="#discipline" className="hover:text-ink">Discipline</a><a href="#faq" className="hover:text-ink">FAQ</a>
        </nav>
        <div className="flex items-center gap-2">
          <button onClick={toggle} className="p-2 rounded-lg hover:bg-raised text-muted" aria-label="Toggle theme">{theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}</button>
          <Link to="/login" className="btn-ghost !py-1.5 text-xs">Log in</Link>
          <Link to="/register" className="btn-primary !py-1.5 text-xs hidden sm:inline-flex">Get started</Link>
        </div>
      </header>

      {/* HERO */}
      <section className="relative z-10 max-w-7xl mx-auto px-5 pt-10 md:pt-16 pb-20 grid lg:grid-cols-[1.05fr_1fr] gap-12 items-center">
        <motion.div style={{ y: yHero }}>
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} className="inline-flex items-center gap-2 chip bg-raised border border-line text-muted !py-1 !px-3 mb-6">
            <Sparkles className="w-3.5 h-3.5 text-cyan" /> Paper trading for NSE · by Abdullah Shaikh
          </motion.div>
          <motion.h1 initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
            className="text-4xl sm:text-5xl lg:text-[64px] leading-[1.02] font-extrabold tracking-[-0.035em]">
            Master the Market.<br /><span className="bg-gradient-to-r from-up via-cyan to-violet bg-clip-text text-transparent">Before You Risk a Rupee.</span>
          </motion.h1>
          <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 0.25 }} className="mt-6 text-lg text-muted max-w-xl">
            Experience Indian markets, practise smarter trading, understand your risk, and build financial discipline with Nifty Nexus.
          </motion.p>
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.4 }} className="mt-8 flex flex-wrap gap-3">
            <Link to="/register" className="btn-primary !px-6 !py-3 text-[15px]">Start Paper Trading <ArrowRight className="w-4 h-4" /></Link>
            <a href="#features" className="btn-ghost !px-6 !py-3 text-[15px]">Explore the Platform</a>
          </motion.div>
          <div className="mt-12 grid grid-cols-2 sm:grid-cols-3 gap-6 max-w-lg">
            <div className="col-span-2 sm:col-span-1"><div className="text-2xl font-bold"><Counter to={1000000} prefix="₹" /></div><div className="text-xs text-faint mt-1">Virtual capital</div></div>
            <div><div className="text-2xl font-bold"><Counter to={50} suffix="+" /></div><div className="text-xs text-faint mt-1">Nifty 50 instruments</div></div>
            <div><div className="text-2xl font-bold"><Counter to={8} /></div><div className="text-xs text-faint mt-1">Behaviour patterns tracked</div></div>
          </div>
        </motion.div>
        <div className="relative">
          <HeroChart />
          <motion.div style={{ y: yCards }} className="hidden md:flex absolute -left-10 -bottom-10 gap-3"><FloatingCard {...CARDS[0]} i={0} /><FloatingCard {...CARDS[1]} i={1} /></motion.div>
          <motion.div style={{ y: yCards }} className="hidden md:flex absolute -right-6 -top-8 flex-col gap-3"><FloatingCard {...CARDS[2]} i={2} /></motion.div>
        </div>
      </section>

      {/* FEATURES */}
      <section id="features" className="relative z-10 max-w-7xl mx-auto px-5 py-20">
        <Reveal><p className="label text-cyan">The platform</p><h2 className="text-3xl md:text-4xl font-bold tracking-tight mt-2 max-w-2xl">A trading workstation that teaches while you trade.</h2></Reveal>
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 mt-10">
          {FEATURES.map((f, i) => (
            <Reveal key={f.t} delay={i * 0.06}>
              <div className="card p-6 h-full group hover:-translate-y-1 transition-transform duration-300">
                <div className={`w-10 h-10 rounded-xl grid place-items-center bg-${f.c}/10 border border-line mb-4`}><f.icon className={`w-5 h-5 text-${f.c}`} /></div>
                <h3 className="font-semibold">{f.t}</h3><p className="text-sm text-muted mt-2 leading-relaxed">{f.d}</p>
              </div>
            </Reveal>))}
        </div>
      </section>

      {/* DASHBOARD PREVIEW */}
      <section className="relative z-10 max-w-7xl mx-auto px-5 py-16">
        <Reveal>
          <div className="glass p-2 md:p-3 shadow-card">
            <div className="rounded-xl border border-line bg-bg overflow-hidden">
              <div className="flex items-center gap-1.5 px-4 h-9 border-b border-line"><span className="w-2.5 h-2.5 rounded-full bg-down/70" /><span className="w-2.5 h-2.5 rounded-full bg-amber/70" /><span className="w-2.5 h-2.5 rounded-full bg-up/70" /><span className="ml-3 text-[11px] text-faint">niftynexus · overview · simulated preview</span></div>
              <div className="grid md:grid-cols-4 gap-3 p-4">
                {[['Portfolio value', '₹10,42,318', '+4.23%', 'up'], ['Realised P&L', '₹18,240', '+1.82%', 'up'], ['Discipline score', '86 / 100', 'Disciplined', 'cyan'], ['Charges paid', '₹2,141', '0.21% of turnover', 'amber']].map(([l, v, s, c]) => (
                  <div key={l} className="card p-4"><div className="label">{l}</div><div className="num text-lg font-semibold mt-1.5">{v}</div><div className={`text-xs num text-${c} mt-0.5`}>{s}</div></div>))}
                <div className="md:col-span-3 card p-4"><div className="label mb-2">Equity vs Nifty 50</div>
                  <svg viewBox="0 0 600 140" className="w-full h-auto">{(() => { const a = [], b = []; let x = 100, z = 100; for (let i = 0; i < 60; i++) { x += Math.sin(i / 5) * 0.8 + 0.25; z += Math.cos(i / 7) * 0.6 + 0.12; a.push(`${i * 10},${140 - (x - 90) * 4}`); b.push(`${i * 10},${140 - (z - 90) * 4}`); } return (<><polyline points={a.join(' ')} fill="none" stroke="rgb(var(--up))" strokeWidth="2" /><polyline points={b.join(' ')} fill="none" stroke="rgb(var(--violet))" strokeWidth="1.6" strokeDasharray="4 4" /></>); })()}</svg></div>
                <div className="card p-4"><div className="label mb-3">Sector mix</div>{[['IT', 34, 'cyan'], ['Banking', 28, 'up'], ['FMCG', 18, 'violet'], ['Energy', 20, 'amber']].map(([s, p, c]) => <div key={s} className="mb-2"><div className="flex justify-between text-xs"><span>{s}</span><span className="num">{p}%</span></div><div className="h-1.5 bg-raised rounded mt-1"><div className={`h-full rounded bg-${c}`} style={{ width: `${p}%` }} /></div></div>)}</div>
              </div>
            </div>
          </div>
        </Reveal>
      </section>

      {/* DISCIPLINE */}
      <section id="discipline" className="relative z-10 max-w-7xl mx-auto px-5 py-20 grid lg:grid-cols-2 gap-12 items-center">
        <Reveal>
          <p className="label text-amber">Signature feature</p>
          <h2 className="text-3xl md:text-4xl font-bold tracking-tight mt-2">Your trading habits, made visible.</h2>
          <p className="text-muted mt-4 leading-relaxed">Most retail traders don’t lose because of bad stocks — they lose because of bad habits. Nifty Nexus watches every recorded trade for overtrading, revenge trades, loss streaks, oversized positions and ignored stop-losses, then links each finding to the trades that triggered it and a short lesson.</p>
          <ul className="mt-6 space-y-3 text-sm">{[[Target, 'Pre-trade checklist before every order'], [Clock, 'Cooldown prompt after consecutive losses'], [ShieldCheck, 'Fat-finger and circuit-limit protection'], [Bot, 'Rule-based coach that explains, never advises']].map(([I, t]) => <li key={t} className="flex items-center gap-3"><I className="w-4 h-4 text-up" />{t}</li>)}</ul>
        </Reveal>
        <Reveal delay={0.1}>
          <div className="card p-6">
            <div className="flex items-center justify-between"><span className="label">Discipline score</span><span className="chip bg-amber/10 text-amber border border-amber/30">Example</span></div>
            <div className="flex items-end gap-3 mt-3"><span className="num text-5xl font-bold">72</span><span className="text-sm text-muted mb-2">/ 100 · Developing</span></div>
            <div className="mt-5 space-y-3">{[['Possible revenge trade in TATAMOTORS', 'Buy placed 6 min after a ₹4,120 loss', 'down'], ['3 consecutive losing trades', 'Cooldown suggested', 'amber'], ['4 of 6 buys had no stop-loss', 'Lesson: decide the exit before entry', 'amber']].map(([t, d, c]) => (
              <div key={t} className="rounded-lg border border-line bg-raised/60 p-3"><div className={`text-sm font-semibold text-${c}`}>{t}</div><div className="text-xs text-muted mt-0.5">{d}</div></div>))}</div>
          </div>
        </Reveal>
      </section>

      {/* CHARGES */}
      <section id="charges" className="relative z-10 max-w-7xl mx-auto px-5 py-20">
        <Reveal className="text-center max-w-2xl mx-auto"><p className="label text-up">Cost transparency</p><h2 className="text-3xl md:text-4xl font-bold tracking-tight mt-2">See every rupee before you confirm.</h2>
          <p className="text-muted mt-3">Example: buying 100 shares at ₹1,000 (delivery)</p></Reveal>
        <Reveal delay={0.1}><div className="card max-w-xl mx-auto mt-8 p-6">
          {[['Brokerage (₹20 or 0.03%)', '20.00'], ['STT (0.1%)', '100.00'], ['Exchange txn (0.00297%)', '2.97'], ['SEBI fee', '0.10'], ['Stamp duty (0.015%)', '15.00'], ['GST (18%)', '4.15']].map(([l, v]) => <div key={l} className="flex justify-between py-2 border-b border-line/60 text-sm"><span className="text-muted">{l}</span><span className="num">₹{v}</span></div>)}
          <div className="flex justify-between pt-3 font-semibold"><span>Total charges</span><span className="num">₹142.22</span></div>
          <div className="flex justify-between pt-1 text-sm text-amber"><span>Break-even sell price</span><span className="num">≈ ₹1,002.70</span></div>
        </div></Reveal>
      </section>

      {/* MORE */}
      <section className="relative z-10 max-w-7xl mx-auto px-5 py-16 grid md:grid-cols-3 gap-4">
        {[[History, 'Historical replay', 'Pick a volatile day and trade it candle by candle with play, pause and speed control.'], [GraduationCap, 'Learning & challenges', 'Daily challenges, badges, skill levels and an F&O risk quiz that unlocks derivatives practice.'], [Trophy, 'Risk-adjusted leaderboard', 'Ranked by return after drawdown and volatility — not by who gambled hardest.']].map(([I, t, d], i) => (
          <Reveal key={t} delay={i * 0.08}><div className="card p-6 h-full"><I className="w-6 h-6 text-violet" /><h3 className="font-semibold mt-4">{t}</h3><p className="text-sm text-muted mt-2">{d}</p></div></Reveal>))}
      </section>

      {/* FAQ */}
      <section id="faq" className="relative z-10 max-w-3xl mx-auto px-5 py-20">
        <Reveal><h2 className="text-3xl font-bold tracking-tight text-center">Frequently asked questions</h2></Reveal>
        <div className="mt-8 space-y-2">{FAQ.map(([q, a], i) => (
          <div key={q} className="card overflow-hidden">
            <button className="w-full flex items-center justify-between px-5 py-4 text-left font-medium" onClick={() => setFaq(faq === i ? -1 : i)} aria-expanded={faq === i}>{q}<ChevronDown className={`w-4 h-4 transition ${faq === i ? 'rotate-180' : ''}`} /></button>
            <motion.div initial={false} animate={{ height: faq === i ? 'auto' : 0, opacity: faq === i ? 1 : 0 }} className="overflow-hidden"><p className="px-5 pb-4 text-sm text-muted">{a}</p></motion.div>
          </div>))}</div>
      </section>

      {/* CTA */}
      <section className="relative z-10 max-w-5xl mx-auto px-5 pb-24">
        <Reveal><div className="relative card p-10 md:p-14 text-center overflow-hidden">
          <div className="absolute inset-0 bg-gradient-to-br from-up/10 via-transparent to-violet/10" />
          <div className="relative"><Zap className="w-8 h-8 text-up mx-auto" /><h2 className="text-3xl md:text-4xl font-bold tracking-tight mt-4">Your first trade is five minutes away.</h2>
            <p className="text-muted mt-3">Create an account, receive ₹10,00,000 in virtual capital and start practising today.</p>
            <Link to="/register" className="btn-primary !px-7 !py-3 mt-8 text-[15px]">Start Paper Trading <ArrowRight className="w-4 h-4" /></Link></div>
        </div></Reveal>
      </section>

      <footer className="relative z-10 border-t border-line">
        <div className="max-w-7xl mx-auto px-5 py-10 grid md:grid-cols-4 gap-8 text-sm">
          <div className="md:col-span-2"><Logo /><p className="text-muted mt-3 max-w-sm text-xs leading-relaxed">Nifty Nexus is an educational paper-trading platform. It does not place real orders, hold real money or provide investment advice. Simulated data is always labelled.</p></div>
          <div><div className="label mb-3">Platform</div><ul className="space-y-2 text-muted"><li><Link to="/register" className="hover:text-ink">Paper trading</Link></li><li><a href="#charges" className="hover:text-ink">Charges</a></li><li><a href="#discipline" className="hover:text-ink">Behavioural insights</a></li></ul></div>
          <div><div className="label mb-3">Account</div><ul className="space-y-2 text-muted"><li><Link to="/login" className="hover:text-ink">Log in</Link></li><li><Link to="/register" className="hover:text-ink">Create account</Link></li></ul></div>
        </div>
        <div className="max-w-7xl mx-auto px-5 pb-8 text-xs text-faint flex flex-wrap items-center gap-2"><Lock className="w-3 h-3" />© {new Date().getFullYear()} Nifty Nexus · Designed &amp; developed by <span className="text-ink font-semibold">Abdullah Shaikh</span> · Educational use only</div>
      </footer>
    </div>
  );
}
