<template>
  <div class="navbar">
    <div class="left-section">
      <div class="hamburger" @click="toggleSidebar">
        <el-icon><Fold v-if="!appStore.sidebarCollapsed" /><Expand v-else /></el-icon>
      </div>
    </div>
    
    <div class="right-section">
      <el-dropdown trigger="click" @command="handleCommand" popper-class="apple-popper">
        <div class="user-avatar">
          <el-avatar :size="32" :src="'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
          <span class="username">{{ userStore.currentUser?.username || t('common.admin') }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="glass-dropdown">
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>{{ t('navbar.profile') }}
            </el-dropdown-item>
            <el-dropdown-item command="settings">
              <el-icon><Setting /></el-icon>{{ t('navbar.settings') }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout" class="text-danger">
              <el-icon><SwitchButton /></el-icon>{{ t('navbar.logout') }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Fold, Expand, User, Setting, SwitchButton } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useUserStore } from '@/stores/user';
import { useAuthStore } from '@/stores/auth';
import { ElMessage } from 'element-plus';

const router = useRouter();
const appStore = useAppStore();
const userStore = useUserStore();
const authStore = useAuthStore();
const { t } = useI18n();

function toggleSidebar() {
  appStore.toggleSidebar();
}

function handleCommand(command: string) {
  if (command === 'logout') {
    ElMessage.success({ message: t('navbar.logoutSuccess'), customClass: 'apple-toast' });
    authStore.logout();
  } else if (command === 'profile') {
    router.push('/admin/profile');
  } else if (command === 'settings') {
    router.push('/admin/settings');
  }
}
</script>

<style scoped>
.navbar {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.left-section {
  display: flex;
  align-items: center;
}

.hamburger {
  font-size: 20px;
  cursor: pointer;
  color: var(--apple-text-primary);
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.2s;
}

.hamburger:hover {
  background-color: var(--apple-hover-overlay);
}

.right-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.2s;
}

.user-avatar:hover {
  background-color: var(--apple-hover-overlay);
}

.username {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-primary);
}

.text-danger {
  color: #FF3B30 !important;
}
</style>
<style>
/* Global overrides for popper and toast */
.apple-popper.el-popper {
  background: var(--apple-card-bg) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid var(--apple-card-highlight-border) !important;
  border-radius: 12px !important;
  box-shadow: 0 10px 30px rgba(0,0,0,0.08) !important;
}

.apple-popper.el-popper .el-popper__arrow::before {
  background: var(--apple-card-bg) !important;
  border: 1px solid var(--apple-card-highlight-border) !important;
}

.apple-toast.el-message {
  background: var(--apple-card-bg) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  border: 1px solid var(--apple-card-highlight-border) !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 24px rgba(0,0,0,0.06) !important;
  color: var(--apple-text-primary) !important;
  padding: 15px 20px !important;
}
</style>
