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
      path: "/list",
      name: "PublicList",
      component: () => import("../page/PublicBrowseView.vue"),
      meta: {
        title: "奖状一览",
      },
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
      redirect: "/admin/browse", // 直接访问 /admin 或免密自动进入时，落到第一个功能页
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
            requiresAuth: true,
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
  // 访问登录页时检测本地 token：有效（或可无感刷新）则直接进后台，无需重复输密码；
  // 仅当主动退出登录（token 被清除）后才需要再次输入账号密码
  if (to.name === "Login") {
    if (loadToken()) {
      return "/admin"; // 已登录 -> 免密进入后台
    }
    return true; // 未登录 -> 正常显示登录页
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
