import { memo, useEffect, useRef, useState } from 'react';
import { createChart, CrosshairMode } from 'lightweight-charts';
import { chartTheme, cssColor } from './themeColors';
import { useTheme } from '../../context/ThemeContext';
import { useQuote } from '../../context/MarketContext';
import { fmtNum } from '../../lib/format';

/**
 * Interactive TradingView Lightweight Chart: candles/line/area, volume, SMA overlays, RSI pane,
 * crosshair OHLC legend, zoom/pan, and live last-candle updates from the STOMP quote stream.
 */
function PriceChart({ data, mode = 'candles', showVolume = true, showRsi = true, height = 460, live = true, symbol }) {
  const box = useRef(null); const rsiBox = useRef(null);
  const refs = useRef({});
  const [legend, setLegend] = useState(null);
  const { theme } = useTheme();
  const q = useQuote(symbol);

  useEffect(() => {
    if (!box.current || !data) return;
    const chart = createChart(box.current, { ...chartTheme(), height: showRsi ? height - 120 : height, autoSize: true, crosshair: { ...chartTheme().crosshair, mode: CrosshairMode.Normal } });
    const up = cssColor('up'), down = cssColor('down');
    let main;
    if (mode === 'line') main = chart.addLineSeries({ color: cssColor('cyan'), lineWidth: 2 });
    else if (mode === 'area') main = chart.addAreaSeries({ lineColor: cssColor('cyan'), topColor: cssColor('cyan', 0.28), bottomColor: cssColor('cyan', 0), lineWidth: 2 });
    else main = chart.addCandlestickSeries({ upColor: up, downColor: down, borderUpColor: up, borderDownColor: down, wickUpColor: up, wickDownColor: down });
    const candles = data.candles.map((c) => ({ time: c.time, open: +c.open, high: +c.high, low: +c.low, close: +c.close }));
    main.setData(mode === 'candles' ? candles : candles.map((c) => ({ time: c.time, value: c.close })));

    let vol;
    if (showVolume) {
      vol = chart.addHistogramSeries({ priceFormat: { type: 'volume' }, priceScaleId: 'vol' });
      chart.priceScale('vol').applyOptions({ scaleMargins: { top: 0.82, bottom: 0 } });
      vol.setData(data.candles.map((c) => ({ time: c.time, value: c.volume, color: +c.close >= +c.open ? cssColor('up', 0.35) : cssColor('down', 0.35) })));
    }
    const smaColors = [cssColor('amber'), cssColor('violet'), cssColor('cyan')];
    Object.entries(data.indicators || {}).filter(([k]) => k.startsWith('SMA')).forEach(([k, pts], i) => {
      const s = chart.addLineSeries({ color: smaColors[i % 3], lineWidth: 1.5, priceLineVisible: false, lastValueVisible: false, title: k });
      s.setData(pts.map((p) => ({ time: p.time, value: +p.value })));
    });

    let rsiChart;
    if (showRsi && rsiBox.current && data.indicators?.RSI14) {
      rsiChart = createChart(rsiBox.current, { ...chartTheme(), height: 110, autoSize: true, timeScale: { ...chartTheme().timeScale, visible: false } });
      const r = rsiChart.addLineSeries({ color: cssColor('violet'), lineWidth: 1.5, priceLineVisible: false, title: 'RSI 14' });
      r.setData(data.indicators.RSI14.map((p) => ({ time: p.time, value: +p.value })));
      r.createPriceLine({ price: 70, color: cssColor('down', 0.6), lineStyle: 2, lineWidth: 1, axisLabelVisible: true });
      r.createPriceLine({ price: 30, color: cssColor('up', 0.6), lineStyle: 2, lineWidth: 1, axisLabelVisible: true });
      chart.timeScale().subscribeVisibleLogicalRangeChange((rng) => rng && rsiChart.timeScale().setVisibleLogicalRange(rng));
    }
    const last = candles[candles.length - 1];
    setLegend(last);
    chart.subscribeCrosshairMove((p) => {
      const d = p?.seriesData?.get(main);
      if (!d) { setLegend(refs.current.last || last); return; }
      setLegend(d.open !== undefined ? d : { open: d.value, high: d.value, low: d.value, close: d.value });
    });
    chart.timeScale().fitContent();
    if (candles.length > 160) chart.timeScale().setVisibleLogicalRange({ from: candles.length - 160, to: candles.length + 3 });
    refs.current = { chart, main, vol, rsiChart, last, mode };
    return () => { chart.remove(); rsiChart?.remove(); };
  }, [data, mode, showVolume, showRsi, height, theme]);

  // Live update of the latest candle from ticks
  useEffect(() => {
    const r = refs.current; if (!live || !q || !r.main || !r.last) return;
    const price = +q.ltp;
    const updated = { ...r.last, high: Math.max(r.last.high, price), low: Math.min(r.last.low, price), close: price };
    r.last = updated;
    try { r.main.update(r.mode === 'candles' ? updated : { time: updated.time, value: price }); } catch { /* out-of-order guard */ }
    setLegend(updated);
  }, [q, live]);

  return (
    <div className="relative">
      {legend && (
        <div className="absolute top-2 left-3 z-10 flex flex-wrap gap-x-3 gap-y-0.5 text-[11px] num pointer-events-none">
          {['open', 'high', 'low', 'close'].map((k) => <span key={k} className="text-faint">{k[0].toUpperCase()} <span className={legend.close >= legend.open ? 'text-up' : 'text-down'}>{fmtNum(legend[k])}</span></span>)}
        </div>
      )}
      <div ref={box} style={{ height: showRsi ? height - 120 : height }} className="w-full" />
      {showRsi && <div ref={rsiBox} style={{ height: 110 }} className="w-full border-t border-line/60 mt-1" />}
    </div>
  );
}
export default memo(PriceChart);
