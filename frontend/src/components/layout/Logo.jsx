export default function Logo({ compact }) {
  return (
    <div className="flex items-center gap-2.5 select-none">
      <svg width="28" height="28" viewBox="0 0 32 32" aria-hidden>
        <defs><linearGradient id="lg" x1="0" x2="1" y1="1" y2="0"><stop offset="0" stopColor="rgb(var(--up))" /><stop offset="1" stopColor="rgb(var(--cyan))" /></linearGradient></defs>
        <rect width="32" height="32" rx="9" fill="rgb(var(--raised))" stroke="rgb(var(--line))" />
        <path d="M7 22 L13 14 L18 18 L25 9" fill="none" stroke="url(#lg)" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="25" cy="9" r="2.4" fill="rgb(var(--cyan))" />
      </svg>
      {!compact && <span className="font-extrabold tracking-[0.18em] text-[13px]">NIFTY<span className="text-up"> NEXUS</span></span>}
    </div>
  );
}
