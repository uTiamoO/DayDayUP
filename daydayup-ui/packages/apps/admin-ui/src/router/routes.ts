import { RouteRecordRaw } from 'vue-router';

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', titleKey: 'routes.login', public: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', titleKey: 'routes.notFound', public: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '403', titleKey: 'routes.forbidden', public: true },
  },
  {
    path: '/',
    redirect: '/admin/dashboard',
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
];

export const adminRoutes: RouteRecordRaw[] = [
  {
    path: '/admin',
    name: 'Admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', titleKey: 'routes.dashboard', icon: 'House' },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', titleKey: 'routes.users', icon: 'User', permission: 'admin:user:list' },
      },
      {
        path: 'menus',
        name: 'MenuManagement',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', titleKey: 'routes.menus', icon: 'Menu', permission: 'admin:menu:list' },
      },
      {
        path: 'dicts',
        name: 'DictManagement',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', titleKey: 'routes.dicts', icon: 'Document', permission: 'admin:dict:list' },
      },
      {
        path: 'dict-items',
        name: 'DictItemManagement',
        component: () => import('@/views/system/dict-item/index.vue'),
        meta: { title: '字典项管理', titleKey: 'routes.dictItems', icon: 'Collection', permission: 'admin:dict-item:list' },
      },
      {
        path: 'oper-logs',
        name: 'OperLogManagement',
        component: () => import('@/views/system/oper-log/index.vue'),
        meta: { title: '操作日志', titleKey: 'routes.operLogs', icon: 'Document', permission: 'admin:oper-log:list' },
      },
      {
        path: 'api-keys',
        name: 'ApiKeyManagement',
        component: () => import('@/views/system/api-key/index.vue'),
        meta: { title: 'API密钥管理', titleKey: 'routes.apiKeys', icon: 'Key', permission: 'admin:apikey:list' },
      },
      {
        path: 'roles',
        name: 'RoleManagement',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', titleKey: 'routes.roles', icon: 'UserFilled', permission: 'admin:role:list' },
      },
      {
        path: 'permissions',
        name: 'PermissionManagement',
        component: () => import('@/views/system/permission/index.vue'),
        meta: { title: '权限管理', titleKey: 'routes.permissions', icon: 'Lock', permission: 'admin:permission:list' },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人空间', titleKey: 'routes.profile', icon: 'User' },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/index.vue'),
        meta: { title: '系统设置', titleKey: 'routes.settings', icon: 'Setting' },
      },
    ],
  },
];
