import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import Components from "unplugin-vue-components/vite";
import { ElementPlusResolver } from "unplugin-vue-components/resolvers";
import path from "path";

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  optimizeDeps: {
    // 预构建主页用到的 element-plus 组件，避免首次进页面时按需打包造成约 10 秒卡顿
    include: [
      "element-plus/es",
      "element-plus/es/components/button/index",
      "element-plus/es/components/empty/index",
      "element-plus/es/components/image/index",
      "element-plus/es/components/input/index",
      "element-plus/es/components/pagination/index",
      "element-plus/es/components/radio-group/index",
      "element-plus/es/components/radio-button/index",
      "element-plus/es/components/select/index",
      "element-plus/es/components/option/index",
      "element-plus/es/components/tag/index",
      "element-plus/es/components/dialog/index",
    ],
  },
  server: {
    host: true, // 监听局域网：手机连同一 WiFi 可通过电脑 IP 访问，便于实测移动端效果
    warmup: {
      // 启动时预先编译主页，进一步缩短首次进入时间
      clientFiles: ["./src/page/HomeView.vue"],
    },
    proxy: {
      "/api": {
        target: "http://localhost:9090",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      "/uploads": {
        target: "http://localhost:9090",
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
