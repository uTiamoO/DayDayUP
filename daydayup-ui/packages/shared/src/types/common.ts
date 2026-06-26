/**
 * 字典
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

/**
 * 字典项
 */
export interface DictItem {
  id: number;
  dictCode: string;
  value: string;
  label: string;
  sort: number;
  status: number;
  remark?: string;
  createTime?: string;
  updateTime?: string;
}

/**
 * 操作日志
 */
export interface OperLog {
  id: number;
  userId?: number;
  username?: string;
  module?: string;
  operation?: string;
  requestUrl?: string;
  requestMethod?: string;
  requestIp?: string;
  success: number;
  errorMsg?: string;
  costMs?: number;
  operTime?: string;
}

/**
 * API Key
 */
export interface ApiKey {
  id: number;
  apiKey: string;
  name?: string;
  status: number;
  createTime?: string;
  expireTime?: string;
}
