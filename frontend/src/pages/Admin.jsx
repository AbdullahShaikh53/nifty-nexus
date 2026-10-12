import { useState } from 'react';
import { Shield, Trash2 } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtTime } from '../lib/format';
import { Badge, Button, DataTable, ErrorState, Metric, PageHeader, Panel, Skeleton, Tabs } from '../components/ui';
import { useToast } from '../context/ToastContext';

/** Admin console. Every call is protected server-side by hasRole('ADMIN'); a non-admin gets 403. */
export default function Admin() {
  const [tab, setTab] = useState('overview'); const toast = useToast();
  const stats = useApi('/admin/stats', { poll: 10000 }); const users = useApi('/admin/users'); const audit = useApi(tab === 'audit' ? '/admin/audit' : null);
  const hol = useApi(tab === 'calendar' ? '/admin/holidays' : null); const charges = useApi(tab === 'charges' ? '/admin/charges' : null);
  const [hd, setHd] = useState({ date: '', description: '' });
  const act = async (fn, ok) => { try { await fn(); toast.success(ok); } catch (e) { toast.error('Action failed', errorMessage(e)); } };
  if (stats.error) return <ErrorState message={stats.error} onRetry={stats.reload} />;
  const s = stats.data; const simOpen = s?.settings?.find((x) => x.key === 'SIMULATE_MARKET_OPEN')?.value === 'true';
  return (
    <>
      <PageHeader icon={Shield} title="Admin Console" subtitle="Users, platform health, configuration and audit">
        <Tabs tabs={['overview', 'users', 'charges', 'calendar', 'audit'].map((t) => ({ value: t, label: t[0].toUpperCase() + t.slice(1) }))} value={tab} onChange={setTab} />
      </PageHeader>
      {!s ? <Skeleton className="h-64" /> : tab === 'overview' ? (<>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
          <Metric label="Registered users" value={<span className="num">{s.totalUsers}</span>} sub={`${s.blockedUsers} blocked · ${s.activeUsers} joined in 30 d`} />
          <Metric label="Orders" accent="up" value={<span className="num">{s.totalOrders}</span>} sub={`${s.filledOrders} filled · ${s.rejectedOrders} rejected · ${s.openOrders} open`} />
          <Metric label="Trades" accent="violet" value={<span className="num">{s.totalTrades}</span>} />
          <Metric label="Market feed" accent="amber" value={s.feed.connected ? (s.feed.stale ? 'Stale' : 'Healthy') : 'Down'} sub={`${s.feed.provider}${s.feed.simulated ? ' · simulated' : ''}`} />
        </div>
        <div className="grid lg:grid-cols-2 gap-3 mt-3">
          <Panel title="Demo controls" subtitle="Every change is written to the audit log">
            <div className="flex items-center justify-between gap-4"><div><div className="text-sm font-medium">Simulate market open</div><p className="text-xs text-muted">Accept orders outside NSE hours for demos and viva. Off by default.</p></div>
              <Button variant={simOpen ? 'danger' : 'primary'} onClick={() => act(() => api.post('/admin/settings', { key: 'SIMULATE_MARKET_OPEN', value: String(!simOpen) }).then(stats.reload), simOpen ? 'Simulate-open disabled' : 'Simulate-open enabled')}>{simOpen ? 'Turn off' : 'Turn on'}</Button></div>
            <div className="mt-4 text-xs text-muted">Slippage: <span className="num text-ink">{s.settings.find((x) => x.key === 'SLIPPAGE_PCT')?.value}%</span></div>
          </Panel>
          <Panel title="Most common trading mistakes" subtitle="Across all users (rule-based)">
            {Object.keys(s.commonMistakes).length === 0 ? <p className="text-sm text-muted">No patterns detected yet.</p> : Object.entries(s.commonMistakes).sort((a, b) => b[1] - a[1]).map(([k, v]) => (
              <div key={k} className="flex items-center gap-3 mb-2 text-sm"><span className="w-40 text-xs">{k.replace('_', ' ')}</span><div className="flex-1 h-2 bg-raised rounded"><div className="h-full bg-amber rounded" style={{ width: `${(v / s.totalUsers) * 100}%` }} /></div><span className="num w-6 text-right">{v}</span></div>))}
          </Panel>
        </div>
      </>) : tab === 'users' ? (
        <Panel bodyClass="p-0">{!users.data ? <Skeleton className="h-40 m-3" /> : <DataTable rows={users.data} columns={[
          { key: 'name', label: 'Name', render: (u) => <div><div className="font-medium">{u.name}</div><div className="text-[11px] text-faint">{u.email}</div></div> },
          { key: 'role', label: 'Role', render: (u) => <Badge tone={u.role === 'ADMIN' ? 'violet' : 'neutral'}>{u.role}</Badge> },
          { key: 'createdAt', label: 'Joined', render: (u) => <span className="text-xs text-muted">{fmtTime(u.createdAt)}</span> },
          { key: 'blocked', label: 'Status', render: (u) => u.blocked ? <Badge tone="down">Blocked</Badge> : <Badge tone="up">Active</Badge> },
          { key: 'x', label: 'Actions', render: (u) => <div className="flex gap-1">
            <Button variant="ghost" className="!py-1 !px-2 text-xs" onClick={() => act(() => api.post(`/admin/users/${u.id}/block`, null, { params: { blocked: !u.blocked } }).then(users.reload), u.blocked ? 'User unblocked' : 'User blocked')}>{u.blocked ? 'Unblock' : 'Block'}</Button>
            <Button variant="plain" className="!py-1 !px-2 text-xs" onClick={() => act(() => api.post(`/admin/users/${u.id}/role`, null, { params: { role: u.role === 'ADMIN' ? 'USER' : 'ADMIN' } }).then(users.reload), 'Role updated')}>{u.role === 'ADMIN' ? 'Make user' : 'Make admin'}</Button></div> },
        ]} />}</Panel>
      ) : tab === 'charges' ? (
        <Panel title="Charges configuration" subtitle="Verify against current official rates before a demo" bodyClass="p-0">{!charges.data ? <Skeleton className="h-40 m-3" /> : <DataTable rows={charges.data.map((c) => ({ ...c, id: c.key }))} columns={[
          { key: 'key', label: 'Key', render: (c) => <span className="num text-xs">{c.key}</span> }, { key: 'description', label: 'Description', render: (c) => <span className="text-xs text-muted">{c.description}</span> },
          { key: 'rate', label: 'Rate', align: 'right', render: (c) => <input className="input num !py-1 w-32 text-right" defaultValue={c.rate} onBlur={(e) => e.target.value !== String(c.rate) && act(() => api.post('/admin/charges', { key: c.key, rate: e.target.value }).then(charges.reload), 'Rate updated')} /> }]} />}</Panel>
      ) : tab === 'calendar' ? (
        <Panel title="NSE holiday calendar" subtitle="Orders are rejected on these dates">
          <div className="flex flex-wrap gap-2 mb-4"><input type="date" className="input w-auto" value={hd.date} onChange={(e) => setHd({ ...hd, date: e.target.value })} /><input className="input w-56" placeholder="Description" value={hd.description} onChange={(e) => setHd({ ...hd, description: e.target.value })} />
            <Button disabled={!hd.date} onClick={() => act(() => api.post('/admin/holidays', hd).then(() => { setHd({ date: '', description: '' }); hol.reload(); }), 'Holiday added')}>Add</Button></div>
          {!hol.data ? <Skeleton className="h-40" /> : <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2">{[...hol.data].sort((a, b) => a.date.localeCompare(b.date)).map((h) => (
            <div key={h.date} className="flex items-center justify-between rounded-lg border border-line px-3 py-2 text-sm"><span><span className="num">{h.date}</span> · {h.description}</span>
              <button className="text-faint hover:text-down" aria-label="Remove" onClick={() => act(() => api.delete(`/admin/holidays/${h.date}`).then(hol.reload), 'Holiday removed')}><Trash2 className="w-4 h-4" /></button></div>))}</div>}
        </Panel>
      ) : (
        <Panel title="Audit log" subtitle="Logins, orders, admin actions, simulate-open usage" bodyClass="p-0">{!audit.data ? <Skeleton className="h-40 m-3" /> : <DataTable rows={audit.data} columns={[
          { key: 'createdAt', label: 'Time', render: (a) => <span className="text-xs text-muted">{fmtTime(a.createdAt)}</span> }, { key: 'actor', label: 'Actor', render: (a) => <span className="text-xs">{a.actor}</span> },
          { key: 'action', label: 'Action', render: (a) => <Badge tone={a.action.includes('FAIL') || a.action.includes('BLOCK') ? 'down' : a.action.includes('SIMULATE') ? 'amber' : 'neutral'}>{a.action}</Badge> }, { key: 'detail', label: 'Detail', render: (a) => <span className="text-xs text-muted">{a.detail}</span> }]} />}</Panel>
      )}
    </>
  );
}
