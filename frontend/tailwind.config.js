/** Design tokens live as CSS variables in src/index.css so dark/light themes swap instantly. */
const v = (name) => `rgb(var(--${name}) / <alpha-value>)`;
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: ['class'],
  theme: {
    extend: {
      colors: {
        bg: v('bg'), surface: v('surface'), raised: v('raised'), line: v('line'),
        ink: v('ink'), muted: v('muted'), faint: v('faint'),
        up: v('up'), down: v('down'), cyan: v('cyan'), violet: v('violet'), amber: v('amber'),
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'ui-monospace', 'monospace'],
      },
      borderRadius: { xl: '14px', '2xl': '20px' },
      boxShadow: {
        card: '0 1px 0 0 rgb(255 255 255 / 0.03) inset, 0 12px 32px -16px rgb(0 0 0 / 0.5)',
        glow: '0 0 0 1px rgb(var(--up) / 0.35), 0 8px 40px -8px rgb(var(--up) / 0.45)',
      },
      keyframes: {
        flashUp: { '0%': { backgroundColor: 'rgb(var(--up) / 0.25)' }, '100%': { backgroundColor: 'transparent' } },
        flashDown: { '0%': { backgroundColor: 'rgb(var(--down) / 0.25)' }, '100%': { backgroundColor: 'transparent' } },
        gridpan: { '0%': { backgroundPosition: '0 0' }, '100%': { backgroundPosition: '48px 48px' } },
        pulseDot: { '0%,100%': { opacity: 1 }, '50%': { opacity: 0.35 } },
        shimmer: { '0%': { backgroundPosition: '-400px 0' }, '100%': { backgroundPosition: '400px 0' } },
      },
      animation: {
        flashUp: 'flashUp 900ms ease-out', flashDown: 'flashDown 900ms ease-out',
        gridpan: 'gridpan 8s linear infinite', pulseDot: 'pulseDot 1.6s ease-in-out infinite',
        shimmer: 'shimmer 1.4s linear infinite',
      },
    },
  },
  safelist: [{ pattern: /^(bg|text)-(up|down|cyan|violet|amber)(\/(10|20))?$/ }],
  plugins: [],
};
