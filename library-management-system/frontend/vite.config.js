import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development, calls to /api are forwarded to the Spring Boot server,
// so the browser sees a single origin and no CORS setup is needed.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // Point at another backend with: API_TARGET=http://localhost:8081 npm run dev
        target: process.env.API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
