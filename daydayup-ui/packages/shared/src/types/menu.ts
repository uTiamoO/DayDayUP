/**
 * 菜单（管理列表项 / 树节点，对齐后端 MenuVO 与 MenuTreeVO）
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
  sort?: number;
  visible?: number;
  status: number;
  createTime?: string;
  updateTime?: string;
  children?: MenuItem[];
}

/**
 * 菜单分页查询参数（对齐 MenuPageQueryDTO）
 */
export interface MenuQuery {
  pageNum: number;
  pageSize: number;
  name?: string;
  type?: string;
  status?: number;
}

/**
 * 新建/更新菜单（对齐 MenuCreateDTO / MenuUpdateDTO）
 */
export interface MenuForm {
  parentId?: number;
  code: string;
  name: string;
  path?: string;
  component?: string;
  icon?: string;
  type?: string;
  permissionCode?: string;
  sort?: number;
  visible?: number;
}
