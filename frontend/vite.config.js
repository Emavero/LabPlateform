import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // host: true + allowedHosts: true permettent d'accéder au serveur dev
    // depuis un tunnel externe (ngrok, Cloudflare Tunnel...) sans être
    // bloqué par la vérification du Host HTTP que Vite fait par défaut.
    // À restreindre ou retirer si tu ne fais plus de démo via tunnel.
    host: true,
    allowedHosts: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
