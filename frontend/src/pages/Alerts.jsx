import { useState } from 'react';
import { Bell, Trash2 } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtTime } from '../lib/format';
import { Badge, Button, DataTable, EmptyState, Field, PageHeader, Panel } from '../components/ui';
import SymbolPicker from '../components/trade/SymbolPicker';
import { useToast } from '../context/ToastContext';
import { useQuote } from '../context/MarketContext';

export default function Alerts() {
  const { data, reload } = useApi('/alerts', { poll: 5000 }); const toast = useToast();
  const [f, setF] = useState({ symbol: 'RELIANCE', condition: 'PRICE_ABOVE', threshold: '' }); const q = useQuote(f.symbol);
  const create = async () => { try { await api.post('/alerts', { ...f, threshold: Number(f.threshold) }); setF({ ...f, threshold: '' }); reload(); toast.success('Alert created'); } catch (e) { toast.error('Could not create alert', errorMessage(e)); } };
  const del = async (id) => { try { await api.delete(`/alerts/${id}`); reload(); } catch (e) { toast.error('Delete failed', errorMessage(e)); } };
  return (
    <>
      <PageHeader icon={Bell} title="Alerts" subtitle="Price and percentage-move alerts, delivered in-app over WebSocket" />
      <div className="grid lg:grid-cols-[340px_1fr] gap-3">
        <Panel title="New alert">
          <div className="space-y-3">
            <Field label="Symbol"><SymbolPicker value={f.symbol} onChange={(s) => setF({ ...f, symbol: s })} includeIndices /></Field>
            {q && <p className="text-xs text-muted">Current LTP <span className="num text-ink">{Number(q.ltp).toFixed(2)}</span></p>}
            <Field label="Condition"><select className="input" value={f.condition} onChange={(e) => setF({ ...f, condition: e.target.value })}>
              <option value="PRICE_ABOVE">Price rises above</option><option value="PRICE_BELOW">Price falls below</option><option value="PCT_MOVE">Day move exceeds ± %</option></select></Field>
            <Field label={f.condition === 'PCT_MOVE' ? 'Percent' : 'Price'}><input className="input num" type="number" step="0.05" value={f.threshold} onChange={(e) => setF({ ...f, threshold: e.target.value })} /></Field>
            <Button className="w-full" onClick={create} disabled={!(Number(f.threshold) > 0)}>Create alert</Button>
            <p className="text-[11px] text-faint">Volume alerts need exchange volume data; email delivery is outside version 1.</p>
          </div>
        </Panel>
        <Panel title="Your alerts" bodyClass="p-0">
          <DataTable rows={data || []} empty={<EmptyState icon={Bell} title="No alerts" body="Create one to get notified when a level is hit." />} columns={[
            { key: 'symbol', label: 'Symbol', render: (a) => <span className="font-semibold">{a.symbol}</span> },
            { key: 'condition', label: 'Condition', render: (a) => a.condition.replace('_', ' ').toLowerCase() },
            { key: 'threshold', label: 'Level', align: 'right', render: (a) => <span className="num">{Number(a.threshold).toFixed(2)}{a.condition === 'PCT_MOVE' ? '%' : ''}</span> },
            { key: 'active', label: 'Status', render: (a) => a.active ? <Badge tone="cyan" dot>Watching</Badge> : <Badge tone="up">Triggered {fmtTime(a.triggeredAt)}</Badge> },
            { key: 'x', label: '', render: (a) => <button onClick={() => del(a.id)} className="text-faint hover:text-down" aria-label="Delete alert"><Trash2 className="w-4 h-4" /></button> },
          ]} />
        </Panel>
      </div>
    </>
  );
}
