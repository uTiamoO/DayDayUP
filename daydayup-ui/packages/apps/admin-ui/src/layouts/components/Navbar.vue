<template>
  <div class="navbar">
    <div class="left-section">
      <el-icon class="hamburger" @click="toggleSidebar">
        <Fold v-if="!appStore.sidebarCollapsed" />
        <Expand v-else />
      </el-icon>
    </div>
    
    <div class="right-section">
      <el-dropdown @command="handleCommand">
        <div class="user-avatar">
          <el-avatar :size="36" :icon="UserFilled" />
          <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
          <el-icon class="arrow-down"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup lang="ts">
import { UserFilled } from '@element-plus/icons-vue';
import { useAppStore } from '@/stores/app';
import { useUserStore } from '@/stores/user';
import { useAuthStore } from '@/stores/auth';

const appStore = useAppStore();
const userStore = useUserStore();
const authStore = useAuthStore();

function toggleSidebar() {
  appStore.toggleSidebar();
}

function handleCommand(command: string) {
  if (command === 'logout') {
    authStore.logout();
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
  font-size: 24px;
  cursor: pointer;
  transition: transform 0.3s;
}

.hamburger:hover {
  transform: rotate(90deg);
}

.right-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-avatar:hover {
  background-color: #f5f5f5;
}

.username {
  font-size: 14px;
  color: #333;
}

.arrow-down {
  font-size: 12px;
  color: #999;
}
</style>
