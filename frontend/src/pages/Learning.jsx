import { useMemo, useState } from 'react';
import { Award, BookOpen, CheckCircle2, Circle, GraduationCap, Lock, Unlock } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { strategyPayoff } from '../lib/calc';
import { fmtINR } from '../lib/format';
import { Badge, Button, Disclaimer, Field, Metric, Modal, PageHeader, Panel, Skeleton, Tabs } from '../components/ui';
import { useToast } from '../context/ToastContext';

const BADGES = { FIRST_TRADE: 'First trade', DISCIPLINED_EXITS: 'Disciplined exits', JOURNAL_KEEPER: 'Journal keeper', ACTIVE_LEARNER: 'Active learner', ICE_COLD: 'Ice cold', FNO_READY: 'F&O ready' };
const LESSONS = [
  ['Market vs limit orders', 'A market order buys now at whatever the price is; a limit order buys only at your price or better. Beginners overpay most often with market orders in fast markets.'],
  ['Why stop-losses matter', 'A 50% loss needs a 100% gain to recover. Small, pre-planned losses keep you in the game long enough to learn.'],
  ['The real cost of a trade', 'On a ₹1 lakh delivery trade, STT alone is ₹100 each way. Frequent trading quietly erodes returns.'],
  ['Position sizing', 'Decide how much you are willing to lose (1–2% of capital), then divide by the distance to your stop to get the share count.'],
  ['Reading RSI', 'RSI above 70 or below 30 shows momentum extremes. In strong trends RSI can stay extreme for a long time — it is context, not a signal.'],
  ['Options in one minute', 'Buyers risk only the premium; sellers collect premium but can face large losses. Time decay (theta) works against buyers every day.'],
];

function Payoff() {
  const [lot, setLot] = useState(25); const [spot, setSpot] = useState(24500);
  const [legs, setLegs] = useState([{ type: 'CE', side: 'BUY', strike: 24500, premium: 180, lots: 1 }]);
  const r = useMemo(() => strategyPayoff(legs.map((l) => ({ ...l, strike: +l.strike, premium: +l.premium, lots: +l.lots })), +lot, spot * 0.9, spot * 1.1), [legs, lot, spot]);
  const W = 560, H = 200; const ys = r.points.map((p) => p.pnl); const mn = Math.min(...ys, 0), mx = Math.max(...ys, 0);
  const X = (s) => ((s - r.points[0].spot) / (r.points[r.points.length - 1].spot - r.points[0].spot)) * W; const Y = (v) => H - ((v - mn) / (mx - mn || 1)) * H;
  const margin = legs.reduce((a, l) => a + (l.side === 'SELL' ? +l.strike * +lot * +l.lots * 0.12 : +l.premium * +lot * +l.lots), 0);
  const upd = (i, k, v) => setLegs(legs.map((l, j) => (j === i ? { ...l, [k]: v } : l)));
  return (
    <div className="space-y-3">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3"><Field label="Underlying spot"><input className="input num" type="number" value={spot} onChange={(e) => setSpot(+e.target.value)} /></Field><Field label="Lot size"><input className="input num" type="number" value={lot} onChange={(e) => setLot(e.target.value)} /></Field></div>
      {legs.map((l, i) => (
        <div key={i} className="grid grid-cols-2 md:grid-cols-6 gap-2 items-end">
          <Field label="Side"><select className="input" value={l.side} onChange={(e) => upd(i, 'side', e.target.value)}><option>BUY</option><option>SELL</option></select></Field>
          <Field label="Type"><select className="input" value={l.type} onChange={(e) => upd(i, 'type', e.target.value)}><option>CE</option><option>PE</option></select></Field>
          <Field label="Strike"><input className="input num" type="number" value={l.strike} onChange={(e) => upd(i, 'strike', e.target.value)} /></Field>
          <Field label="Premium"><input className="input num" type="number" value={l.premium} onChange={(e) => upd(i, 'premium', e.target.value)} /></Field>
          <Field label="Lots"><input className="input num" type="number" value={l.lots} onChange={(e) => upd(i, 'lots', e.target.value)} /></Field>
          <Button variant="ghost" onClick={() => setLegs(legs.filter((_, j) => j !== i))} disabled={legs.length === 1}>Remove</Button>
        </div>))}
      <Button variant="ghost" onClick={() => setLegs([...legs, { type: 'PE', side: 'BUY', strike: spot, premium: 150, lots: 1 }])}>+ Add leg</Button>
      <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-auto bg-raised/40 rounded-lg" role="img" aria-label="Payoff diagram">
        <line x1="0" x2={W} y1={Y(0)} y2={Y(0)} stroke="rgb(var(--faint))" strokeDasharray="4 4" />
        <line x1={X(spot)} x2={X(spot)} y1="0" y2={H} stroke="rgb(var(--cyan))" strokeDasharray="2 4" />
        <polyline fill="none" stroke="rgb(var(--violet))" strokeWidth="2" points={r.points.map((p) => `${X(p.spot)},${Y(p.pnl)}`).join(' ')} />
      </svg>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-sm">
        <Metric label="Max profit" value={<span className="num text-up">{r.maxProfit === Infinity ? 'Unlimited' : fmtINR(r.maxProfit, true)}</span>} />
        <Metric label="Max loss" value={<span className="num text-down">{r.maxLoss === -Infinity ? 'Unlimited' : fmtINR(r.maxLoss, true)}</span>} />
        <Metric label="Break-even" value={<span className="num">{r.breakEvens.map((b) => b.toFixed(0)).join(' / ') || '—'}</span>} />
        <Metric label="Margin (approx.)" value={<span className="num">{fmtINR(margin, true)}</span>} sub="Sell legs ≈ 12% of notional" />
      </div>
      <div className="text-xs text-muted grid md:grid-cols-3 gap-2">
        <p><b className="text-ink">Delta</b> — how much the option moves for a ₹1 move in the underlying.</p>
        <p><b className="text-ink">Theta</b> — value lost each day from time decay; it hurts buyers.</p>
        <p><b className="text-ink">IV</b> — the market’s expected volatility; high IV makes options expensive.</p>
      </div>
      <Disclaimer>Option-chain and live Greeks need an F&O data feed (not configured). Calculators use the values you enter.</Disclaimer>
    </div>
  );
}

