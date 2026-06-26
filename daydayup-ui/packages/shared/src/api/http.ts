import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import CryptoJS from 'crypto-js';
import { ApiResponse, BusinessError, RequestConfig } from './types';

export class HttpClient {
  private instance: AxiosInstance;
  private tokenGetter?: () => string | null;
  private tokenSetter?: (token: string) => void;
  private tokenClearer?: () => void;
  private refreshTokenFn?: () => Promise<string>;

  constructor(baseURL: string) {
    this.instance = axios.create({
      baseURL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  /**
   * 配置 Token 管理函数
   */
  setTokenManager(
    getter: () => string | null,
    setter: (token: string) => void,
    clearer: () => void,
    refreshFn?: () => Promise<string>
  ) {
    this.tokenGetter = getter;
    this.tokenSetter = setter;
    this.tokenClearer = clearer;
    this.refreshTokenFn = refreshFn;
  }

  /**
   * 设置拦截器
   */
  private setupInterceptors() {
    this.instance.interceptors.request.use(
      (config: InternalAxiosRequestConfig) => {
        const customConfig = config as InternalAxiosRequestConfig & RequestConfig;

        if (customConfig.useAuth !== false && this.tokenGetter) {
          const token = this.tokenGetter();
          if (token) {
            config.headers.Authorization = `Bearer ${token}`;
          }
        }

        if (customConfig.useSignature) {
          this.addSignature(config);
        }

        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    this.instance.interceptors.response.use(
      (response: AxiosResponse<ApiResponse<any>>) => {
        const { code, data, message, timestamp } = response.data;

        if (code === 200) {
          return data;
        } else {
          throw new BusinessError(code, message, timestamp);
        }
      },
      async (error) => {
        if (error.response?.status === 401) {
          if (this.refreshTokenFn) {
            try {
              const newToken = await this.refreshTokenFn();
              if (this.tokenSetter) {
                this.tokenSetter(newToken);
              }
              
              const originalRequest = error.config;
              originalRequest.headers.Authorization = `Bearer ${newToken}`;
              return this.instance.request(originalRequest);
            } catch (refreshError) {
              if (this.tokenClearer) {
                this.tokenClearer();
              }
              throw new BusinessError(401, '登录已过期，请重新登录', Date.now());
            }
          } else {
            if (this.tokenClearer) {
              this.tokenClearer();
            }
            throw new BusinessError(401, '未授权，请先登录', Date.now());
          }
        }

        if (error.response?.status === 403) {
          throw new BusinessError(403, '无权限访问该资源', Date.now());
        }

        if (error.response?.status >= 500) {
          const message = error.response?.data?.message || '服务器内部错误';
          throw new BusinessError(500, message, Date.now());
        }

        if (error.response?.data) {
          const { code, message, timestamp } = error.response.data;
          throw new BusinessError(code, message, timestamp);
        }

        throw new BusinessError(0, error.message || '网络请求失败', Date.now());
      }
    );
  }

  /**
   * 添加接口签名（HmacSHA256）
   */
  private addSignature(config: InternalAxiosRequestConfig) {
    const timestamp = Date.now().toString();
    const nonce = this.generateNonce();
    const secret = (import.meta.env?.VITE_API_SECRET as string) || 'default-secret';

    const signString = `${config.method?.toUpperCase()}${config.url}${timestamp}${nonce}`;
    const signature = CryptoJS.HmacSHA256(signString, secret).toString();

    config.headers['X-Timestamp'] = timestamp;
    config.headers['X-Nonce'] = nonce;
    config.headers['X-Signature'] = signature;
  }

  /**
   * 生成随机 nonce
   */
  private generateNonce(): string {
    return Math.random().toString(36).substring(2, 15) + Math.random().toString(36).substring(2, 15);
  }

  /**
   * GET 请求
   */
  get<T = any>(url: string, config?: AxiosRequestConfig & RequestConfig): Promise<T> {
    return this.instance.get(url, config);
  }

  /**
   * POST 请求
   */
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig & RequestConfig): Promise<T> {
    return this.instance.post(url, data, config);
  }

  /**
   * PUT 请求
   */
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig & RequestConfig): Promise<T> {
    return this.instance.put(url, data, config);
  }

  /**
   * PATCH 请求
   */
  patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig & RequestConfig): Promise<T> {
    return this.instance.patch(url, data, config);
  }

  /**
   * DELETE 请求
   */
  delete<T = any>(url: string, config?: AxiosRequestConfig & RequestConfig): Promise<T> {
    return this.instance.delete(url, config);
  }
}

export const createHttpClient = (baseURL: string): HttpClient => {
  return new HttpClient(baseURL);
};
