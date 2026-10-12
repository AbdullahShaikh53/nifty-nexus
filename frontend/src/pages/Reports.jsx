import { useState } from 'react';
import { Download, FileSpreadsheet, FileText } from 'lucide-react';
import { api, errorMessage } from '../lib/api';
import { Button, PageHeader, Panel } from '../components/ui';
import { useToast } from '../context/ToastContext';

export default function Reports() {
  const [busy, setBusy] = useState(null); const toast = useToast();
  const dl = async (kind) => {
    setBusy(kind);
    try {
      const { data } = await api.get(`/reports/${kind}`, { responseType: 'blob' });
      const url = URL.createObjectURL(data); const a = document.createElement('a'); a.href = url; a.download = `nifty-nexus-report.${kind === 'excel' ? 'xlsx' : 'pdf'}`; a.click(); URL.revokeObjectURL(url);
      toast.success('Report downloaded');
    } catch (e) { toast.error('Download failed', errorMessage(e)); } finally { setBusy(null); }
  };
  return (
    <>
      <PageHeader icon={FileText} title="Reports" subtitle="Export P&L, order history and your trade journal" />
      <div className="grid md:grid-cols-2 gap-3">
        <Panel title="Excel workbook" subtitle="Summary · Trades · Orders · Journal sheets">
          <FileSpreadsheet className="w-10 h-10 text-up mb-4" /><Button onClick={() => dl('excel')} loading={busy === 'excel'}><Download className="w-4 h-4" />Download .xlsx</Button>
        </Panel>
        <Panel title="PDF report" subtitle="Portfolio summary and trade list">
          <FileText className="w-10 h-10 text-down mb-4" /><Button onClick={() => dl('pdf')} loading={busy === 'pdf'}><Download className="w-4 h-4" />Download .pdf</Button>
        </Panel>
      </div>
      <p className="text-xs text-faint mt-4">Tax P&L estimate: paper-trading results are educational. For real trades, short-term gains (held under 12 months) and long-term gains are taxed differently — consult a tax professional.</p>
    </>
  );
}
