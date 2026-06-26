/**
 * 权限判断工具
 */

/**
 * 判断用户是否拥有指定权限
 * 支持通配符（如 admin:* 匹配 admin:user:add）
 */
export function can(authorities: string[], permission: string): boolean {
  if (authorities.includes(permission)) return true;
  const namespace = permission.split(':')[0];
  return authorities.includes(`${namespace}:*`);
}

/**
 * 判断用户是否拥有任一权限
 */
export function hasAnyPermission(authorities: string[], permissions: string[]): boolean {
  return permissions.some((p) => can(authorities, p));
}

/**
 * 判断用户是否拥有所有权限
 */
export function hasAllPermissions(authorities: string[], permissions: string[]): boolean {
  return permissions.every((p) => can(authorities, p));
}
