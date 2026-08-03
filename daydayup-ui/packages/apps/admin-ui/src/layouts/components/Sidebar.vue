<template>
  <div class="sidebar">
    <!-- 系统 Logo 区域 -->
    <div class="logo-container">
      <h2 v-if="!appStore.sidebarCollapsed" class="logo-title">DayDayUP</h2>
      <h2 v-else class="logo-title-collapsed">D</h2>
    </div>
    <!-- 侧边栏导航菜单：去除了写死的深色背景，使其透出外层毛玻璃背景 -->
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :unique-opened="true"
      router
    >
      <el-menu-item index="/admin/dashboard">
        <el-icon><House /></el-icon>
        <template #title>首页</template>
      </el-menu-item>
      
      <el-sub-menu index="system">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </template>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/roles">
          <el-icon><UserFilled /></el-icon>
          <template #title>角色管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/permissions">
          <el-icon><Lock /></el-icon>
          <template #title>权限管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/menus">
          <el-icon><Menu /></el-icon>
          <template #title>菜单管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/dicts">
          <el-icon><Document /></el-icon>
          <template #title>字典管理</template>
        </el-menu-item>
        <el-menu-item index="/admin/dict-items">
          <el-icon><Collection /></el-icon>
          <template #title>字典项管理</template>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="monitor">
        <template #title>
          <el-icon><Monitor /></el-icon>
          <span>系统监控</span>
        </template>
        <el-menu-item index="/admin/oper-logs">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
        <el-menu-item index="/admin/api-keys">
          <el-icon><Key /></el-icon>
          <template #title>API密钥</template>
        </el-menu-item>
      </el-sub-menu>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';

// 初始化路由与全局应用状态
const route = useRoute();
const appStore = useAppStore();

// 计算当前激活的菜单项路径
const activeMenu = computed(() => route.path);
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 顶部 Logo 栏样式：透明背景，加细底边框 */
.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  color: var(--apple-text-primary);
  border-bottom: 1px solid var(--apple-border);
}

.logo-title {
  font-size: 20px;
  font-weight: bold;
  margin: 0;
}

.logo-title-collapsed {
  font-size: 24px;
  font-weight: bold;
  margin: 0;
}

.el-menu {
  border-right: none;
  flex: 1;
}

/* ==========================================================================
   Apple Style 胶囊式导航样式定制
   ========================================================================== */

/* 保证菜单本身透明且无边框 */
:deep(.el-menu) {
  background-color: transparent !important;
  border-right: none;
}

/* 定制一级菜单项与折叠标题项，缩进并圆角化 */
:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  height: 40px !important;
  line-height: 40px !important;
  margin: 4px 12px !important;
  border-radius: 8px !important;
  color: var(--apple-text-secondary) !important;
  font-weight: 500;
  transition: all 0.25s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
}

/* 鼠标悬停时的背景变暗效果 */
:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: var(--apple-hover-overlay) !important;
  color: var(--apple-text-primary) !important;
}

/* 当前选中状态的高亮：使用磨砂感微透淡蓝色 */
:deep(.el-menu-item.is-active) {
  background-color: rgba(0, 122, 255, 0.1) !important;
  color: var(--apple-blue) !important;
  font-weight: 600;
}

/* 菜单项图标样式 */
:deep(.el-menu-item .el-icon),
:deep(.el-sub-menu__title .el-icon) {
  color: var(--apple-text-secondary) !important;
  transition: color 0.25s ease;
}

/* 选中项的图标颜色同步变蓝 */
:deep(.el-menu-item.is-active .el-icon) {
  color: var(--apple-blue) !important;
}

/* 悬停时的图标颜色同步变深 */
:deep(.el-menu-item:hover .el-icon),
:deep(.el-sub-menu__title:hover .el-icon) {
  color: var(--apple-text-primary) !important;
}

/* 子菜单展开后的内容面板背景设为透明 */
:deep(.el-menu--inline) {
  background-color: transparent !important;
}

/* 折叠状态下的侧边栏宽度及居中对齐适配 */
:deep(.el-menu--collapse) {
  width: 64px;
}

/* 显式隐藏折叠状态下的非图标文本及右侧子菜单指示箭头，防止其占据空间导致图标居中偏移 */
:deep(.el-menu--collapse span),
:deep(.el-menu--collapse .el-sub-menu__icon-arrow) {
  display: none !important;
}

:deep(.el-menu--collapse .el-menu-item),
:deep(.el-menu--collapse .el-sub-menu__title) {
  margin: 8px 10px !important;
  padding: 0 !important;
  display: flex !important;
  justify-content: center !important;
  align-items: center !important;
  border-radius: 8px !important;
}

/* 强制折叠后的图标无边距居中 */
:deep(.el-menu--collapse .el-icon) {
  margin: 0 !important;
  font-size: 18px !important;
}

</style>
