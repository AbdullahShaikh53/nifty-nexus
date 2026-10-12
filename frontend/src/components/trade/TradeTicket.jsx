import { useEffect, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { AlertTriangle, CheckCircle2, Info, Lightbulb, ShieldAlert, Timer } from 'lucide-react';
import { api, errorMessage } from '../../lib/api';
import { fmtINR, fmtNum } from '../../lib/format';
import { useQuote, useMarket } from '../../context/MarketContext';
import { useToast } from '../../context/ToastContext';
import { Badge, Button, Field, FlashPrice, Tabs } from '../ui';

const TIPS = {
  MARKET: 'Market orders fill immediately at the server price plus a small simulated slippage. Use them when getting in matters more than the exact price.',
  LIMIT: 'Limit orders wait in the order book until the price reaches your level. Unfilled limit orders expire at 3:30 PM IST.',
  SL: 'A stop-loss decides your exit before emotions do. Place it where your trade idea is proven wrong.',
};

/**
 * Trade ticket with pre-trade checklist (FR-21), contextual tips (FR-39), server preview of charges/
 * break-even/risk (FR-18), warnings and a confirmation panel. The browser never sends a fill price.
 */
export default function TradeTicket({ symbol, onPlaced, compact, initialSide = 'BUY' }) {
  const q = useQuote(symbol); const { status } = useMarket(); const toast = useToast();
  const [side, setSide] = useState(initialSide); const [type, setType] = useState('MARKET');
  const [qty, setQty] = useState(10); const [limit, setLimit] = useState(''); const [sl, setSl] = useState(''); const [target, setTarget] = useState('');
  const [reason, setReason] = useState(''); const [tags, setTags] = useState('');
  const [check, setCheck] = useState({ plan: false, size: false, exit: false });
  const [preview, setPreview] = useState(null); const [busy, setBusy] = useState(false); const [ack, setAck] = useState(false);
  const [cooldown, setCooldown] = useState(null); const [wallet, setWallet] = useState(null);
  const marketOpen = !status || status.marketOpen || status.simulateOpen;

  useEffect(() => { setLimit(''); setSl(''); setTarget(''); setPreview(null); }, [symbol]);
  useEffect(() => {
    api.get('/insights').then(({ data }) => setCooldown(data.cooldownActive ? data.currentLossStreak : null)).catch(() => {});
    api.get('/portfolio').then(({ data }) => setWallet(data)).catch(() => {});
  }, [symbol]);

  const body = () => ({
    symbol, side, type, quantity: Number(qty), limitPrice: type === 'LIMIT' && limit ? Number(limit) : null,
    stopLoss: sl ? Number(sl) : null, targetPrice: target ? Number(target) : null, reason: reason || null, tags: tags || null, acknowledgeWarnings: ack,
  });

  const validate = () => {
    if (!symbol) return 'Choose a stock first';
    if (!Number.isInteger(Number(qty)) || Number(qty) < 1) return 'Quantity must be a whole number of at least 1';
    if (type === 'LIMIT' && !(Number(limit) > 0)) return 'Enter a limit price';
    if (side === 'BUY' && sl && q && Number(sl) >= Number(q.ltp)) return 'For a buy, stop-loss must be below the current price';
    if (side === 'BUY' && target && q && Number(target) <= Number(q.ltp)) return 'For a buy, target must be above the current price';
    return null;
  };

  const doPreview = async () => {
    const v = validate(); if (v) { toast.error('Check the ticket', v); return; }
    setBusy(true);
    try { const { data } = await api.post('/orders/preview', body()); setPreview(data); setAck(false); } catch (e) { toast.error('Preview failed', errorMessage(e)); } finally { setBusy(false); }
  };

  const submit = async () => {
    setBusy(true);
    try {
      const { data } = await api.post('/orders', { ...body(), acknowledgeWarnings: ack });
      toast.success(data.status === 'FILLED' ? 'Order filled' : 'Order placed', `${data.side} ${Number(data.quantity)} ${data.symbol} · ${data.status}`);
      setPreview(null); setReason(''); setTags(''); setCheck({ plan: false, size: false, exit: false });
      api.get('/portfolio').then(({ data: d }) => setWallet(d)).catch(() => {});
      onPlaced?.(data);
    } catch (e) { toast.error('Order rejected', errorMessage(e)); } finally { setBusy(false); }
  };

  const est = q ? Number(type === 'LIMIT' && limit ? limit : q.ltp) * Number(qty || 0) : 0;
  const allChecked = check.plan && check.size && check.exit;

  return (
    <div className="space-y-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <div className="text-lg font-bold tracking-tight">{symbol || 'Select a stock'}</div>
          {q && <div className="flex items-baseline gap-2"><FlashPrice value={Number(q.ltp)} format={(v) => fmtINR(v)} className="text-xl font-semibold" />
            <span className={`num text-xs ${q.changePct >= 0 ? 'text-up' : 'text-down'}`}>{q.changePct >= 0 ? '+' : ''}{Number(q.changePct).toFixed(2)}%</span></div>}
        </div>
        {q?.source === 'MOCK' && <Badge tone="amber">Simulated price</Badge>}
      </div>

      <div className="grid grid-cols-2 gap-1 p-1 rounded-lg bg-raised border border-line" role="radiogroup" aria-label="Side">
        {['BUY', 'SELL'].map((s) => (
          <button key={s} role="radio" aria-checked={side === s} onClick={() => setSide(s)}
            className={`py-2 rounded-md text-sm font-bold transition ${side === s ? (s === 'BUY' ? 'bg-up text-bg' : 'bg-down text-white') : 'text-muted hover:text-ink'}`}>{s}</button>))}
      </div>

      <div className="flex items-center justify-between">
        <Tabs tabs={['MARKET', 'LIMIT']} value={type} onChange={setType} />
        <span className="text-[11px] text-faint">Avail. <span className="num text-ink">{fmtINR(wallet?.available, true)}</span></span>
      </div>
      <p className="text-[11px] text-muted flex gap-1.5"><Lightbulb className="w-3.5 h-3.5 text-amber shrink-0 mt-px" />{TIPS[type]}</p>

      <div className="grid grid-cols-2 gap-3">
        <Field label="Quantity"><input className="input num" type="number" min="1" step="1" value={qty} onChange={(e) => setQty(e.target.value)} /></Field>
        {type === 'LIMIT' ? <Field label="Limit price"><input className="input num" type="number" step="0.05" value={limit} placeholder={q ? Number(q.ltp).toFixed(2) : ''} onChange={(e) => setLimit(e.target.value)} /></Field>
          : <Field label="Est. value"><div className="input num bg-transparent">{fmtINR(est)}</div></Field>}
        <Field label="Stop-loss" hint={!compact ? 'Recommended' : undefined}><input className="input num" type="number" step="0.05" value={sl} onChange={(e) => setSl(e.target.value)} placeholder="Optional" /></Field>
        <Field label="Target"><input className="input num" type="number" step="0.05" value={target} onChange={(e) => setTarget(e.target.value)} placeholder="Optional" /></Field>
      </div>
      {sl && target && <p className="text-[11px] text-muted">Stop-loss and target form a bracket (OCO): whichever is hit first exits the position and cancels the other.</p>}

      {!compact && <>
        <Field label="Why this trade? (journal)"><input className="input" value={reason} maxLength={255} onChange={(e) => setReason(e.target.value)} placeholder="e.g. Breakout above 20-day high with volume" /></Field>
        <Field label="Tags"><input className="input" value={tags} maxLength={120} onChange={(e) => setTags(e.target.value)} placeholder="breakout, swing" /></Field>
      </>}

      <fieldset className="rounded-lg border border-line p-3 space-y-2">
        <legend className="label px-1">Pre-trade checklist {allChecked ? <span className="text-up">✓</span> : <span className="normal-case tracking-normal text-faint">(recommended)</span>}</legend>
        {[['plan', 'I have a clear reason for this trade'], ['size', 'Position size risks ≤ 2% of my capital'], ['exit', 'I know where I will exit if wrong']].map(([k, l]) => (
          <label key={k} className="flex items-center gap-2 text-xs cursor-pointer"><input type="checkbox" className="accent-[rgb(var(--up))]" checked={check[k]} onChange={(e) => setCheck({ ...check, [k]: e.target.checked })} />{l}</label>))}
      </fieldset>

      {cooldown && (
        <div className="rounded-lg border border-down/30 bg-down/10 p-3 text-xs flex gap-2"><Timer className="w-4 h-4 text-down shrink-0" />
          <div><b className="text-down">Cooldown suggested.</b> You have {cooldown} losing trades in a row. Take a short break and review your journal before the next trade.</div></div>)}

      <Button className={`w-full ${side === 'SELL' ? '!bg-down !text-white !shadow-none' : ''}`} onClick={doPreview} loading={busy && !preview} disabled={!symbol || !marketOpen}>
        {!marketOpen ? 'Market closed' : `Review ${side.toLowerCase()} order`}
      </Button>
      {!marketOpen && <p className="text-[11px] text-faint text-center">Orders are accepted Mon–Fri, 9:15 AM – 3:30 PM IST (enforced on the server).</p>}

      <AnimatePresence>{preview && (
        <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="overflow-hidden">
          <div className="rounded-xl border border-cyan/30 bg-cyan/5 p-4 space-y-3">
            <div className="flex items-center justify-between"><span className="text-sm font-semibold flex items-center gap-2"><Info className="w-4 h-4 text-cyan" />Confirm order</span><Badge tone="cyan">{preview.priceSource === 'MOCK' ? 'Simulated LTP' : 'Live LTP'}</Badge></div>
            <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 text-xs">
              <dt className="text-muted">{side} × {qty} @</dt><dd className="num text-right">{fmtINR(preview.referencePrice)}</dd>
              <dt className="text-muted">Trade value</dt><dd className="num text-right">{fmtINR(preview.estimatedValue)}</dd>
              <dt className="text-muted">Brokerage</dt><dd className="num text-right">{fmtINR(preview.charges.brokerage)}</dd>
              <dt className="text-muted">STT</dt><dd className="num text-right">{fmtINR(preview.charges.stt)}</dd>
              <dt className="text-muted">Exchange + SEBI</dt><dd className="num text-right">{fmtINR(Number(preview.charges.exchange) + Number(preview.charges.sebi))}</dd>
              <dt className="text-muted">Stamp duty</dt><dd className="num text-right">{fmtINR(preview.charges.stampDuty)}</dd>
              <dt className="text-muted">GST</dt><dd className="num text-right">{fmtINR(preview.charges.gst)}</dd>
              <dt className="font-semibold">Total charges</dt><dd className="num text-right font-semibold">{fmtINR(preview.charges.total)}</dd>
              <dt className="text-muted">Break-even price</dt><dd className="num text-right text-amber">{fmtINR(preview.charges.breakEvenPrice)}</dd>
              <dt className="text-muted">Slippage (simulated)</dt><dd className="num text-right">{fmtNum(preview.slippagePct)}%</dd>
              {preview.riskReward && <><dt className="text-muted">Risk : reward</dt><dd className="num text-right">1 : {preview.riskReward}</dd>
                <dt className="text-muted">Max loss / gain</dt><dd className="num text-right"><span className="text-down">{fmtINR(preview.maxLoss, true)}</span> / <span className="text-up">{fmtINR(preview.maxGain, true)}</span></dd></>}
            </dl>
            {preview.warnings?.length > 0 && (
              <div className="space-y-1.5">{preview.warnings.map((w) => <p key={w} className="text-[11px] text-amber flex gap-1.5"><AlertTriangle className="w-3.5 h-3.5 shrink-0" />{w}</p>)}
                <label className="flex items-center gap-2 text-xs mt-1"><input type="checkbox" checked={ack} onChange={(e) => setAck(e.target.checked)} />I understand the warnings</label></div>)}
            <details className="text-[11px] text-muted"><summary className="cursor-pointer">Compare cost across brokers</summary>
              <ul className="mt-1.5 space-y-1">{preview.charges.brokerComparison.map((b) => <li key={b.broker} className="flex justify-between"><span>{b.broker}</span><span className="num">{fmtINR(b.total)}</span></li>)}</ul></details>
            <div className="flex gap-2">
              <Button variant="ghost" className="flex-1" onClick={() => setPreview(null)}>Edit</Button>
              <Button className={`flex-1 ${side === 'SELL' ? '!bg-down !text-white !shadow-none' : ''}`} loading={busy} disabled={preview.warnings?.length > 0 && !ack} onClick={submit}>
                {side === 'BUY' ? <CheckCircle2 className="w-4 h-4" /> : <ShieldAlert className="w-4 h-4" />}Confirm {side.toLowerCase()}</Button>
            </div>
          </div>
        </motion.div>)}</AnimatePresence>
      <p className="text-[10px] text-faint text-center">Paper trading with virtual money. No real orders are placed.</p>
    </div>
  );
}
