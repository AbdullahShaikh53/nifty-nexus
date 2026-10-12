import { Activity, BarChart3, Bell, BookOpen, Bot, CandlestickChart, ClipboardList, FileText, FlaskConical, GraduationCap, History, LayoutDashboard, LineChart, NotebookPen, Settings, Shield, ShieldCheck, Star, Wallet } from 'lucide-react';

export const NAV = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/markets', label: 'Markets', icon: Activity },
  { to: '/app/watchlists', label: 'Watchlists', icon: Star },
  { to: '/app/charts', label: 'Charts', icon: CandlestickChart },
  { to: '/app/trade', label: 'Paper Trading', icon: LineChart },
  { to: '/app/orders', label: 'Orders', icon: ClipboardList },
  { to: '/app/portfolio', label: 'Portfolio', icon: Wallet },
  { to: '/app/journal', label: 'Trading Journal', icon: NotebookPen },
  { to: '/app/risk', label: 'Risk Analytics', icon: ShieldCheck },
  { to: '/app/coach', label: 'AI Trade Coach', icon: Bot },
  { to: '/app/strategy', label: 'Strategy Lab', icon: FlaskConical },
  { to: '/app/replay', label: 'Historical Replay', icon: History },
  { to: '/app/learn', label: 'Learning Hub', icon: GraduationCap },
  { to: '/app/alerts', label: 'Alerts', icon: Bell },
  { to: '/app/reports', label: 'Reports', icon: FileText },
  { to: '/app/settings', label: 'Settings', icon: Settings },
  { to: '/app/admin', label: 'Admin Console', icon: Shield, admin: true },
];
export const EXTRA = [{ to: '/app/insights', label: 'Behavioural Insights', icon: BarChart3 }, { to: '/app/learn', label: 'F&O Practice', icon: BookOpen }];
