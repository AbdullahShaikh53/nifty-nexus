import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { LineChart } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtINR, fmtTime } from '../lib/format';
import { Badge, Button, DataTable, PageHeader, Panel, SimBadge, Skeleton } from '../components/ui';
import TradeTicket from '../components/trade/TradeTicket';
import SymbolPicker from '../components/trade/SymbolPicker';
import { IndexCard } from './Overview';
import { useToast } from '../context/ToastContext';

export const statusTone = (s) => ({ FILLED: 'up', OPEN: 'cyan', CANCELLED: 'neutral', REJECTED: 'down', EXPIRED: 'amber', PARTIAL: 'violet' }[s] || 'neutral');

export default function Trade() {
  const [params] = useSearchParams(); const [symbol, setSymbol] = useState(params.get('symbol') || 'RELIANCE'); const side = params.get('side') === 'SELL' ? 'SELL' : 'BUY'; const orders = useApi('/orders'); const toast = useToast();
  const cancel = async (id) => { try { await api.delete(`/orders/${id}`); orders.reload(); toast.info('Order cancelled'); } catch (e) { toast.error('Cancel failed', errorMessage(e)); } };
  return (
    <>
      <PageHeader icon={LineChart} title="Paper Trading" subtitle="Orders fill at the server price — never a price from your browser"><SimBadge /></PageHeader>
      <div className="grid lg:grid-cols-[380px_1fr] gap-3">
        <Panel title="Order ticket">
          <SymbolPicker value={symbol} onChange={setSymbol} className="mb-4" />
          <TradeTicket key={side} symbol={symbol} initialSide={side} onPlaced={() => orders.reload()} />
        </Panel>
        <div className="space-y-3 min-w-0">
          <div className="grid grid-cols-2 gap-3"><IndexCard symbol={symbol} label={`${symbol} · live`} /><IndexCard symbol="NIFTY 50" /></div>
          <Panel title="Recent orders" bodyClass="p-0">
            {!orders.data ? <Skeleton className="h-40 m-3" /> : (
              <DataTable rows={orders.data.slice(0, 15)} columns={[
                { key: 'placedAt', label: 'Time', render: (o) => <span className="text-xs text-muted">{fmtTime(o.placedAt)}</span> },
                { key: 'symbol', label: 'Symbol', render: (o) => <span className="font-semibold">{o.symbol}</span> },
                { key: 'side', label: 'Side', render: (o) => <span className={o.side === 'BUY' ? 'text-up' : 'text-down'}>{o.side}</span> },
                { key: 'type', label: 'Type' },
                { key: 'quantity', label: 'Qty', align: 'right', render: (o) => <span className="num">{Number(o.quantity)}</span> },
                { key: 'limitPrice', label: 'Limit', align: 'right', render: (o) => <span className="num">{o.limitPrice ? fmtINR(o.limitPrice) : '—'}</span> },
                { key: 'status', label: 'Status', render: (o) => <Badge tone={statusTone(o.status)}>{o.status}</Badge> },
                { key: 'x', label: '', render: (o) => o.status === 'OPEN' ? <Button variant="plain" className="!py-1 !px-2 text-xs" onClick={() => cancel(o.id)}>Cancel</Button> : null },
              ]} />)}
          </Panel>
        </div>
      </div>
    </>
  );
}
