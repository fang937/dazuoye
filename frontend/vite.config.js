import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';

export default {
  plugins: [vue()],

  // 相对路径：构建产物放进 Spring Boot 的 static 目录后，不管从根路径还是子路径访问都能加载到资源。
  base: './',

  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },

  server: {
    port: 5174,
    // 开发模式：前端跑在 5174，/api 请求由 Vite 代理到后端 8082，页面里不用写死后端地址
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  },

  build: {
    // 默认交付模式：直接构建进后端的静态目录，启动 Spring Boot 访问 8082 就是完整系统。
    // Docker 构建时用 VITE_OUT_DIR=dist 覆盖，先产出独立 dist，再在镜像里拷进后端资源目录。
    // 这个目录里的内容是构建产物，不手工编辑，随时可以用 npm run build 重新生成。
    outDir: process.env.VITE_OUT_DIR || '../backend/src/main/resources/static',
    emptyOutDir: true
  }
};
