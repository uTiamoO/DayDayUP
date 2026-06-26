/**
 * 菜单项
 */
export interface MenuItem {
  id: number;
  parentId: number;
  code: string;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  type?: string;
  permissionCode?: string;
  sort: number;
  visible: number;
  status: number;
  createTime?: string;
  updateTime?: string;
  children?: MenuItem[];
}

/**
 * 菜单查询参数
 */
export interface MenuQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  status?: number;
}

/**
 * 菜单创建/更新请求
 */
export interface MenuForm {
  parentId: number;
  code: string;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  type?: string;
  permissionCode?: string;
  sort: number;
  visible: number;
  status: number;
}
