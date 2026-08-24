import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 3000,
    host: '0.0.0.0',
    // 云 IDE（CloudStudio/Codespaces）公网预览域名不固定，显式放行常见预览域名及其子域
    allowedHosts: [
      'localhost',
      '.cloudstudio.club',
      '.ap-shanghai2.cloudstudio.club',
      '.cloudstudio.works',
      '.gitee.io',
      '.githubpreview.dev',
      '.preview.app.github.dev',
      '.app.github.dev',
    ],
    // 跨域公网预览时常因 HMR 的 ws 连 localhost 触发浏览器 PNA 拦截；
    // 设置环境变量 VITE_NO_HMR=1 可关闭热更新（换来预览稳定）。
    hmr: process.env.VITE_NO_HMR === '1' ? false : undefined,
    proxy: {
      '/api': {
        target: 'http://localhost:8000',
        changeOrigin: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks: {
          echarts: ['echarts'],
          'element-plus': ['element-plus'],
        },
      },
    },
  },
})
