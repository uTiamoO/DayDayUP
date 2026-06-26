import { defineStore } from 'pinia';
import { ref } from 'vue';
import { UserDetail, can } from '@daydayup/shared';
import { useAuthStore } from './auth';

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserDetail | null>(null);
  const authorities = ref<string[]>([]);

  async function fetchUserInfo() {
    const authStore = useAuthStore();
    try {
      const data = await authStore.authApi.getCurrentUser();
      userInfo.value = data;
      authorities.value = data.authorities || [];
    } catch (error) {
      console.error('Failed to fetch user info:', error);
      throw error;
    }
  }

  function hasPermission(permission: string): boolean {
    return can(authorities.value, permission);
  }

  function clearUserInfo() {
    userInfo.value = null;
    authorities.value = [];
  }

  return {
    userInfo,
    authorities,
    fetchUserInfo,
    hasPermission,
    clearUserInfo,
  };
});
