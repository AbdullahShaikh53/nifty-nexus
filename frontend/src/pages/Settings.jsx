import { Settings as Cog } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { useAuth } from '../context/AuthContext';
import { PageHeader, Panel, Tabs } from '../components/ui';
import { handleOf, traderId } from '../lib/user';

function Toggle({ checked, onChange, label, desc }) {
  return (
    <label className="flex items-center justify-between gap-4 py-3 border-b border-line/60 last:border-0 cursor-pointer">
      <span><span className="text-sm font-medium block">{label}</span><span className="text-xs text-muted">{desc}</span></span>
      <button role="switch" aria-checked={checked} onClick={() => onChange(!checked)} className={`w-10 h-6 rounded-full relative transition ${checked ? 'bg-up' : 'bg-raised border border-line'}`}>
        <span className={`absolute top-0.5 w-5 h-5 rounded-full bg-white shadow transition-all ${checked ? 'left-[18px]' : 'left-0.5'}`} /></button>
    </label>
  );
}

export default function Settings() {
  const { theme, setTheme, largeText, setLargeText, cbSafe, setCbSafe } = useTheme(); const { user } = useAuth();
  return (
    <>
      <PageHeader icon={Cog} title="Settings" subtitle="Appearance, accessibility and account" />
      <div className="grid lg:grid-cols-2 gap-3">
        <Panel title="Appearance & accessibility">
          <div className="flex items-center justify-between py-3 border-b border-line/60"><span className="text-sm font-medium">Theme</span><Tabs tabs={[{ value: 'dark', label: 'Dark' }, { value: 'light', label: 'Light' }]} value={theme} onChange={setTheme} /></div>
          <Toggle checked={largeText} onChange={setLargeText} label="Large text" desc="Increase the base font size by 12.5%" />
          <Toggle checked={cbSafe} onChange={setCbSafe} label="Colour-blind safe colours" desc="Use blue for gains and orange for losses" />
          <div className="py-3 text-sm"><span className="font-medium">Language</span><p className="text-xs text-muted mt-1">English. Hindi and Marathi are planned as an extension (strings are centralised for translation).</p></div>
        </Panel>
        <Panel title="Account">
          <dl className="text-sm space-y-3">
            <div className="flex justify-between"><dt className="text-muted">Name</dt><dd>{user?.name}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Trader ID</dt><dd className="num">{traderId(user)}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Username</dt><dd className="text-cyan">{handleOf(user)}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Email</dt><dd>{user?.email}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Role</dt><dd>{user?.role}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Project by</dt><dd>Abdullah Shaikh</dd></div>
            <div className="flex justify-between"><dt className="text-muted">F&O practice</dt><dd>{user?.fnoUnlocked ? 'Unlocked' : 'Locked'}</dd></div>
          </dl>
          <p className="text-xs text-faint mt-6">Keyboard: <kbd className="border border-line rounded px-1">Ctrl/Cmd K</kbd> command palette · <kbd className="border border-line rounded px-1">Alt T</kbd> quick trade · <kbd className="border border-line rounded px-1">Esc</kbd> close dialogs</p>
        </Panel>
      </div>
    </>
  );
}
