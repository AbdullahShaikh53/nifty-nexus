import { memo, useEffect, useRef } from 'react';
import { createChart } from 'lightweight-charts';
import { chartTheme, cssColor } from './themeColors';
import { useTheme } from '../../context/ThemeContext';

/** Multi-line chart (equity curves, benchmark comparison). series: [{name,color,data:[{time,value}]}] */
function LineSeriesChart({ series, height = 280, area }) {
  const box = useRef(null); const { theme } = useTheme();
  useEffect(() => {
    if (!box.current) return;
    const chart = createChart(box.current, { ...chartTheme(), height, autoSize: true });
    series.forEach((s, i) => {
      const color = cssColor(s.color || ['cyan', 'violet', 'amber'][i % 3]);
      const ser = area && i === 0
        ? chart.addAreaSeries({ lineColor: color, topColor: cssColor(s.color || 'cyan', 0.25), bottomColor: cssColor(s.color || 'cyan', 0), lineWidth: 2, title: s.name })
        : chart.addLineSeries({ color, lineWidth: 2, title: s.name, lineStyle: s.dashed ? 2 : 0 });
      ser.setData(s.data);
    });
    chart.timeScale().fitContent();
    return () => chart.remove();
  }, [series, height, theme, area]);
  return <div ref={box} style={{ height }} className="w-full" />;
}
export default memo(LineSeriesChart);
