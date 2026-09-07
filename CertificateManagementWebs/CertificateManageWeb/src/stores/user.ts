// src/stores/user.ts
import { defineStore } from "pinia";
import type { UserInfo } from "../api/api";

export const useUserStore = defineStore("user", {
  state: () => ({
    token: null as string | null,
    userInfo: null as UserInfo | null,
  }),

  getters: {
    // 是否已登录
    isLoggedIn: (state) => !!state.token,
    // 用户名（仅当登录后才有值）
    userName: (state) => state.userInfo?.username || null,
    // 用户角色（仅当登录后才有值）
    userRole: (state) => state.userInfo?.role || null,
    // 真实姓名（仅当登录后才有值）
    realName: (state) => state.userInfo?.realName || null,
  },

  actions: {
    // 设置登录信息（仅修改状态，不做持久化）
    setLoginData(token: string, userInfo: UserInfo) {
      this.token = token;
      this.userInfo = userInfo;
    },

    // 清除登录信息（仅修改状态，不做持久化）
    clearLoginData() {
      this.token = null;
      this.userInfo = null;
    },
  },
});
