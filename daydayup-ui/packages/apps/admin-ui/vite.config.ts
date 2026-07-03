import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath, URL } from 'node:url';

// 说明：开发环境前端直连网关（VITE_GATEWAY_BASE_URL，默认 http://127.0.0.1:9000）。
// 网关 CORS 已放行 http://127.0.0.1:* 与 http://localhost:*，故无需 vite 代理。
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true
  }
});
