/**
 * 通用状态码
 */
export const STATUS = {
  ENABLED: 1,
  DISABLED: 0,
} as const;

/**
 * 状态文本映射
 */
export const STATUS_TEXT: Record<number, string> = {
  [STATUS.ENABLED]: '启用',
  [STATUS.DISABLED]: '禁用',
};

/**
 * 菜单类型
 */
export const MENU_TYPE = {
  DIRECTORY: 'M',
  MENU: 'C',
  BUTTON: 'F',
} as const;

/**
 * 菜单类型文本映射
 */
export const MENU_TYPE_TEXT: Record<string, string> = {
  [MENU_TYPE.DIRECTORY]: '目录',
  [MENU_TYPE.MENU]: '菜单',
  [MENU_TYPE.BUTTON]: '按钮',
};

/**
 * 业务状态码（对齐后端 ErrorCode）
 */
export const BIZ_CODE = {
  SUCCESS: 200,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  TOKEN_INVALID: 20001,
  TOKEN_EXPIRED: 20002,
  TOKEN_BLACKLISTED: 20006,
} as const;
