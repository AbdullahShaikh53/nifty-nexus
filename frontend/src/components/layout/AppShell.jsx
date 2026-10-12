import { Suspense, useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { Bell, ChevronsLeft, ChevronsRight, LayoutDashboard, LineChart, LogOut, Menu, Moon, Search, Activity, Sun, User, Wallet, Zap } from 'lucide-react';
import { handleOf, traderId } from '../../lib/user';
import { NAV } from './nav';
import Logo from './Logo';
import FeedStatus from './FeedStatus';
import MarketBanner from './MarketBanner';
import CommandPalette from './CommandPalette';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useMarket } from '../../context/MarketContext';
import { useToast } from '../../context/ToastContext';
import { Skeleton } from '../ui';
import { fmtTime } from '../../lib/format';

function Sidebar({ collapsed, setCollapsed, mobile, onNavigate }) {
  const { isAdmin } = useAuth();
  return (
    <motion.aside animate={{ width: mobile ? 260 : collapsed ? 72 : 240 }} transition={{ type: 'spring', stiffness: 380, damping: 36 }}
      className="h-full flex flex-col border-r border-line bg-surface/80 backdrop-blur">
      <div className="h-14 flex items-center px-4 border-b border-line"><Logo compact={collapsed && !mobile} /></div>
      <nav className="flex-1 overflow-y-auto p-2 space-y-0.5" aria-label="Main">
        {NAV.filter((n) => !n.admin || isAdmin).map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end} onClick={onNavigate} title={collapsed ? n.label : undefined}
            className={({ isActive }) => `relative flex items-center gap-3 px-3 py-2 rounded-lg text-[13px] font-medium transition ${isActive ? 'text-ink' : 'text-muted hover:text-ink hover:bg-raised/70'}`}>
            {({ isActive }) => (<>
              {isActive && <motion.span layoutId="nav-active" className="absolute inset-0 rounded-lg bg-raised border border-line" transition={{ type: 'spring', stiffness: 500, damping: 40 }} />}
              {isActive && <span className="absolute left-0 top-2 bottom-2 w-[3px] rounded-r bg-up" />}
              <n.icon className={`relative w-4 h-4 shrink-0 ${isActive ? 'text-up' : ''} ${n.admin ? 'text-violet' : ''}`} />
              {(!collapsed || mobile) && <span className="relative truncate">{n.label}</span>}
            </>)}
          </NavLink>
        ))}
      </nav>
      {(!collapsed || mobile) && <div className="mx-3 mb-1 px-3 py-2 rounded-lg border border-line bg-raised/50 text-[11px] text-muted">Built by <span className="text-ink font-semibold">Abdullah Shaikh</span></div>}
      {!mobile && (
        <button onClick={() => setCollapsed((c) => !c)} className="m-2 p-2 rounded-lg text-faint hover:text-ink hover:bg-raised flex items-center justify-center gap-2 text-xs" aria-label="Toggle sidebar">
          {collapsed ? <ChevronsRight className="w-4 h-4" /> : <><ChevronsLeft className="w-4 h-4" />Collapse</>}
        </button>
      )}
    </motion.aside>
  );
}

function Notifications() {
  const { userEvents, setUserEvents } = useMarket(); const [open, setOpen] = useState(false);
  return (
    <div className="relative">
      <button onClick={() => setOpen((o) => !o)} className="relative p-2 rounded-lg hover:bg-raised text-muted hover:text-ink" aria-label="Notifications">
        <Bell className="w-4 h-4" />
        {userEvents.length > 0 && <span className="absolute top-1 right-1 min-w-[16px] h-4 px-1 rounded-full bg-down text-[10px] text-white grid place-items-center num">{userEvents.length}</span>}
      </button>
      <AnimatePresence>{open && (
        <motion.div initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} className="absolute right-0 mt-2 w-80 card p-2 z-50">
          <div className="flex items-center justify-between px-2 py-1.5"><span className="label">Notifications</span>
            {userEvents.length > 0 && <button className="text-xs text-cyan" onClick={() => setUserEvents([])}>Clear</button>}</div>
          {userEvents.length === 0 ? <p className="text-xs text-muted px-2 py-4 text-center">You’re all caught up.</p> : (
            <ul className="max-h-80 overflow-auto">{userEvents.map((e, i) => (
              <li key={i} className="px-2 py-2 rounded-lg hover:bg-raised text-xs">
                <div className="font-semibold">{e.type === 'FILL' ? `Filled: ${e.trade.side} ${Number(e.trade.fillQuantity)} ${e.trade.symbol} @ ${Number(e.trade.fillPrice).toFixed(2)}` : e.type === 'ALERT' ? `Alert: ${e.message}` : `Order ${e.order?.status}: ${e.order?.symbol}`}</div>
                <div className="text-faint mt-0.5">{fmtTime(e.at)}</div>
              </li>))}</ul>)}
        </motion.div>)}</AnimatePresence>
    </div>
  );
}

