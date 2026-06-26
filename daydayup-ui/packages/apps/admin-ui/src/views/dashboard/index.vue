<template>
  <div class="dashboard">
    <el-card class="welcome-card">
      <template #header>
        <div class="card-header">
          <span>欢迎使用 DayDayUP 管理后台</span>
        </div>
      </template>
      <div class="welcome-content">
        <el-icon class="welcome-icon" :size="80"><House /></el-icon>
        <h2>欢迎，{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}！</h2>
        <p>您当前的权限：{{ authorities }}</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon" :size="40" color="#409EFF"><User /></el-icon>
            <div class="stat-info">
              <div class="stat-value">--</div>
              <div class="stat-label">用户总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon" :size="40" color="#67C23A"><Menu /></el-icon>
            <div class="stat-info">
              <div class="stat-value">--</div>
              <div class="stat-label">菜单总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon" :size="40" color="#E6A23C"><Document /></el-icon>
            <div class="stat-info">
              <div class="stat-value">--</div>
              <div class="stat-label">字典总数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-item">
            <el-icon class="stat-icon" :size="40" color="#F56C6C"><Key /></el-icon>
            <div class="stat-info">
              <div class="stat-value">--</div>
              <div class="stat-label">API密钥</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();

const authorities = computed(() => {
  return userStore.authorities.join(', ') || '无';
});
</script>

<style scoped>
.dashboard {
  width: 100%;
}

.welcome-card {
  margin-bottom: 20px;
}

.card-header {
  font-size: 16px;
  font-weight: bold;
}

.welcome-content {
  text-align: center;
  padding: 40px 0;
}

.welcome-icon {
  margin-bottom: 20px;
  color: #409EFF;
}

.welcome-content h2 {
  margin: 0 0 10px 0;
  font-size: 24px;
  color: #333;
}

.welcome-content p {
  margin: 0;
  font-size: 14px;
  color: #999;
}

.stats-row {
  margin-top: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #999;
}
</style>
