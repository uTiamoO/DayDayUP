<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header"><span>API 密钥管理</span></div>
      </template>

      <el-alert
        type="info"
        :closable="false"
        title="API Key 用于服务间调用 / 定时任务认证，关联当前登录用户的权限。明文仅在创建时显示一次，请妥善保存。后端不提供列表查询。"
        class="tip"
      />

      <div class="actions">
        <el-button
          v-permission="'admin:apikey:create'"
          type="primary"
          :loading="creating"
          @click="handleCreate"
        >
          生成 API Key
        </el-button>
      </div>

      <el-form
        v-permission="'admin:apikey:revoke'"
        :model="revokeForm"
        inline
        class="revoke-form"
        @submit.prevent="handleRevoke"
      >
        <el-form-item label="吊销 Key">
          <el-input
            v-model="revokeForm.apiKey"
            placeholder="输入要吊销的 API Key"
            style="width: 360px"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="danger" :loading="revoking" :disabled="!revokeForm.apiKey" @click="handleRevoke">
            吊销
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog v-model="resultVisible" title="API Key 创建成功" width="520px">
      <el-alert type="warning" :closable="false" title="请立即复制保存，关闭后无法再次查看。" class="result-tip" />
      <el-input :model-value="createdApiKey" readonly>
        <template #append>
          <el-button @click="copyKey">复制</el-button>
        </template>
      </el-input>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">我已保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createApiKeyApi } from '@daydayup/shared';
import { httpClient } from '@/api/client';

const apiKeyApi = createApiKeyApi(httpClient);

const creating = ref(false);
const resultVisible = ref(false);
const createdApiKey = ref('');

async function handleCreate() {
  creating.value = true;
  try {
    const res = await apiKeyApi.create();
    createdApiKey.value = res.apiKey ?? '';
    resultVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建失败');
  } finally {
    creating.value = false;
  }
}

async function copyKey() {
  try {
    await navigator.clipboard.writeText(createdApiKey.value);
    ElMessage.success('已复制到剪贴板');
  } catch {
    ElMessage.warning('复制失败，请手动复制');
  }
}

const revoking = ref(false);
const revokeForm = reactive({ apiKey: '' });

async function handleRevoke() {
  if (!revokeForm.apiKey) return;
  try {
    await ElMessageBox.confirm('确认吊销该 API Key？吊销后将立即失效。', '警告', { type: 'warning' });
  } catch {
    return;
  }
  revoking.value = true;
  try {
    await apiKeyApi.revoke(revokeForm.apiKey);
    ElMessage.success('吊销成功');
    revokeForm.apiKey = '';
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '吊销失败');
  } finally {
    revoking.value = false;
  }
}
</script>

<style scoped>
.page-container {
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.tip {
  margin-bottom: 20px;
}
.actions {
  margin-bottom: 24px;
}
.revoke-form {
  margin-top: 8px;
}
.result-tip {
  margin-bottom: 16px;
}
</style>
