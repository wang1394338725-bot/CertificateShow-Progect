export interface LoginRequest {
  username: string;
  password: string;
}

export interface UserInfo {
  id: number;
  username: string;
  role: string;
  realName: string;
}

export interface LoginData extends UserInfo {
  token: string;
}

export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string;
}

export interface Certificate {
  id: number;
  title: string;
  recipient: string;
  eventName: string;
  awardLevel: string;
  projectName?: string;
  awardDate: string;
  status: number;
  imageUrl: string;
  isPinned: boolean;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}
