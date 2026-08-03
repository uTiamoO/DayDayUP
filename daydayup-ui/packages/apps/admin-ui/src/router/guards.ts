import { Router } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useUserStore } from '@/stores/user';
import { i18n } from '@/i18n';
import { ElMessage } from 'element-plus';

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const authStore = useAuthStore();
    const userStore = useUserStore();

    const routeTitleKey = to.meta.titleKey as string | undefined;
    const routeTitle = routeTitleKey ? i18n.global.t(routeTitleKey) : ((to.meta.title as string) || '');
    const appTitle = i18n.global.t('app.name');
    document.title = routeTitle ? `${routeTitle} - ${appTitle}` : appTitle;

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
        ElMessage.error(i18n.global.t('messages.fetchUserFailed'));
        authStore.logout();
        next({ path: '/login', query: { redirect: to.fullPath } });
        return;
      }
    }

    const permission = to.meta.permission as string;
    if (permission && !userStore.hasPermission(permission)) {
      ElMessage.warning(i18n.global.t('messages.noPermission'));
      next('/403');
      return;
    }

    next();
  });

  router.afterEach(() => {
    // 可以在这里添加页面加载完成后的逻辑
  });
}
