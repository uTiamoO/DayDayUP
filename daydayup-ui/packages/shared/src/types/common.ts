/**
 * 字典（对齐后端 DictVO）
 */
export interface Dict {
  id: number;
  code: string;
  name: string;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface DictQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  status?: number;
}

/** 新建/更新字典（对齐 DictCreateDTO / DictUpdateDTO） */
export interface DictForm {
  code: string;
  name: string;
  remark?: string;
}

/**
 * 字典项（对齐后端 DictItemVO）
 */
export interface DictItem {
  id: number;
  dictCode: string;
  value: string;
  label: string;
  sort?: number;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

export interface DictItemQuery {
  pageNum: number;
  pageSize: number;
  dictCode?: string;
  label?: string;
  status?: number;
}

/** 新建/更新字典项（对齐 DictItemCreateDTO / DictItemUpdateDTO） */
export interface DictItemForm {
  dictCode: string;
  value: string;
  label: string;
  sort?: number;
  remark?: string;
}

/**
 * 操作日志（只读，对齐后端 OperLogVO）
 */
export interface OperLog {
  id: number;
  userId?: number;
  username?: string;
  module?: string;
  operation?: string;
  method?: string;
  requestUrl?: string;
  requestMethod?: string;
  requestIp?: string;
  requestParams?: string;
  responseBody?: string;
  success: number;
  errorMsg?: string;
  costMs?: number;
  operTime?: string;
}

export interface OperLogQuery {
  pageNum: number;
  pageSize: number;
  username?: string;
  module?: string;
  success?: number;
  startTime?: string;
  endTime?: string;
}

/**
 * 创建 API Key 的返回结果（后端返回 Map<String,String>，如 apiKey/secret）。
 * 后端仅支持创建与吊销，无列表接口。
 */
export interface ApiKeyCreateResult {
  [key: string]: string;
}

/**
 * 仪表盘统计（对齐后端 DashboardStatsVO）
 */
export interface DashboardStats {
  userCount: number;
  roleCount: number;
  menuCount: number;
  dictCount: number;
}
