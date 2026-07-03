<template>
  <div class="page-container">
    <!-- 查询条件 -->
    <el-card class="search-card">
      <el-form :model="query" inline @submit.prevent="handleSearch">
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="请输入用户名"
            clearable
            @keyup.enter="handleSearch"
          />
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

    <!-- 列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button v-permission="'admin:user:create'" type="primary" @click="openCreate">
            新增用户
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" :border="false">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="mobile" label="手机号" min-width="120" />
        <el-table-column label="角色" min-width="160">
          <template #default="{ row }">
            <template v-if="row.roleCodes && row.roleCodes.length">
              <el-tag v-for="code in row.roleCodes" :key="code" size="small" class="role-tag">
                {{ code }}
              </el-tag>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === STATUS.ENABLED ? 'success' : 'info'">
              {{ STATUS_TEXT[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最后登录" min-width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'admin:user:update'"
              link
              type="primary"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'admin:user:status'"
              link
              :type="row.status === STATUS.ENABLED ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === STATUS.ENABLED ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-permission="'admin:user:resetPwd'"
              link
              type="danger"
              @click="openResetPwd(row)"
            >
              重置密码
            </el-button>
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

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :value="role.id" :label="role.name" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
        <el-form-item label="用户">
          <span>{{ pwdForm.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import {
  createUserApi,
  createRoleApi,
  STATUS,
  STATUS_TEXT,
  type UserDetail,
  type UserQuery,
  type UserCreateForm,
  type UserUpdateForm,
  type Role,
} from '@daydayup/shared';
import { httpClient } from '@/api/client';
import { usePageTable } from '@/composables/usePageTable';

const userApi = createUserApi(httpClient);
const roleApi = createRoleApi(httpClient);

const { query, list, total, loading, fetchData, handleSearch, handleReset, handlePageChange, handleSizeChange } =
  usePageTable<UserDetail, UserQuery>(
    (q) => userApi.page({ ...q, username: q.username || undefined }),
    () => ({ pageNum: 1, pageSize: 10, username: '', status: undefined })
  );

// 角色下拉选项
const roleOptions = ref<Role[]>([]);
async function loadRoles() {
  try {
    const res = await roleApi.page({ pageNum: 1, pageSize: 100 });
    roleOptions.value = res.records;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '角色列表加载失败');
  }
}

onMounted(() => {
  fetchData();
  loadRoles();
});

// 新增/编辑弹窗
const dialogVisible = ref(false);
const dialogTitle = ref('');
const isEdit = ref(false);
const editingId = ref<number | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  mobile: '',
  roleIds: [] as number[],
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 位', trigger: 'blur' },
  ],
};

function resetForm() {
  form.username = '';
  form.password = '';
  form.nickname = '';
  form.email = '';
  form.mobile = '';
  form.roleIds = [];
}

function openCreate() {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增用户';
  resetForm();
  dialogVisible.value = true;
}

function openEdit(row: UserDetail) {
  isEdit.value = true;
  editingId.value = row.id;
  dialogTitle.value = '编辑用户';
  resetForm();
  form.username = row.username;
  form.nickname = row.nickname ?? '';
  form.email = row.email ?? '';
  form.mobile = row.mobile ?? '';
  form.roleIds = row.roleIds ?? [];
  dialogVisible.value = true;
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      if (isEdit.value && editingId.value != null) {
        const payload: UserUpdateForm = {
          nickname: form.nickname || undefined,
          email: form.email || undefined,
          mobile: form.mobile || undefined,
          roleIds: form.roleIds,
        };
        await userApi.update(editingId.value, payload);
        ElMessage.success('更新成功');
      } else {
        const payload: UserCreateForm = {
          username: form.username,
          password: form.password,
          nickname: form.nickname || undefined,
          email: form.email || undefined,
          mobile: form.mobile || undefined,
          roleIds: form.roleIds,
        };
        await userApi.create(payload);
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

async function toggleStatus(row: UserDetail) {
  const next = row.status === STATUS.ENABLED ? STATUS.DISABLED : STATUS.ENABLED;
  const actionText = next === STATUS.ENABLED ? '启用' : '停用';
  try {
    await ElMessageBox.confirm(`确认${actionText}用户「${row.username}」？`, '提示', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await userApi.changeStatus(row.id, next);
    ElMessage.success(`${actionText}成功`);
    fetchData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败');
  }
}

// 重置密码弹窗
const pwdDialogVisible = ref(false);
const pwdFormRef = ref<FormInstance>();
const pwdForm = reactive({ id: 0, username: '', newPassword: '' });
const pwdRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为 6-64 位', trigger: 'blur' },
  ],
};

function openResetPwd(row: UserDetail) {
  pwdForm.id = row.id;
  pwdForm.username = row.username;
  pwdForm.newPassword = '';
  pwdDialogVisible.value = true;
}

async function submitResetPwd() {
  if (!pwdFormRef.value) return;
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      await userApi.resetPassword(pwdForm.id, pwdForm.newPassword);
      ElMessage.success('密码重置成功');
      pwdDialogVisible.value = false;
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      submitting.value = false;
    }
  });
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

.role-tag {
  margin-right: 4px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
