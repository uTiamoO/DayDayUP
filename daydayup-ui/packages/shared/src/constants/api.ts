/**
 * API 路径常量
 */
export const API_PATHS = {
  AUTH: {
    LOGIN: '/auth/login',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
  },
  ADMIN: {
    ME: '/admin/me',
    USERS: '/admin/users/manage',
    MENUS: '/admin/menus',
    DICTS: '/admin/dicts',
    DICT_ITEMS: '/admin/dict-items',
    OPER_LOGS: '/admin/oper-logs',
    API_KEYS: '/admin/api-keys',
  },
} as const;