export default function AppShell() {
  const [collapsed, setCollapsed] = useState(false); const [mobileOpen, setMobileOpen] = useState(false); const [palette, setPalette] = useState(false); const [menu, setMenu] = useState(false);
  const { user, logout } = useAuth(); const { theme, toggle } = useTheme(); const { watchUser } = useMarket(); const toast = useToast();
  const loc = useLocation(); const nav = useNavigate();

  useEffect(() => {
    const h = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') { e.preventDefault(); setPalette((p) => !p); }
      if (e.altKey && e.key.toLowerCase() === 't') { e.preventDefault(); nav('/app/trade'); }
    };
    window.addEventListener('keydown', h); return () => window.removeEventListener('keydown', h);
  }, [nav]);

  useEffect(() => watchUser(user?.id, (ev) => {
    if (ev.type === 'FILL') toast.success('Order filled', `${ev.trade.side} ${Number(ev.trade.fillQuantity)} ${ev.trade.symbol} @ ₹${Number(ev.trade.fillPrice).toFixed(2)}`);
    if (ev.type === 'ALERT') toast.info('Price alert', ev.message);
    if (ev.type === 'ORDER' && ev.order?.status === 'EXPIRED') toast.warning('Order expired', `${ev.order.symbol} limit order expired at close`);
  }), [user?.id]); // eslint-disable-line

  return (
    <div className="h-screen flex overflow-hidden noise">
      <div className="hidden lg:block h-full shrink-0"><Sidebar collapsed={collapsed} setCollapsed={setCollapsed} /></div>
      <AnimatePresence>{mobileOpen && (
        <motion.div className="lg:hidden fixed inset-0 z-[70] flex" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
          <motion.div initial={{ x: -280 }} animate={{ x: 0 }} exit={{ x: -280 }} transition={{ type: 'spring', stiffness: 400, damping: 40 }} className="h-full">
            <Sidebar mobile collapsed={false} setCollapsed={() => {}} onNavigate={() => setMobileOpen(false)} />
          </motion.div>
          <div className="flex-1 bg-black/50" onClick={() => setMobileOpen(false)} />
        </motion.div>)}</AnimatePresence>

      <div className="flex-1 flex flex-col min-w-0 relative z-10">
        <header className="h-14 shrink-0 flex items-center gap-2 px-3 md:px-5 border-b border-line bg-bg/80 backdrop-blur sticky top-0 z-40">
          <button className="lg:hidden p-2 rounded-lg hover:bg-raised" onClick={() => setMobileOpen(true)} aria-label="Open menu"><Menu className="w-5 h-5" /></button>
          <button onClick={() => setPalette(true)} className="flex items-center gap-2 h-9 px-3 rounded-lg bg-raised border border-line text-faint text-sm w-full max-w-xs hover:border-cyan/40 transition" aria-label="Search">
            <Search className="w-4 h-4" /><span className="truncate">Search stocks, pages…</span>
            <kbd className="ml-auto hidden sm:inline text-[10px] border border-line rounded px-1.5 py-0.5">Ctrl K</kbd>
          </button>
          <div className="flex-1" />
          <FeedStatus />
          <button onClick={() => nav('/app/trade')} className="btn-primary !py-1.5 !px-3 text-xs hidden sm:inline-flex" title="Quick trade (Alt+T)"><Zap className="w-3.5 h-3.5" />Trade</button>
          <Notifications />
          <button onClick={toggle} className="p-2 rounded-lg hover:bg-raised text-muted hover:text-ink" aria-label="Toggle theme">{theme === 'dark' ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}</button>
          <div className="relative">
            <button onClick={() => setMenu((m) => !m)} className="flex items-center gap-2 p-1 pr-2 rounded-lg hover:bg-raised" aria-label="Profile menu">
              <span className="w-7 h-7 rounded-lg bg-gradient-to-br from-up to-cyan text-bg grid place-items-center text-xs font-bold">{user?.name?.[0]?.toUpperCase()}</span>
              <span className="hidden md:block text-xs font-medium max-w-[110px] truncate">{user?.name}</span>
            </button>
            <AnimatePresence>{menu && (
              <motion.div initial={{ opacity: 0, y: -6 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} className="absolute right-0 mt-2 w-56 card p-1.5 z-50" onMouseLeave={() => setMenu(false)}>
                <div className="px-3 py-2 border-b border-line mb-1"><div className="text-sm font-semibold truncate">{user?.name}</div><div className="text-xs text-cyan truncate">{handleOf(user)} · ID {traderId(user)}</div><div className="text-xs text-faint truncate">{user?.email}</div></div>
                <button onClick={() => { setMenu(false); nav('/app/settings'); }} className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-raised text-sm"><User className="w-4 h-4" />Settings</button>
                <button onClick={logout} className="w-full flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-raised text-sm text-down"><LogOut className="w-4 h-4" />Log out</button>
              </motion.div>)}</AnimatePresence>
          </div>
        </header>
        <MarketBanner />
        <main className="flex-1 overflow-y-auto pb-20 lg:pb-0">
          <AnimatePresence mode="wait">
            <motion.div key={loc.pathname} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -6 }} transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
              className="p-4 md:p-6 max-w-[1600px] mx-auto">
              <Suspense fallback={<div className="space-y-3"><Skeleton className="h-8 w-56" /><Skeleton className="h-40" /><Skeleton className="h-64" /></div>}>
                <Outlet />
              </Suspense>
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
      <CommandPalette open={palette} setOpen={setPalette} />
      {/* Mobile bottom navigation */}
      <nav className="lg:hidden fixed bottom-0 inset-x-0 z-50 border-t border-line bg-surface/95 backdrop-blur grid grid-cols-5 pb-[env(safe-area-inset-bottom)]" aria-label="Quick navigation">
        {[['/app', 'Home', LayoutDashboard], ['/app/markets', 'Markets', Activity], ['/app/trade', 'Trade', Zap], ['/app/portfolio', 'Portfolio', Wallet], ['/app/charts', 'Charts', LineChart]].map(([to, label, Icon]) => (
          <NavLink key={to} to={to} end={to === '/app'} className={({ isActive }) => `flex flex-col items-center justify-center gap-0.5 py-2 text-[10px] font-medium ${isActive ? 'text-up' : 'text-muted'}`}>
            <Icon className="w-5 h-5" />{label}
          </NavLink>))}
      </nav>
    </div>
  );
}
