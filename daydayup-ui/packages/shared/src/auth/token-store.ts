const TOKEN_KEY = 'daydayup_access_token';
const REFRESH_TOKEN_KEY = 'daydayup_refresh_token';

/**
 * Token 存储管理类
 * 使用 sessionStorage 存储 token，并提供简单的 Base64 混淆
 */
export class TokenStore {
  private accessToken: string | null = null;
  private refreshToken: string | null = null;

  constructor() {
    this.loadFromStorage();
  }

  /**
   * 从存储中加载 token
   */
  private loadFromStorage() {
    try {
      const encodedAccessToken = sessionStorage.getItem(TOKEN_KEY);
      const encodedRefreshToken = sessionStorage.getItem(REFRESH_TOKEN_KEY);

      if (encodedAccessToken) {
        this.accessToken = this.decode(encodedAccessToken);
      }
      if (encodedRefreshToken) {
        this.refreshToken = this.decode(encodedRefreshToken);
      }
    } catch (error) {
      console.error('Failed to load tokens from storage:', error);
    }
  }

  /**
   * 获取 Access Token
   */
  getAccessToken(): string | null {
    return this.accessToken;
  }

  /**
   * 获取 Refresh Token
   */
  getRefreshToken(): string | null {
    return this.refreshToken;
  }

  /**
   * 设置 Access Token
   */
  setAccessToken(token: string) {
    this.accessToken = token;
    try {
      sessionStorage.setItem(TOKEN_KEY, this.encode(token));
    } catch (error) {
      console.error('Failed to save access token:', error);
    }
  }

  /**
   * 设置 Refresh Token
   */
  setRefreshToken(token: string) {
    this.refreshToken = token;
    try {
      sessionStorage.setItem(REFRESH_TOKEN_KEY, this.encode(token));
    } catch (error) {
      console.error('Failed to save refresh token:', error);
    }
  }

  /**
   * 设置 Token（同时设置 access 和 refresh）
   */
  setTokens(accessToken: string, refreshToken: string) {
    this.setAccessToken(accessToken);
    this.setRefreshToken(refreshToken);
  }

  /**
   * 清除所有 Token
   */
  clearTokens() {
    this.accessToken = null;
    this.refreshToken = null;
    try {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    } catch (error) {
      console.error('Failed to clear tokens:', error);
    }
  }

  /**
   * 检查是否已登录（有 access token）
   */
  isAuthenticated(): boolean {
    return !!this.accessToken;
  }

  /**
   * Base64 编码（简易混淆）
   */
  private encode(str: string): string {
    try {
      return btoa(encodeURIComponent(str));
    } catch {
      return str;
    }
  }

  /**
   * Base64 解码
   */
  private decode(str: string): string {
    try {
      return decodeURIComponent(atob(str));
    } catch {
      return str;
    }
  }
}

export const tokenStore = new TokenStore();
