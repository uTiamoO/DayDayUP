<template>
  <div class="sidebar">
    <div class="logo-container">
      <h2 v-if="!appStore.sidebarCollapsed" class="logo-title">DayDayUP</h2>
      <h2 v-else class="logo-title-collapsed">D</h2>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :unique-opened="true"
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409EFF"
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

const route = useRoute();
const appStore = useAppStore();

const activeMenu = computed(() => route.path);
</script>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4a;
  color: #fff;
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
</style>
