import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react()],
    server: {
      port: 5173,
      host: '0.0.0.0',
      proxy: {
        // Por defecto el backend local escucha en 8080. Si ese puerto está ocupado por otro
        // proyecto, levantá el backend en otro puerto y poné el destino en frontend/.env.local
        // (gitignoreado), por ejemplo: VITE_PROXY_TARGET=http://localhost:8081
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: false,
      minify: 'terser'
    }
  }
})
