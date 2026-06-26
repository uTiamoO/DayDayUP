import { HttpClient } from '../api/http';

/**
 * 登录请求参数
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Token 响应
 */
export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

/**
 * 刷新 Token 请求
 */
export interface RefreshTokenRequest {
  refresh_token: string;
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
      { refresh_token: refreshToken },
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
  async getCurrentUser(): Promise<any> {
    return this.httpClient.get('/admin/me');
  }
}

export const createAuthApi = (httpClient: HttpClient): AuthApi => {
  return new AuthApi(httpClient);
};
