import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev proxy keeps the JWT cookie same-origin: browser talks only to :5173, Vite forwards to Spring Boot on :8080.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    host: true, // also reachable from a phone on the same Wi-Fi: http://<PC-IP>:5173
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: false },
      '/ws': { target: 'ws://localhost:8080', ws: true, changeOrigin: false },
    },
  },
  build: { chunkSizeWarningLimit: 900 },
  test: { environment: 'node' },
});
