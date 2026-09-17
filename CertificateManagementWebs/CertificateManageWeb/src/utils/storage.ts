import type { UserInfo } from "../api/api";

const TOKEN_KEY = "token";
const USER_INFO_KEY = "userinfo";

// 保存 token
export const saveAuthData = (token: string, userinfo: UserInfo) => {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_INFO_KEY, JSON.stringify(userinfo));
};

// 加载 token
export const loadToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY);
};

export const loadUserInfo = (): UserInfo | null => {
  const data = localStorage.getItem(USER_INFO_KEY);
  if (!data) return null;
  try {
    return JSON.parse(data) as UserInfo;
  } catch {
    // 解析失败时返回 null，并清除异常数据
    localStorage.removeItem(USER_INFO_KEY);
    return null;
  }
};

// 清除所有认证数据
export const clearAuthData = () => {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_INFO_KEY);
};
