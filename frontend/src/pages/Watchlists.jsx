import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Plus, Star, Trash2, X } from 'lucide-react';
import { api, errorMessage } from '../lib/api';
import { useApi } from '../lib/useApi';
import { fmtNum, fmtPct, tone } from '../lib/format';
import { Button, EmptyState, ErrorState, FlashPrice, Modal, PageHeader, Panel, Skeleton } from '../components/ui';
import { useQuote } from '../context/MarketContext';
import SymbolPicker from '../components/trade/SymbolPicker';
import { useToast } from '../context/ToastContext';
import Sparkline from '../components/charts/Sparkline';
import { useTickHistory } from '../components/charts/useTickHistory';

export function WatchRow({ symbol, onRemove, compact, onSelect, active }) {
  const q = useQuote(symbol); const hist = useTickHistory(symbol, 30);
  return (
    <div className={`flex items-center gap-3 px-3 py-2.5 border-b border-line/60 last:border-0 hover:bg-raised/60 ${active ? 'bg-raised' : ''} ${onSelect ? 'cursor-pointer' : ''}`} onClick={onSelect}>
      <div className="min-w-0 flex-1">{onSelect ? <span className="text-sm font-semibold">{symbol}</span> : <Link to={`/app/charts?symbol=${encodeURIComponent(symbol)}`} className="text-sm font-semibold hover:text-cyan">{symbol}</Link>}</div>
      {!compact && <Sparkline values={hist} width={80} height={24} />}
      <div className="text-right w-24">{q ? <><FlashPrice value={Number(q.ltp)} format={fmtNum} className="text-sm" /><div className={`num text-[11px] ${tone(q.changePct)}`}>{fmtPct(q.changePct)}</div></> : <Skeleton className="h-4 w-16 ml-auto" />}</div>
      {onRemove && <button onClick={(e) => { e.stopPropagation(); onRemove(); }} className="text-faint hover:text-down p-1" aria-label={`Remove ${symbol}`}><X className="w-3.5 h-3.5" /></button>}
    </div>
  );
}

export default function Watchlists() {
  const { data, error, loading, reload, setData } = useApi('/watchlists'); const toast = useToast();
  const [adding, setAdding] = useState(null); const [creating, setCreating] = useState(false); const [name, setName] = useState('');
  const replace = (wl) => setData((d) => d.map((x) => (x.id === wl.id ? wl : x)));
  const add = async (id, symbol) => { try { const { data: wl } = await api.post(`/watchlists/${id}/items`, { symbol }); replace(wl); setAdding(null); toast.success('Added', `${symbol} added to watchlist`); } catch (e) { toast.error('Could not add', errorMessage(e)); } };
  const remove = async (id, symbol) => { try { const { data: wl } = await api.delete(`/watchlists/${id}/items/${encodeURIComponent(symbol)}`); replace(wl); } catch (e) { toast.error('Could not remove', errorMessage(e)); } };
  const create = async () => { if (!name.trim()) return; try { await api.post('/watchlists', { name }); setName(''); setCreating(false); reload(); } catch (e) { toast.error('Could not create', errorMessage(e)); } };
  const del = async (id) => { try { await api.delete(`/watchlists/${id}`); reload(); } catch (e) { toast.error('Could not delete', errorMessage(e)); } };
  return (
    <>
      <PageHeader icon={Star} title="Watchlists" subtitle="Track the stocks you care about, live"><Button onClick={() => setCreating(true)}><Plus className="w-4 h-4" />New watchlist</Button></PageHeader>
      {error ? <ErrorState message={error} onRetry={reload} /> : loading ? <Skeleton className="h-64" /> : data.length === 0 ? <EmptyState icon={Star} title="No watchlists" action={<Button onClick={() => setCreating(true)}>Create one</Button>} /> : (
        <div className="grid md:grid-cols-2 xl:grid-cols-3 gap-3">
          {data.map((wl) => (
            <Panel key={wl.id} title={wl.name} subtitle={`${wl.items.length} symbols`} bodyClass="p-0" action={<div className="flex gap-1">
              <button onClick={() => setAdding(wl.id)} className="p-1.5 rounded-md hover:bg-raised text-cyan" aria-label="Add symbol"><Plus className="w-4 h-4" /></button>
              <button onClick={() => del(wl.id)} className="p-1.5 rounded-md hover:bg-raised text-faint hover:text-down" aria-label="Delete watchlist"><Trash2 className="w-4 h-4" /></button></div>}>
              {wl.items.length === 0 ? <EmptyState title="Empty watchlist" body="Add symbols with the + button." /> : wl.items.map((i) => <WatchRow key={i.symbol} symbol={i.symbol} onRemove={() => remove(wl.id, i.symbol)} />)}
            </Panel>))}
        </div>)}
      <Modal open={!!adding} onClose={() => setAdding(null)} title="Add symbol"><SymbolPicker value="" onChange={(s) => add(adding, s)} /></Modal>
      <Modal open={creating} onClose={() => setCreating(false)} title="New watchlist">
        <input className="input" autoFocus value={name} onChange={(e) => setName(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && create()} placeholder="e.g. Banking picks" maxLength={60} />
        <Button className="w-full mt-4" onClick={create}>Create</Button>
      </Modal>
    </>
  );
}