export default function Learning() {
  const prog = useApi('/learning/progress'); const toast = useToast();
  const [tab, setTab] = useState('learn'); const [quiz, setQuiz] = useState(null); const [answers, setAnswers] = useState({}); const [lesson, setLesson] = useState(null);
  const openQuiz = async () => { try { const { data } = await api.get('/learning/quiz'); setQuiz(data); setAnswers({}); } catch (e) { toast.error('Quiz unavailable', errorMessage(e)); } };
  const submit = async () => {
    try { const { data } = await api.post('/learning/quiz', answers); setQuiz(null); prog.reload();
      data.passed ? toast.success('F&O practice unlocked', `${data.correct}/${data.total} correct`) : toast.warning('Not quite', `${data.correct}/${data.total} correct — you need 5. Review the lessons and try again.`); }
    catch (e) { toast.error('Submit failed', errorMessage(e)); }
  };
  const p = prog.data;
  return (
    <>
      <PageHeader icon={GraduationCap} title="Learning Hub" subtitle="Lessons, daily challenges, badges and F&O practice">
        <Tabs tabs={[{ value: 'learn', label: 'Learn' }, { value: 'fno', label: 'F&O practice' }]} value={tab} onChange={setTab} />
      </PageHeader>
      {!p ? <Skeleton className="h-64" /> : tab === 'learn' ? (<>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <Metric label="Skill level" accent="violet" value={p.level} sub={`${p.filledOrders} filled orders`} />
          <Metric label="Discipline" accent="up" value={<span className="num">{p.disciplineScore}/100</span>} />
          <Metric label="Badges" accent="amber" value={<span className="num">{p.badges.length} / {Object.keys(BADGES).length}</span>} />
          <Metric label="F&O" accent="cyan" value={p.fnoUnlocked ? 'Unlocked' : 'Locked'} sub={p.fnoUnlocked ? 'Quiz passed' : 'Pass the risk quiz'} />
        </div>
        <div className="grid lg:grid-cols-3 gap-3 mt-3">
          <Panel title="Today’s challenges">{p.challenges.map((c) => <div key={c.title} className="flex items-center gap-2 py-2 text-sm">{c.done ? <CheckCircle2 className="w-4 h-4 text-up" /> : <Circle className="w-4 h-4 text-faint" />}<span className={c.done ? 'line-through text-muted' : ''}>{c.title}</span></div>)}</Panel>
          <Panel title="Badges"><div className="grid grid-cols-3 gap-2">{Object.entries(BADGES).map(([k, v]) => { const got = p.badges.some((b) => b.badgeCode === k); return (
            <div key={k} className={`rounded-lg border p-2 text-center ${got ? 'border-amber/40 bg-amber/10' : 'border-line opacity-50'}`}><Award className={`w-5 h-5 mx-auto ${got ? 'text-amber' : 'text-faint'}`} /><div className="text-[10px] mt-1">{v}</div></div>); })}</div></Panel>
          <Panel title="Lessons">{LESSONS.map(([t]) => <button key={t} onClick={() => setLesson(LESSONS.find((l) => l[0] === t))} className="w-full flex items-center gap-2 py-2 text-sm text-left hover:text-cyan"><BookOpen className="w-4 h-4 text-cyan" />{t}</button>)}</Panel>
        </div>
      </>) : p.fnoUnlocked ? <Panel title="Options payoff & margin simulator" action={<Badge tone="up"><Unlock className="w-3 h-3" />Unlocked</Badge>}><Payoff /></Panel> : (
        <Panel><div className="text-center py-12"><Lock className="w-8 h-8 text-amber mx-auto" /><h2 className="font-semibold mt-3">F&O practice is locked</h2>
          <p className="text-sm text-muted mt-1 max-w-md mx-auto">SEBI studies show about 9 in 10 retail F&O traders lose money. Pass a 6-question risk quiz (5 correct) to unlock practice.</p>
          <Button className="mt-5" onClick={openQuiz}>Take the risk quiz</Button></div></Panel>)}
      <Modal open={!!quiz} onClose={() => setQuiz(null)} title="F&O risk quiz" wide>
        {quiz && <div className="space-y-5">{quiz.map((q) => (
          <fieldset key={q.id}><legend className="text-sm font-semibold mb-2">{q.id}. {q.question}</legend>
            <div className="grid sm:grid-cols-2 gap-2">{q.options.map((o, i) => (
              <label key={o} className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm cursor-pointer ${answers[q.id] === i ? 'border-cyan bg-cyan/10' : 'border-line hover:bg-raised'}`}>
                <input type="radio" name={`q${q.id}`} className="sr-only" checked={answers[q.id] === i} onChange={() => setAnswers({ ...answers, [q.id]: i })} />{o}</label>))}</div></fieldset>))}
          <Button className="w-full" disabled={Object.keys(answers).length < quiz.length} onClick={submit}>Submit answers</Button></div>}
      </Modal>
      <Modal open={!!lesson} onClose={() => setLesson(null)} title={lesson?.[0]}><p className="text-sm text-muted leading-relaxed">{lesson?.[1]}</p></Modal>
    </>
  );
}
