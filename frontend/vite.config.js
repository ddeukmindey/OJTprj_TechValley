import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
//
// QUAN TRỌNG: trong lúc dev (npm run dev), Vite chạy ở port 5173 (origin khác
// với backend). Ta proxy /api và /docs sang nginx (port 80, do docker-compose
// dựng) để CODE GỌI API GIỐNG HỆT PRODUCTION — không có nhánh if(dev)/if(prod)
// nào cả, không có biến IP nào phải đổi khi bàn giao.
//
// Điều kiện: phải chạy `docker compose up -d` (đã có nginx expose 80:80)
// trước khi chạy `npm run dev`.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost',
        changeOrigin: true,
      },
      '/docs': {
        target: 'http://localhost',
        changeOrigin: true,
      },
    },
  },
})
