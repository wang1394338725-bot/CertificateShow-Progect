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
  /** 记录名称（原奖状名称） */
  title: string;
  /** 后端字段保留：前端不再展示，新上传固定传空字符串 */
  recipient: string;
  /** 后端字段保留：前端不再展示，新上传固定传空字符串 */
  eventName: string;
  /** 前端隐藏，新上传/编辑固定传「其他」 */
  awardLevel: string;
  /** 后端字段保留：前端不再展示，新上传固定传空字符串 */
  projectName?: string;
  /** 记录日期（原获奖日期） */
  awardDate: string;
  /** 想法/吐槽附录（复用后端 organization 字段），可能为空 */
  organization?: string;
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
