import { createContext, useCallback, useContext, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { AlertTriangle, CheckCircle2, Info, X, XCircle } from 'lucide-react';

const ToastCtx = createContext(null);
const icons = { success: CheckCircle2, error: XCircle, warning: AlertTriangle, info: Info };
const tones = { success: 'text-up', error: 'text-down', warning: 'text-amber', info: 'text-cyan' };

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const dismiss = useCallback((id) => setToasts((t) => t.filter((x) => x.id !== id)), []);
  const push = useCallback((type, title, body) => {
    const id = Math.random().toString(36).slice(2);
    setToasts((t) => [...t.slice(-4), { id, type, title, body }]);
    setTimeout(() => dismiss(id), type === 'error' ? 7000 : 4500);
  }, [dismiss]);
  const api = { success: (t, b) => push('success', t, b), error: (t, b) => push('error', t, b), warning: (t, b) => push('warning', t, b), info: (t, b) => push('info', t, b) };
  return (
    <ToastCtx.Provider value={api}>
      {children}
      <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 w-[min(380px,calc(100vw-2rem))]" role="status" aria-live="polite">
        <AnimatePresence initial={false}>
          {toasts.map((t) => {
            const Icon = icons[t.type];
            return (
              <motion.div key={t.id} layout initial={{ opacity: 0, y: 16, scale: 0.96 }} animate={{ opacity: 1, y: 0, scale: 1 }} exit={{ opacity: 0, x: 40 }}
                transition={{ type: 'spring', stiffness: 420, damping: 32 }} className="glass p-3.5 flex gap-3 shadow-card">
                <Icon className={`w-5 h-5 shrink-0 mt-0.5 ${tones[t.type]}`} />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-semibold">{t.title}</div>
                  {t.body && <div className="text-xs text-muted mt-0.5 break-words">{t.body}</div>}
                </div>
                <button onClick={() => dismiss(t.id)} className="text-faint hover:text-ink" aria-label="Dismiss"><X className="w-4 h-4" /></button>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>
    </ToastCtx.Provider>
  );
}
export const useToast = () => useContext(ToastCtx);
