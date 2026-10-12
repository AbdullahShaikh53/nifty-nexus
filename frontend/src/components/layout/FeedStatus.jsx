import { Radio, WifiOff } from 'lucide-react';
import { useMarket } from '../../context/MarketContext';
import { fmtTime } from '../../lib/format';

/** Market status + live-feed connection + stale-data badge (FR-8, FR-73). */
export default function FeedStatus() {
  const { status, connected } = useMarket();
  const stale = !connected || status?.stale;
  return (
    <div className="hidden md:flex items-center gap-2 text-[11px]">
      <span className={`chip border ${(status?.marketOpen || status?.simulateOpen) ? 'bg-up/10 text-up border-up/25' : 'bg-raised text-muted border-line'}`}>
        <span className={`w-1.5 h-1.5 rounded-full ${(status?.marketOpen || status?.simulateOpen) ? 'bg-up animate-pulseDot' : 'bg-faint'}`} />
        {status?.marketOpen ? 'NSE Open' : status?.simulateOpen ? 'Market Open · Practice' : 'NSE Closed'}
      </span>
      <span title={status?.lastTick ? `Last tick ${fmtTime(status.lastTick)}` : 'No ticks yet'}
        className={`chip border ${stale ? 'bg-down/10 text-down border-down/25' : 'bg-cyan/10 text-cyan border-cyan/25'}`}>
        {stale ? <WifiOff className="w-3 h-3" /> : <Radio className="w-3 h-3" />}
        {stale ? `Stale · ${status?.lastTick ? fmtTime(status.lastTick) : 'no data'}` : status?.simulated ? 'Feed · Simulated' : 'Feed · Live'}
      </span>
    </div>
  );
}
