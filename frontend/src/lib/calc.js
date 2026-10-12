// Client-side helpers for calculators that run on user-entered values (not market data).

/** Option payoff at expiry per unit for a single leg. */
export function legPayoff(leg, spot) {
  const { type, side, strike, premium } = leg;
  const intrinsic = type === 'CE' ? Math.max(spot - strike, 0) : Math.max(strike - spot, 0);
  const longPnl = intrinsic - premium;
  return side === 'BUY' ? longPnl : -longPnl;
}

export function strategyPayoff(legs, lotSize, from, to, steps = 120) {
  const pts = [];
  for (let i = 0; i <= steps; i++) {
    const s = from + ((to - from) * i) / steps;
    pts.push({ spot: s, pnl: legs.reduce((a, l) => a + legPayoff(l, s) * lotSize * (l.lots || 1), 0) });
  }
  const pnls = pts.map((p) => p.pnl);
  const breakEvens = [];
  for (let i = 1; i < pts.length; i++) if (Math.sign(pts[i - 1].pnl) !== Math.sign(pts[i].pnl)) breakEvens.push((pts[i - 1].spot + pts[i].spot) / 2);
  // unbounded detection: slope at edges
  const slopeHi = pts[pts.length - 1].pnl - pts[pts.length - 2].pnl;
  const slopeLo = pts[1].pnl - pts[0].pnl;
  return {
    points: pts,
    maxProfit: slopeHi > 1e-9 || slopeLo < -1e-9 ? Infinity : Math.max(...pnls),
    maxLoss: slopeHi < -1e-9 || slopeLo > 1e-9 ? -Infinity : Math.min(...pnls),
    breakEvens,
  };
}
