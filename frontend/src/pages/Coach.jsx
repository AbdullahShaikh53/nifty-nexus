import { useEffect, useRef, useState } from 'react';
import { Bot, Copy, Send, User } from 'lucide-react';
import { motion } from 'framer-motion';
import { api, errorMessage } from '../lib/api';
import { Badge, Disclaimer, PageHeader, Panel } from '../components/ui';
import { useToast } from '../context/ToastContext';

const load = () => { try { return JSON.parse(sessionStorage.getItem('nn.coach') || '[]'); } catch { return []; } };

export default function Coach() {
  const [msgs, setMsgs] = useState(load); const [q, setQ] = useState(''); const [typing, setTyping] = useState(false); const [sugg, setSugg] = useState(['Review my mistakes', 'Why did my portfolio drop?', 'Analyse my last trade', 'Explain RSI', 'What is risk reward?']);
  const end = useRef(null); const toast = useToast();
  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }); try { sessionStorage.setItem('nn.coach', JSON.stringify(msgs.slice(-40))); } catch { /* ignore */ } }, [msgs, typing]);

  const ask = async (text) => {
    const question = (text ?? q).trim(); if (!question) return;
    setQ(''); setMsgs((m) => [...m, { role: 'user', text: question }]); setTyping(true);
    try {
      const { data } = await api.post('/coach/ask', { question });
      setSugg(data.suggestions);
      // streaming-style reveal
      const full = data.answer; let i = 0; setMsgs((m) => [...m, { role: 'coach', text: '', mode: data.mode, refs: data.references }]); setTyping(false);
      const step = () => { i = Math.min(full.length, i + 6); setMsgs((m) => { const c = [...m]; c[c.length - 1] = { ...c[c.length - 1], text: full.slice(0, i) }; return c; }); if (i < full.length) setTimeout(step, 12); };
      step();
    } catch (e) { setTyping(false); setMsgs((m) => [...m, { role: 'coach', text: `Sorry — ${errorMessage(e)}`, mode: 'error' }]); }
  };

  return (
    <>
      <PageHeader icon={Bot} title="AI Trade Coach" subtitle="Reviews your own recorded trades and explains concepts in plain language"><Badge tone="violet">Rule-based mode</Badge></PageHeader>
      <div className="grid lg:grid-cols-[1fr_300px] gap-3">
        <section className="card flex flex-col h-[calc(100vh-230px)] min-h-[480px]">
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {msgs.length === 0 && <div className="text-center py-16"><div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-br from-violet/30 to-cyan/30 grid place-items-center"><Bot className="w-7 h-7 text-violet" /></div>
              <h2 className="font-semibold mt-4">How can I help you trade better?</h2><p className="text-sm text-muted mt-1">Ask about your mistakes, your portfolio, or any concept.</p></div>}
            {msgs.map((m, i) => (
              <motion.div key={i} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} className={`flex gap-3 ${m.role === 'user' ? 'flex-row-reverse' : ''}`}>
                <div className={`w-8 h-8 shrink-0 rounded-lg grid place-items-center ${m.role === 'user' ? 'bg-raised' : 'bg-violet/15'}`}>{m.role === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4 text-violet" />}</div>
                <div className={`max-w-[80%] rounded-xl px-4 py-3 text-sm whitespace-pre-wrap leading-relaxed ${m.role === 'user' ? 'bg-up/10 border border-up/20' : 'bg-raised border border-line'}`}>
                  {m.text}
                  {m.role === 'coach' && m.text && <div className="flex items-center gap-2 mt-2 pt-2 border-t border-line/60">
                    {m.mode && <span className="text-[10px] uppercase tracking-wider text-faint">{m.mode}</span>}
                    {m.refs?.map((r) => <Badge key={r} tone="cyan">{r}</Badge>)}
                    <button className="ml-auto text-faint hover:text-ink" aria-label="Copy" onClick={() => { navigator.clipboard?.writeText(m.text); toast.info('Copied'); }}><Copy className="w-3.5 h-3.5" /></button></div>}
                </div>
              </motion.div>))}
            {typing && <div className="flex gap-3"><div className="w-8 h-8 rounded-lg bg-violet/15 grid place-items-center"><Bot className="w-4 h-4 text-violet" /></div>
              <div className="bg-raised border border-line rounded-xl px-4 py-3 flex gap-1">{[0, 1, 2].map((d) => <span key={d} className="w-1.5 h-1.5 rounded-full bg-muted animate-pulseDot" style={{ animationDelay: `${d * 0.2}s` }} />)}</div></div>}
            <div ref={end} />
          </div>
          <div className="border-t border-line p-3">
            <div className="flex flex-wrap gap-1.5 mb-2">{sugg.map((s) => <button key={s} onClick={() => ask(s)} className="chip bg-raised border border-line text-muted hover:text-ink">{s}</button>)}</div>
            <form onSubmit={(e) => { e.preventDefault(); ask(); }} className="flex gap-2">
              <input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Ask the coach…" aria-label="Message" maxLength={500} />
              <button className="btn-primary !px-3" aria-label="Send"><Send className="w-4 h-4" /></button>
            </form>
            <div className="mt-2"><Disclaimer /></div>
          </div>
        </section>
        <Panel title="What the coach can do">
          <ul className="text-sm text-muted space-y-2.5">
            <li>• Analyse your recent trades</li><li>• Summarise recurring mistakes</li><li>• Explain why your portfolio moved</li><li>• Explain RSI, SMA, stop-loss, STT, slippage, drawdown</li><li>• Generate a weekly summary (“weekly summary”)</li>
          </ul>
          <p className="text-xs text-faint mt-4">Runs rule-based on the Java backend. An optional LLM can be enabled with <code className="num">COACH_LLM_ENABLED</code>; answers stay grounded in your own data.</p>
        </Panel>
      </div>
    </>
  );
}
