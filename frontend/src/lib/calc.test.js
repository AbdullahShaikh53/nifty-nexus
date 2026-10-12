import { describe, expect, it } from 'vitest';
import { legPayoff, strategyPayoff } from './calc';

describe('option payoff', () => {
  it('long call loses only premium below strike', () => {
    expect(legPayoff({ type: 'CE', side: 'BUY', strike: 100, premium: 5 }, 90)).toBe(-5);
    expect(legPayoff({ type: 'CE', side: 'BUY', strike: 100, premium: 5 }, 110)).toBe(5);
  });
  it('long call has unlimited profit and premium-limited loss', () => {
    const r = strategyPayoff([{ type: 'CE', side: 'BUY', strike: 100, premium: 5 }], 1, 50, 150);
    expect(r.maxProfit).toBe(Infinity); expect(r.maxLoss).toBeCloseTo(-5); expect(r.breakEvens[0]).toBeCloseTo(105, 0);
  });
  it('short put has limited profit', () => {
    const r = strategyPayoff([{ type: 'PE', side: 'SELL', strike: 100, premium: 4 }], 1, 0, 200);
    expect(r.maxProfit).toBeCloseTo(4);
  });
});
