<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="字典名称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option :value="STATUS.ENABLED" :label="STATUS_TEXT[STATUS.ENABLED]" />
            <el-option :value="STATUS.DISABLED" :label="STATUS_TEXT[STATUS.DISABLED]" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>字典管理</span>
          <el-button v-permission="'admin:dict:create'" type="primary" @click="openCreate">新增字典</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="code" label="字典编码" min-width="140" />
        <el-table-column prop="name" label="字典名称" min-width="140" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === STATUS.ENABLED ? 'success' : 'info'">{{ STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'admin:dict:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-permission="'admin:dict:status'"
              link
              :type="row.status === STATUS.ENABLED ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === STATUS.ENABLED ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'admin:dict:delete'" link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入字典编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入字典名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { createDictApi, STATUS, STATUS_TEXT, type Dict, type DictQuery, type DictForm } from '@daydayup/shared';
import { httpClient } from '@/api/client';
import { usePageTable } from '@/composables/usePageTable';

const dictApi = createDictApi(httpClient);

const { query, list, total, loading, fetchData, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  usePageTable<Dict, DictQuery>(
    (q) => dictApi.page({ ...q, name: q.name || undefined }),
    () => ({ pageNum: 1, pageSize: 10, name: '', status: undefined })
  );

onMounted(fetchData);

const dialogVisible = ref(false);
const dialogTitle = ref('');
const isEdit = ref(false);
const editingId = ref<number | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ code: '', name: '', remark: '' });

const rules: FormRules = {
  code: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
};

function resetForm() {
  form.code = '';
  form.name = '';
  form.remark = '';
}

function openCreate() {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增字典';
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: Dict) {
  isEdit.value = true;
  editingId.value = row.id;
  dialogTitle.value = '编辑字典';
  resetForm();
  form.code = row.code;
  form.name = row.name;
  form.remark = row.remark ?? '';
  dialogVisible.value = true;
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload: DictForm = { code: form.code, name: form.name, remark: form.remark || undefined };
      if (isEdit.value && editingId.value != null) {
        await dictApi.update(editingId.value, payload);
        ElMessage.success('更新成功');
      } else {
        await dictApi.create(payload);
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      fetchData();
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      submitting.value = false;
    }
  });
}

async function toggleStatus(row: Dict) {
  const next = row.status === STATUS.ENABLED ? STATUS.DISABLED : STATUS.ENABLED;
  const actionText = next === STATUS.ENABLED ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确认${actionText}字典「${row.name}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await dictApi.changeStatus(row.id, next);
    ElMessage.success(`${actionText}成功`);
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

async function remove(row: Dict) {
  try {
    await ElMessageBox.confirm(`确认删除字典「${row.name}」？此操作不可恢复。`, '警告', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await dictApi.remove(row.id);
    ElMessage.success('删除成功');
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
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
</style>
