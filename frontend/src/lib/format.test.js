import { describe, expect, it } from 'vitest';
import { fmtCompact, fmtINR, fmtPct, tone } from './format';

describe('format', () => {
  it('uses Indian grouping for INR', () => { expect(fmtINR(1000000)).toContain('10,00,000'); });
  it('signs percentages', () => { expect(fmtPct(1.5)).toBe('+1.50%'); expect(fmtPct(-2)).toBe('-2.00%'); });
  it('compacts lakhs and crores', () => { expect(fmtCompact(150000)).toBe('1.50 L'); expect(fmtCompact(25000000)).toBe('2.50 Cr'); });
  it('maps tone', () => { expect(tone(1)).toBe('text-up'); expect(tone(-1)).toBe('text-down'); expect(tone(0)).toBe('text-muted'); });
});
