import { defineStore } from 'pinia';
import { ref } from 'vue';
import { tokenStore, type TokenResponse } from '@daydayup/shared';
import { ElMessage } from 'element-plus';
import { authApi } from '@/api/client';

export const useAuthStore = defineStore('auth', () => {
  const isAuthenticated = ref(tokenStore.isAuthenticated());
  const loading = ref(false);

  async function login(username: string, password: string): Promise<boolean> {
    try {
      loading.value = true;
      const response: TokenResponse = await authApi.login({ username, password });

      tokenStore.setTokens(response.accessToken, response.refreshToken);
      isAuthenticated.value = true;

      ElMessage.success('登录成功');
      return true;
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '登录失败');
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
    login,
    logout,
    checkAuth,
  };
});
