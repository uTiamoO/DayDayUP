/**
 * 当前登录用户上下文（GET /admin/me 返回，对齐后端 UserContext）
 * 仅含身份与权限码，不含 nickname/email 等资料字段
 */
export interface UserContext {
  userId: number;
  username: string;
  authorities: string[];
}

/**
 * 用户详情（管理列表/详情，对齐后端 UserDetailVO）
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
}

/**
 * 用户分页查询参数（对齐 UserPageQuery）
 */
export interface UserQuery {
  pageNum: number;
  pageSize: number;
  username?: string;
  status?: number;
}

/**
 * 新建用户（对齐 UserCreateDTO）
 */
export interface UserCreateForm {
  username: string;
  password: string;
  nickname?: string;
  email?: string;
  mobile?: string;
  roleIds?: number[];
}

/**
 * 更新用户（对齐 UserUpdateDTO；用户名与密码不可通过此接口修改）
 */
export interface UserUpdateForm {
  nickname?: string;
  email?: string;
  mobile?: string;
  roleIds?: number[];
}
