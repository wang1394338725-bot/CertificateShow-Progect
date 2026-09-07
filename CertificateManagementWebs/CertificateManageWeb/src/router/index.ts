// router/index.ts
import { createRouter, createWebHistory } from "vue-router";
import AdminLayout from "../page/AdminLayout.vue";
import { loadToken, loadUserInfo } from "../utils/storage.ts";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "Home",
      component: () => import("../page/HomeView.vue"),
    },
    {
      path: "/login",
      name: "Login",
      component: () => import("../page/Login.vue"),
    },
    {
      path: "/admin",
      name: "AdminLayout",
      component: AdminLayout,
      children: [
        {
          path: "upload",
          name: "Upload",
          component: () => import("../page/UploadView.vue"),
          meta: {
            title: "上传奖状",
          },
        },
        {
          path: "browse",
          name: "Browse",
          component: () => import("../page/BrowseView.vue"),
          meta: {
            title: "奖状浏览",
            keepAlive: true,
          },
        },
        {
          path: "messages",
          name: "Messages",
          component: () => import("../page/MessagesView.vue"),
          meta: {
            title: "消息栏",
            requiresAuth: true,
          },
        },
        {
          path: "manage",
          name: "Manage",
          component: () => import("../page/ManageView.vue"),
          meta: {
            title: "管理员管理",
            role: "SUPER_ADMIN",
          },
        },
      ],
    },
  ],
});

router.beforeEach((to, _from) => {
  if (to.path === "/Login") {
    const token = loadToken(); // 你的读取 Token 方法
    if (token) {
      return "/admin"; // 已登录 -> 去后台
    } else {
      return "/login"; // 未登录 -> 去登录页
    }
  }

  const userinfo = loadUserInfo();
  const token = loadToken();

  console.log("当前路径:", to.path, " token值:", token);

  const requiresAuth = to.meta.requiresAuth || to.meta.role;

  if (requiresAuth && !token) {
    return {
      path: "/login",
      query: { redirect: to.fullPath },
    };
  }

  if (to.meta.role) {
    const requiredRole = to.meta.role as string;
    const userRole = userinfo?.role;

    if (userRole !== requiredRole) {
      return { path: "/admin/browse" };
    }
  }

  return true;
});

export default router;
