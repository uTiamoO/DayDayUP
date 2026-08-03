<template>
  <div class="dashboard">
    <div class="welcome-section">
      <h2 class="greeting">Welcome back, {{ userStore.currentUser?.username || 'Admin' }}</h2>
      <p class="subtitle">Here's a quick overview of your workspace today.</p>
    </div>

    <div v-loading="loading" class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper user-bg">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">Total Users</div>
          <div class="stat-value">{{ formatCount(stats.userCount) }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper role-bg">
          <el-icon :size="24"><UserFilled /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">Roles</div>
          <div class="stat-value">{{ formatCount(stats.roleCount) }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper menu-bg">
          <el-icon :size="24"><Menu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">Menus</div>
          <div class="stat-value">{{ formatCount(stats.menuCount) }}</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper dict-bg">
          <el-icon :size="24"><Document /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-label">Dictionaries</div>
          <div class="stat-value">{{ formatCount(stats.dictCount) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { User, UserFilled, Menu, Document } from '@element-plus/icons-vue';
import { createDashboardApi, type DashboardStats } from '@daydayup/shared';
import { httpClient } from '@/api/client';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const dashboardApi = createDashboardApi(httpClient);

const loading = ref(false);
const stats = reactive<DashboardStats>({
  userCount: 0,
  roleCount: 0,
  menuCount: 0,
  dictCount: 0,
});

function formatCount(value: number): string {
  return (value ?? 0).toLocaleString();
}

async function loadStats() {
  loading.value = true;
  try {
    const result = await dashboardApi.stats();
    stats.userCount = result.userCount ?? 0;
    stats.roleCount = result.roleCount ?? 0;
    stats.menuCount = result.menuCount ?? 0;
    stats.dictCount = result.dictCount ?? 0;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载统计数据失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadStats);
</script>

<style scoped>
.dashboard {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-section {
  margin-bottom: 48px;
}

.greeting {
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.5px;
  color: var(--apple-text-primary);
  margin: 0 0 8px 0;
}

.subtitle {
  font-size: 16px;
  color: var(--apple-text-secondary);
  margin: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 24px;
}

.stat-card {
  background: var(--apple-card-bg);
  border-radius: var(--el-border-radius-round);
  padding: 32px 24px;
  display: flex;
  align-items: center;
  gap: 20px;
  box-shadow: var(--el-box-shadow-light);
  border: 1px solid var(--apple-card-highlight-border);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--el-box-shadow);
}

.stat-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.user-bg { background: linear-gradient(135deg, #007AFF 0%, #005BBF 100%); }
.role-bg { background: linear-gradient(135deg, #AF52DE 0%, #8E30C4 100%); }
.menu-bg { background: linear-gradient(135deg, #34C759 0%, #248A3D 100%); }
.dict-bg { background: linear-gradient(135deg, #FF9500 0%, #CC7700 100%); }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--apple-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: var(--apple-text-primary);
}
</style>
