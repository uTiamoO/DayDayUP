<template>
  <div class="admin-layout">
    <el-container>
      <el-aside :width="sidebarWidth" class="sidebar-container glass-panel">
        <Sidebar />
      </el-aside>
      <el-container>
        <el-header height="60px" class="navbar-container glass-panel">
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
  return appStore.sidebarCollapsed ? '64px' : '240px';
});
</script>

<style scoped>
.admin-layout {
  width: 100%;
  height: 100vh;
  background-color: var(--apple-bg);
}

.el-container {
  height: 100%;
}

.glass-panel {
  background: var(--apple-card-bg);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
  border: none;
}

.sidebar-container {
  transition: width 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  overflow-x: hidden;
  border-right: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 1px 0 15px rgba(0, 0, 0, 0.02);
  z-index: 10;
}

.navbar-container {
  padding: 0 32px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 1px 15px rgba(0, 0, 0, 0.02);
  z-index: 9;
}

.main-container {
  padding: 40px;
  overflow-y: auto;
  background-color: #f2f4f7;
  /* 苹果风浅色双重流光，似有似无的光晕，消除大片纯白的单调太空感 */
  background-image: 
    radial-gradient(circle at 10% 20%, rgba(0, 122, 255, 0.035) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(162, 89, 255, 0.025) 0%, transparent 40%);
}

</style>
