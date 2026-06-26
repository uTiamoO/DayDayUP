import { createRouter, createWebHistory } from 'vue-router';
import { staticRoutes, adminRoutes } from './routes';
import { setupRouterGuards } from './guards';

const router = createRouter({
  history: createWebHistory((import.meta.env?.BASE_URL as string) || '/'),
  routes: [...staticRoutes, ...adminRoutes],
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      return { top: 0 };
    }
  },
});

setupRouterGuards(router);

export default router;
