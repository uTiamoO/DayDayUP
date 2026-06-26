import { RouteRecordRaw } from 'vue-router';

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404', public: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '403', public: true },
  },
  {
    path: '/',
    redirect: '/admin/dashboard',
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
        meta: { title: '首页', icon: 'House' },
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'admin:user:list' },
      },
      {
        path: 'menus',
        name: 'MenuManagement',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu', permission: 'admin:menu:list' },
      },
      {
        path: 'dicts',
        name: 'DictManagement',
        component: () => import('@/views/system/dict/index.vue'),
        meta: { title: '字典管理', icon: 'Document', permission: 'admin:dict:list' },
      },
      {
        path: 'dict-items',
        name: 'DictItemManagement',
        component: () => import('@/views/system/dict-item/index.vue'),
        meta: { title: '字典项管理', icon: 'Collection', permission: 'admin:dict-item:list' },
      },
      {
        path: 'oper-logs',
        name: 'OperLogManagement',
        component: () => import('@/views/system/oper-log/index.vue'),
        meta: { title: '操作日志', icon: 'Document', permission: 'admin:oper-log:list' },
      },
      {
        path: 'api-keys',
        name: 'ApiKeyManagement',
        component: () => import('@/views/system/api-key/index.vue'),
        meta: { title: 'API密钥管理', icon: 'Key', permission: 'admin:apikey:list' },
      },
    ],
  },
];
