import { defineStore } from 'pinia';
import { ref } from 'vue';
import { tokenStore, createHttpClient, createAuthApi, TokenResponse } from '@daydayup/shared';
import { ElMessage } from 'element-plus';

export const useAuthStore = defineStore('auth', () => {
  const isAuthenticated = ref(tokenStore.isAuthenticated());
  const loading = ref(false);

  const httpClient = createHttpClient((import.meta.env?.VITE_GATEWAY_BASE_URL as string) || 'http://127.0.0.1:9000');
  const authApi = createAuthApi(httpClient);

  httpClient.setTokenManager(
    () => tokenStore.getAccessToken(),
    (token: string) => tokenStore.setAccessToken(token),
    () => {
      tokenStore.clearTokens();
      isAuthenticated.value = false;
    },
    async () => {
      const refreshToken = tokenStore.getRefreshToken();
      if (!refreshToken) {
        throw new Error('No refresh token available');
      }
      const response = await authApi.refresh(refreshToken);
      return response.access_token;
    }
  );

  async function login(username: string, password: string): Promise<boolean> {
    try {
      loading.value = true;
      const response: TokenResponse = await authApi.login({ username, password });
      
      tokenStore.setTokens(response.access_token, response.refresh_token);
      isAuthenticated.value = true;
      
      ElMessage.success('登录成功');
      return true;
    } catch (error: any) {
      ElMessage.error(error.message || '登录失败');
      return false;
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout API failed:', error);
    } finally {
      tokenStore.clearTokens();
      isAuthenticated.value = false;
      window.location.href = '/login';
    }
  }

  function checkAuth(): boolean {
    const authenticated = tokenStore.isAuthenticated();
    isAuthenticated.value = authenticated;
    return authenticated;
  }

  return {
    isAuthenticated,
    loading,
    httpClient,
    authApi,
    login,
    logout,
    checkAuth,
  };
});
