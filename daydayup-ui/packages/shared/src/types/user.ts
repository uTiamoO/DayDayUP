/**
 * 用户详情
 */
export interface UserDetail {
  id: number;
  username: string;
  nickname?: string;
  email?: string;
  mobile?: string;
  status: number;
  lastLoginAt?: string;
  lastLoginIp?: string;
  roleIds?: number[];
  roleCodes?: string[];
  authorities?: string[];
  createTime?: string;
  updateTime?: string;
}

/**
 * 用户查询参数
 */
export interface UserQuery {
  pageNum: number;
  pageSize: number;
  username?: string;
  status?: number;
}

/**
 * 用户创建/更新请求
 */
export interface UserForm {
  username: string;
  nickname?: string;
  email?: string;
  mobile?: string;
  password?: string;
  status?: number;
  roleIds?: number[];
}
