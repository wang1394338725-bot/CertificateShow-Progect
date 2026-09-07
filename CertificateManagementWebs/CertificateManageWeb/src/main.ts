import { createApp } from "vue";
import App from "./App.vue";
import { createPinia } from "pinia";
import "element-plus/dist/index.css";
import "element-plus/theme-chalk/dark/css-vars.css"; // 黑色模式变量（主页 html.dark 时生效）
import router from "./router/index.ts";
import "./styles/global.css";
import "./api/axiosSetup"; // 全局 axios 拦截器：token 闲置超时自动续期

const app = createApp(App);
app.use(createPinia());
app.use(router);
app.mount("#app");
