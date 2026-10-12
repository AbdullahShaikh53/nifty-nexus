import { lazy, Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { MarketProvider } from './context/MarketContext';
import { ThemeProvider } from './context/ThemeContext';
import { ToastProvider } from './context/ToastContext';
import AppShell from './components/layout/AppShell';
import Landing from './pages/Landing';
import { Login, Register } from './pages/Auth';
import NotFound from './pages/NotFound';
import ErrorBoundary from './components/ErrorBoundary';

// Authenticated pages are code-split.
const Overview = lazy(() => import('./pages/Overview'));
const Markets = lazy(() => import('./pages/Markets'));
const Watchlists = lazy(() => import('./pages/Watchlists'));
const Charts = lazy(() => import('./pages/Charts'));
const Trade = lazy(() => import('./pages/Trade'));
const Orders = lazy(() => import('./pages/Orders'));
const Portfolio = lazy(() => import('./pages/Portfolio'));
const Journal = lazy(() => import('./pages/Journal'));
const Insights = lazy(() => import('./pages/Insights'));
const Risk = lazy(() => import('./pages/Risk'));
const Coach = lazy(() => import('./pages/Coach'));
const StrategyLab = lazy(() => import('./pages/StrategyLab'));
const Replay = lazy(() => import('./pages/Replay'));
const Learning = lazy(() => import('./pages/Learning'));
const Alerts = lazy(() => import('./pages/Alerts'));
const Reports = lazy(() => import('./pages/Reports'));
const Settings = lazy(() => import('./pages/Settings'));
const Admin = lazy(() => import('./pages/Admin'));

function Splash() {
  return <div className="h-screen grid place-items-center"><div className="w-10 h-10 rounded-full border-2 border-line border-t-up animate-spin" aria-label="Loading" /></div>;
}

function Protected({ children, admin }) {
  const { user, loading, isAdmin } = useAuth(); const loc = useLocation();
  if (loading) return <Splash />;
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname + loc.search }} replace />;
  if (admin && !isAdmin) return <Navigate to="/app" replace />;
  return children;
}

function PublicOnly({ children }) {
  const { user, loading } = useAuth();
  if (loading) return <Splash />;
  return user ? <Navigate to="/app" replace /> : children;
}

export default function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider><ToastProvider><AuthProvider><MarketProvider>
        <BrowserRouter>
          <Suspense fallback={<Splash />}>
            <Routes>
              <Route path="/" element={<Landing />} />
              <Route path="/login" element={<PublicOnly><Login /></PublicOnly>} />
              <Route path="/register" element={<PublicOnly><Register /></PublicOnly>} />
              <Route path="/app" element={<Protected><AppShell /></Protected>}>
                <Route index element={<Overview />} />
                <Route path="markets" element={<Markets />} />
                <Route path="watchlists" element={<Watchlists />} />
                <Route path="charts" element={<Charts />} />
                <Route path="trade" element={<Trade />} />
                <Route path="orders" element={<Orders />} />
                <Route path="portfolio" element={<Portfolio />} />
                <Route path="journal" element={<Journal />} />
                <Route path="insights" element={<Insights />} />
                <Route path="risk" element={<Risk />} />
                <Route path="coach" element={<Coach />} />
                <Route path="strategy" element={<StrategyLab />} />
                <Route path="replay" element={<Replay />} />
                <Route path="learn" element={<Learning />} />
                <Route path="alerts" element={<Alerts />} />
                <Route path="reports" element={<Reports />} />
                <Route path="settings" element={<Settings />} />
                <Route path="admin" element={<Protected admin><Admin /></Protected>} />
                <Route path="*" element={<NotFound />} />
              </Route>
              <Route path="*" element={<NotFound />} />
            </Routes>
          </Suspense>
        </BrowserRouter>
      </MarketProvider></AuthProvider></ToastProvider></ThemeProvider>
    </ErrorBoundary>
  );
}
