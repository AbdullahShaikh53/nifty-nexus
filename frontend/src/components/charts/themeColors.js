export function cssColor(name, alpha = 1) {
  const v = getComputedStyle(document.documentElement).getPropertyValue(`--${name}`).trim().split(' ').join(',');
  return `rgba(${v},${alpha})`;
}
export function chartTheme() {
  return {
    layout: { background: { color: 'transparent' }, textColor: cssColor('muted'), fontFamily: 'JetBrains Mono, monospace', fontSize: 11 },
    grid: { vertLines: { color: cssColor('line', 0.45) }, horzLines: { color: cssColor('line', 0.45) } },
    rightPriceScale: { borderColor: cssColor('line') },
    timeScale: { borderColor: cssColor('line'), timeVisible: true, secondsVisible: false },
    crosshair: { mode: 1, vertLine: { color: cssColor('cyan', 0.5), labelBackgroundColor: cssColor('raised') }, horzLine: { color: cssColor('cyan', 0.5), labelBackgroundColor: cssColor('raised') } },
  };
}
