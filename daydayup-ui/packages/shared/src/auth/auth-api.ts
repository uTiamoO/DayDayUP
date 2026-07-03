import { HttpClient } from '../api/http';
import { UserContext } from '../types/user';

/**
 * 登录请求参数
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Token 响应
 * 字段与后端 LoginVO 对齐（camelCase）：accessToken / refreshToken / expiresIn
 */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

/**
 * 刷新 Token 请求
 */
export interface RefreshTokenRequest {
  refreshToken: string;
}

/**
 * 认证 API 类
 */
export class AuthApi {
  private httpClient: HttpClient;

  constructor(httpClient: HttpClient) {
    this.httpClient = httpClient;
  }

  /**
   * 登录（DirectToken 流程）
   * POST /auth/login
   */
  async login(data: LoginRequest): Promise<TokenResponse> {
    return this.httpClient.post<TokenResponse>('/auth/login', data, { useAuth: false });
  }

  /**
   * 刷新 Token
   * POST /auth/refresh
   */
  async refresh(refreshToken: string): Promise<TokenResponse> {
    return this.httpClient.post<TokenResponse>(
      '/auth/refresh',
      { refreshToken },
      { useAuth: false }
    );
  }

  /**
   * 登出
   * POST /auth/logout
   */
  async logout(): Promise<void> {
    return this.httpClient.post<void>('/auth/logout', {});
  }

  /**
   * 获取当前用户信息
   * GET /admin/me
   */
  async getCurrentUser(): Promise<UserContext> {
    return this.httpClient.get<UserContext>('/admin/me');
  }
}

export const createAuthApi = (httpClient: HttpClient): AuthApi => {
  return new AuthApi(httpClient);
};
