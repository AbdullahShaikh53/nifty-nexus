import { useState } from 'react';
import { NotebookPen, Plus } from 'lucide-react';
import { useApi } from '../lib/useApi';
import { api, errorMessage } from '../lib/api';
import { fmtTime } from '../lib/format';
import { Badge, Button, EmptyState, ErrorState, Field, Modal, PageHeader, Skeleton } from '../components/ui';
import { useToast } from '../context/ToastContext';

export default function Journal() {
  const { data, error, reload } = useApi('/journal'); const toast = useToast();
  const [edit, setEdit] = useState(null); const [filter, setFilter] = useState('');
  const save = async () => {
    try { if (edit.id) await api.put(`/journal/${edit.id}`, { notes: edit.notes, tags: edit.tags, reason: edit.reason }); else await api.post('/journal', edit); setEdit(null); reload(); toast.success('Journal saved'); }
    catch (e) { toast.error('Save failed', errorMessage(e)); }
  };
  const rows = (data || []).filter((j) => !filter || `${j.symbol} ${j.tags} ${j.reason} ${j.notes}`.toLowerCase().includes(filter.toLowerCase()));
  return (
    <>
      <PageHeader icon={NotebookPen} title="Trading Journal" subtitle="Auto-built from every order — add notes, tags and lessons">
        <input className="input w-48" placeholder="Search journal…" value={filter} onChange={(e) => setFilter(e.target.value)} aria-label="Search journal" />
        <Button onClick={() => setEdit({ symbol: '', reason: '', tags: '', notes: '' })}><Plus className="w-4 h-4" />Entry</Button>
      </PageHeader>
      {error ? <ErrorState message={error} onRetry={reload} /> : !data ? <Skeleton className="h-64" /> : rows.length === 0 ? <EmptyState icon={NotebookPen} title="No journal entries" body="Every order you place creates an entry automatically." /> : (
        <ol className="relative border-l border-line ml-3 space-y-4">
          {rows.map((j) => (
            <li key={j.id} className="ml-5">
              <span className="absolute -left-[5px] mt-2 w-2.5 h-2.5 rounded-full bg-cyan ring-4 ring-bg" />
              <button onClick={() => setEdit({ ...j })} className="card p-4 w-full text-left hover:border-cyan/40 transition">
                <div className="flex flex-wrap items-center gap-2"><span className="font-semibold">{j.symbol}</span><span className="text-xs text-faint">{fmtTime(j.createdAt)}</span>
                  <div className="flex-1" />{(j.tags || '').split(',').filter(Boolean).map((t) => <Badge key={t}>{t.trim()}</Badge>)}</div>
                <p className="text-sm mt-1.5"><span className="text-muted">Reason:</span> {j.reason || '—'}</p>
                {j.notes && <p className="text-xs text-muted mt-1 whitespace-pre-wrap">{j.notes}</p>}
              </button>
            </li>))}
        </ol>)}
      <Modal open={!!edit} onClose={() => setEdit(null)} title={edit?.id ? 'Edit journal entry' : 'New journal entry'}>
        {edit && <div className="space-y-3">
          {!edit.id && <Field label="Symbol"><input className="input" value={edit.symbol} onChange={(e) => setEdit({ ...edit, symbol: e.target.value.toUpperCase() })} /></Field>}
          <Field label="Reason"><input className="input" value={edit.reason || ''} onChange={(e) => setEdit({ ...edit, reason: e.target.value })} maxLength={255} /></Field>
          <Field label="Tags (comma separated)"><input className="input" value={edit.tags || ''} onChange={(e) => setEdit({ ...edit, tags: e.target.value })} maxLength={255} /></Field>
          <Field label="Notes & lessons"><textarea className="input min-h-[120px]" value={edit.notes || ''} onChange={(e) => setEdit({ ...edit, notes: e.target.value })} /></Field>
          <Button className="w-full" onClick={save}>Save</Button></div>}
      </Modal>
    </>
  );
}
