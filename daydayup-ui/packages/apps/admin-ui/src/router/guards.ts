import { Router } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore();
    const userStore = useUserStore();

    document.title = (to.meta.title as string) || 'DayDayUP 管理后台';

    if (to.meta.public) {
      next();
      return;
    }

    if (!authStore.isAuthenticated) {
      next({ path: '/login', query: { redirect: to.fullPath } });
      return;
    }

    if (!userStore.currentUser) {
      try {
        await userStore.fetchUserInfo();
      } catch (error) {
        ElMessage.error('获取用户信息失败');
        authStore.logout();
        next({ path: '/login', query: { redirect: to.fullPath } });
        return;
      }
    }

    const permission = to.meta.permission as string;
    if (permission && !userStore.hasPermission(permission)) {
      ElMessage.warning('您没有权限访问该页面');
      next('/403');
      return;
    }

    next();
  });

  router.afterEach(() => {
    // 可以在这里添加页面加载完成后的逻辑
  });
}
