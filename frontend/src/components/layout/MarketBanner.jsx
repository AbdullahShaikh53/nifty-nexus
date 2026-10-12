import { Clock } from 'lucide-react';
import { useMarket } from '../../context/MarketContext';

/** FR-67: "Market closed" banner with the next open time. */
export default function MarketBanner() {
  const { status } = useMarket();
  if (!status || status.marketOpen || status.simulateOpen) return null;
  const next = status.nextOpen ? new Date(status.nextOpen + '+05:30').toLocaleString('en-IN', { weekday: 'short', day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kolkata' }) : '—';
  return (
    <div className="mx-4 md:mx-6 mt-4 rounded-lg border border-amber/30 bg-amber/10 text-amber text-xs px-3 py-2 flex items-center gap-2">
      <Clock className="w-3.5 h-3.5 shrink-0" />
      <span><b>Market closed.</b> Orders open again {next} IST. Charts, portfolio, insights, backtesting and replay remain available.</span>
    </div>
  );
}
