// 全局 axios 拦截器：闲置超过 token 时限后，下一次请求自动刷新续期（无感刷新）
// 在 main.ts 中导入一次即可，对所有 `import axios from 'axios'` 的页面生效（默认实例是单例）
import axios from "axios";
import { ElMessage } from "element-plus";

// 与后端 application.yml 的 jwt.expiration（3600000ms = 1 小时）保持一致
const TOKEN_EXPIRE_MS = 60 * 60 * 1000;

let lastActivity = Date.now();
let refreshing: Promise<boolean> | null = null; // 单飞（single-flight）：并发请求只触发一次刷新

// 刷新用独立实例，避免触发下面的全局拦截器造成递归
const rawAxios = axios.create();

async function refreshToken(): Promise<boolean> {
  const token = localStorage.getItem("token");
  if (!token) return false;
  try {
    // 后端对"已过期但签名有效"的 token 仍会换发新 token；签名无效返回 code 401
    const res = await rawAxios.post(
      "/api/certificate/refresh-token",
      {},
      {
        headers: { Authorization: `Bearer ${token}` },
      },
    );
    if (res.data.code === 200 && res.data.data) {
      localStorage.setItem("token", res.data.data);
      return true;
    }
    // code 401 = 签名无效或会话超期（超过 max-session-days），服务端明确要求重新登录
    forceRelogin(String(res.data.message || "登录已过期，请重新登录"));
    return false;
  } catch {
    // 网络异常：不强制登出（token 可能仍有效），本次请求按原样发出
    return false;
  }
}

function forceRelogin(reason: string) {
  ElMessage.warning(reason);
  localStorage.removeItem("token");
  localStorage.removeItem("userinfo");
  if (window.location.pathname !== "/login") {
    window.location.href = "/login";
  }
}

// 单飞：同一时刻只发一次刷新请求，其余请求等待同一个结果
function ensureFreshToken(): Promise<boolean> {
  if (!refreshing) {
    refreshing = refreshToken().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

axios.interceptors.request.use(async (config) => {
  const token = localStorage.getItem("token");
  const idle = Date.now() - lastActivity;

  // 闲置超过 token 时限：本次请求先等待刷新完成，再携带新 token 发出（用户无感知）；
  // 刷新被拒（会话超期/签名无效）时 refreshToken 内部已强制跳转登录页
  if (token && idle >= TOKEN_EXPIRE_MS) {
    await ensureFreshToken();
  }

  // 始终携带 localStorage 中最新的 token（刷新成功后此处已是新 token）
  const latest = localStorage.getItem("token");
  if (latest) config.headers.set("Authorization", `Bearer ${latest}`);

  lastActivity = Date.now(); // 任何请求都视为一次操作
  return config;
});

// 统一响应处理：业务码非 200 弹出后端 message 并 reject；
// 页面只需写成功分支，catch 里不要再 ElMessage.error（会双重弹窗）
axios.interceptors.response.use(
  (res) => {
    const body = res.data;
    if (body && typeof body === "object" && "code" in body && body.code !== 200) {
      ElMessage.error(String(body.message || "操作失败"));
      return Promise.reject(body);
    }
    return res;
  },
  () => {
    ElMessage.error("网络异常，请稍后重试");
    return Promise.reject(new Error("network"));
  },
);
