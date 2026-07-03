<template>
  <div class="page-container">
    <el-card shadow="never" class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="所属字典">
          <el-select v-model="query.dictCode" placeholder="全部" clearable filterable style="width: 200px">
            <el-option v-for="d in dictOptions" :key="d.code" :value="d.code" :label="`${d.name}（${d.code}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="query.label" placeholder="字典项标签" clearable @keyup.enter="handleSearch" />
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
          <span>字典项管理</span>
          <el-button v-permission="'admin:dict-item:create'" type="primary" @click="openCreate">新增字典项</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="dictCode" label="所属字典" min-width="140" />
        <el-table-column prop="label" label="标签" min-width="140" />
        <el-table-column prop="value" label="值" min-width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === STATUS.ENABLED ? 'success' : 'info'">{{ STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'admin:dict-item:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-permission="'admin:dict-item:status'"
              link
              :type="row.status === STATUS.ENABLED ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === STATUS.ENABLED ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'admin:dict-item:delete'" link type="danger" @click="remove(row)">删除</el-button>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属字典" prop="dictCode">
          <el-select v-model="form.dictCode" placeholder="请选择字典" filterable style="width: 100%">
            <el-option v-for="d in dictOptions" :key="d.code" :value="d.code" :label="`${d.name}（${d.code}）`" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="label">
          <el-input v-model="form.label" placeholder="请输入标签（显示文本）" />
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="form.value" placeholder="请输入值" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
import {
  createDictApi,
  createDictItemApi,
  STATUS,
  STATUS_TEXT,
  type Dict,
  type DictItem,
  type DictItemQuery,
  type DictItemForm,
} from '@daydayup/shared';
import { httpClient } from '@/api/client';
import { usePageTable } from '@/composables/usePageTable';

const dictItemApi = createDictItemApi(httpClient);
const dictApi = createDictApi(httpClient);

const { query, list, total, loading, fetchData, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  usePageTable<DictItem, DictItemQuery>(
    (q) => dictItemApi.page({ ...q, dictCode: q.dictCode || undefined, label: q.label || undefined }),
    () => ({ pageNum: 1, pageSize: 10, dictCode: '', label: '', status: undefined })
  );

const dictOptions = ref<Dict[]>([]);
async function loadDicts() {
  try {
    const res = await dictApi.page({ pageNum: 1, pageSize: 100 });
    dictOptions.value = res.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '字典列表加载失败');
  }
}

onMounted(() => {
  fetchData();
  loadDicts();
});

const dialogVisible = ref(false);
const dialogTitle = ref('');
const isEdit = ref(false);
const editingId = ref<number | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({ dictCode: '', value: '', label: '', sort: 0, remark: '' });

const rules: FormRules = {
  dictCode: [{ required: true, message: '请选择所属字典', trigger: 'change' }],
  label: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  value: [{ required: true, message: '请输入值', trigger: 'blur' }],
};

function resetForm() {
  form.dictCode = '';
  form.value = '';
  form.label = '';
  form.sort = 0;
  form.remark = '';
}

function openCreate() {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增字典项';
  resetForm();
  if (query.dictCode) {
    form.dictCode = query.dictCode;
  }
  dialogVisible.value = true;
}

function openEdit(row: DictItem) {
  isEdit.value = true;
  editingId.value = row.id;
  dialogTitle.value = '编辑字典项';
  resetForm();
  form.dictCode = row.dictCode;
  form.value = row.value;
  form.label = row.label;
  form.sort = row.sort ?? 0;
  form.remark = row.remark ?? '';
  dialogVisible.value = true;
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload: DictItemForm = {
        dictCode: form.dictCode,
        value: form.value,
        label: form.label,
        sort: form.sort,
        remark: form.remark || undefined,
      };
      if (isEdit.value && editingId.value != null) {
        await dictItemApi.update(editingId.value, payload);
        ElMessage.success('更新成功');
      } else {
        await dictItemApi.create(payload);
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

async function toggleStatus(row: DictItem) {
  const next = row.status === STATUS.ENABLED ? STATUS.DISABLED : STATUS.ENABLED;
  const actionText = next === STATUS.ENABLED ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确认${actionText}字典项「${row.label}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await dictItemApi.changeStatus(row.id, next);
    ElMessage.success(`${actionText}成功`);
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

async function remove(row: DictItem) {
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.label}」？`, '警告', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await dictItemApi.remove(row.id);
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
