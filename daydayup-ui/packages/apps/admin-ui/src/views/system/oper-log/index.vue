<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="操作人" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模块">
          <el-input v-model="query.module" placeholder="模块" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部" clearable style="width: 110px">
            <el-option :value="1" label="成功" />
            <el-option :value="0" label="失败" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="onDateRangeChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header"><span>操作日志</span></div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" min-width="110" />
        <el-table-column prop="module" label="模块" min-width="110" />
        <el-table-column prop="operation" label="操作" min-width="120" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="requestUrl" label="请求 URL" min-width="180" show-overflow-tooltip />
        <el-table-column prop="requestIp" label="IP" min-width="120" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'">{{ row.success === 1 ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="costMs" label="耗时(ms)" width="100" />
        <el-table-column prop="operTime" label="操作时间" min-width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="操作人">{{ current.username }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ current.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ current.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="请求 URL">{{ current.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ current.requestIp }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="json">{{ current.requestParams }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="响应">
          <pre class="json">{{ current.responseBody }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="结果">{{ current.success === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ current.errorMsg }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costMs }} ms</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current.operTime }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { createOperLogApi, type OperLog, type OperLogQuery } from '@daydayup/shared';
import { httpClient } from '@/api/client';
import { usePageTable } from '@/composables/usePageTable';

const operLogApi = createOperLogApi(httpClient);

const { query, list, total, loading, fetchData, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  usePageTable<OperLog, OperLogQuery>(
    (q) =>
      operLogApi.page({
        ...q,
        username: q.username || undefined,
        module: q.module || undefined,
        startTime: q.startTime || undefined,
        endTime: q.endTime || undefined,
      }),
    () => ({ pageNum: 1, pageSize: 10, username: '', module: '', success: undefined, startTime: undefined, endTime: undefined })
  );

const dateRange = ref<[string, string] | null>(null);
function onDateRangeChange(val: [string, string] | null) {
  query.startTime = val?.[0];
  query.endTime = val?.[1];
}
function onReset() {
  dateRange.value = null;
  handleReset();
}

onMounted(fetchData);

const detailVisible = ref(false);
const current = ref<OperLog | null>(null);
function openDetail(row: OperLog) {
  current.value = row;
  detailVisible.value = true;
}
</script>

<style scoped>
.page-container {
  width: 100%;
}
.search-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
}
</style>
