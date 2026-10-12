import { useState } from 'react';
import { ClipboardList } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtINR, fmtTime, tone } from '../lib/format';
import { Badge, Button, DataTable, EmptyState, ErrorState, PageHeader, Panel, Skeleton, Tabs } from '../components/ui';
import { statusTone } from './Trade';
import { useToast } from '../context/ToastContext';

export default function Orders() {
  const [tab, setTab] = useState('open'); const orders = useApi('/orders', { poll: 5000 }); const trades = useApi('/orders/trades', { poll: 5000 }); const toast = useToast();
  const cancel = async (id) => { try { await api.delete(`/orders/${id}`); orders.reload(); toast.info('Order cancelled'); } catch (e) { toast.error('Cancel failed', errorMessage(e)); } };
  const list = orders.data || [];
  const rows = tab === 'open' ? list.filter((o) => o.status === 'OPEN') : list;
  return (
    <>
      <PageHeader icon={ClipboardList} title="Orders" subtitle="Order book, history and executed trades">
        <Tabs tabs={[{ value: 'open', label: `Order book (${list.filter((o) => o.status === 'OPEN').length})` }, { value: 'all', label: 'History' }, { value: 'trades', label: 'Trades' }]} value={tab} onChange={setTab} />
      </PageHeader>
      <Panel bodyClass="p-0">
        {orders.error ? <ErrorState message={orders.error} onRetry={orders.reload} /> : !orders.data ? <Skeleton className="h-60 m-3" /> : tab === 'trades' ? (
          <DataTable rows={trades.data || []} empty={<EmptyState title="No trades yet" />} columns={[
            { key: 'executedAt', label: 'Executed', render: (t) => <span className="text-xs text-muted">{fmtTime(t.executedAt)}</span> },
            { key: 'symbol', label: 'Symbol', render: (t) => <span className="font-semibold">{t.symbol}</span> },
            { key: 'side', label: 'Side', render: (t) => <span className={t.side === 'BUY' ? 'text-up' : 'text-down'}>{t.side}</span> },
            { key: 'fillQuantity', label: 'Qty', align: 'right', render: (t) => <span className="num">{Number(t.fillQuantity)}</span> },
            { key: 'fillPrice', label: 'Fill price', align: 'right', render: (t) => <span className="num">{fmtINR(t.fillPrice)}</span> },
            { key: 'charges', label: 'Charges', align: 'right', render: (t) => <span className="num text-muted">{fmtINR(t.charges)}</span> },
            { key: 'realisedPnl', label: 'Realised P&L', align: 'right', render: (t) => t.side === 'SELL' ? <span className={`num ${tone(t.realisedPnl)}`}>{fmtINR(t.realisedPnl)}</span> : <span className="text-faint">—</span> },
          ]} />
        ) : (
          <DataTable rows={rows} empty={<EmptyState title={tab === 'open' ? 'No open orders' : 'No orders yet'} body="Limit orders waiting for their price appear here and expire at 3:30 PM IST." />} columns={[
            { key: 'placedAt', label: 'Placed', render: (o) => <span className="text-xs text-muted">{fmtTime(o.placedAt)}</span> },
            { key: 'id', label: '#', render: (o) => <span className="num text-faint">{o.id}</span> },
            { key: 'symbol', label: 'Symbol', render: (o) => <span className="font-semibold">{o.symbol}</span> },
            { key: 'side', label: 'Side', render: (o) => <span className={o.side === 'BUY' ? 'text-up' : 'text-down'}>{o.side}</span> },
            { key: 'type', label: 'Type' },
            { key: 'quantity', label: 'Qty', align: 'right', render: (o) => <span className="num">{Number(o.filledQuantity)}/{Number(o.quantity)}</span> },
            { key: 'limitPrice', label: 'Limit', align: 'right', render: (o) => <span className="num">{o.limitPrice ? fmtINR(o.limitPrice) : 'MKT'}</span> },
            { key: 'stopLoss', label: 'SL / Target', align: 'right', render: (o) => <span className="num text-xs text-muted">{o.stopLoss ? Number(o.stopLoss).toFixed(2) : '—'} / {o.targetPrice ? Number(o.targetPrice).toFixed(2) : '—'}</span> },
            { key: 'status', label: 'Status', render: (o) => <span title={o.rejectReason || ''}><Badge tone={statusTone(o.status)}>{o.status}</Badge></span> },
            { key: 'x', label: '', render: (o) => o.status === 'OPEN' ? <Button variant="plain" className="!py-1 !px-2 text-xs" onClick={() => cancel(o.id)}>Cancel</Button> : o.rejectReason ? <span className="text-[11px] text-faint">{o.rejectReason}</span> : null },
          ]} />)}
      </Panel>
    </>
  );
}
