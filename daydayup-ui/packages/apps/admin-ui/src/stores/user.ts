import { defineStore } from 'pinia';
import { ref } from 'vue';
import { UserContext, can } from '@daydayup/shared';
import { authApi } from '@/api/client';

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<UserContext | null>(null);
  const authorities = ref<string[]>([]);

  async function fetchUserInfo() {
    try {
      const data = await authApi.getCurrentUser();
      currentUser.value = data;
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
    currentUser.value = null;
    authorities.value = [];
  }

  return {
    currentUser,
    authorities,
    fetchUserInfo,
    hasPermission,
    clearUserInfo,
  };
});
