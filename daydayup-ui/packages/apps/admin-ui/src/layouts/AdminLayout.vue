<template>
  <div class="admin-layout">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar-container">
        <Sidebar />
      </el-aside>
      <el-container>
        <el-header height="60px" class="navbar-container">
          <Navbar />
        </el-header>
        <el-main class="main-container">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useAppStore } from '@/stores/app';
import Sidebar from './components/Sidebar.vue';
import Navbar from './components/Navbar.vue';

const appStore = useAppStore();

const sidebarWidth = computed(() => {
  return appStore.sidebarCollapsed ? '64px' : '200px';
});
</script>

<style scoped>
.admin-layout {
  width: 100%;
  height: 100vh;
}

.el-container {
  height: 100%;
}

.sidebar-container {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}

.navbar-container {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  display: flex;
  align-items: center;
}

.main-container {
  background-color: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
