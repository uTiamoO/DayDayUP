/**
 * 统一 API 响应结构
 */
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * 分页查询参数
 */
export interface PageQuery {
  pageNum: number;
  pageSize: number;
}

/**
 * 分页响应结果
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages: number;
}

/**
 * HTTP 请求配置
 */
export interface RequestConfig {
  useAuth?: boolean;
  useSignature?: boolean;
  [key: string]: any;
}

/**
 * 业务错误类
 */
export class BusinessError extends Error {
  code: number;
  timestamp: number;

  constructor(code: number, message: string, timestamp: number) {
    super(message);
    this.code = code;
    this.timestamp = timestamp;
    this.name = 'BusinessError';
  }
}
