<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <div>
            <el-button @click="loadTree">刷新</el-button>
            <el-button v-permission="'admin:menu:create'" type="primary" @click="openCreateRoot">
              新增顶级菜单
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="菜单名称" min-width="180" />
        <el-table-column prop="code" label="编码" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag>{{ MENU_TYPE_TEXT[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="140" show-overflow-tooltip />
        <el-table-column prop="component" label="组件" min-width="160" show-overflow-tooltip />
        <el-table-column prop="permissionCode" label="权限码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="70" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === STATUS.ENABLED ? 'success' : 'info'">{{ STATUS_TEXT[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'admin:menu:create'" link type="primary" @click="openCreateChild(row)">新增子级</el-button>
            <el-button v-permission="'admin:menu:update'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'admin:menu:delete'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'name', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="不选则为顶级菜单"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="MENU_TYPE.DIRECTORY">目录</el-radio>
            <el-radio :value="MENU_TYPE.MENU">菜单</el-radio>
            <el-radio :value="MENU_TYPE.BUTTON">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="菜单名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="菜单编码（唯一）" />
        </el-form-item>
        <el-form-item v-if="form.type !== MENU_TYPE.BUTTON" label="路由路径">
          <el-input v-model="form.path" placeholder="如 /admin/users" />
        </el-form-item>
        <el-form-item v-if="form.type !== MENU_TYPE.BUTTON" label="组件">
          <el-input v-model="form.component" placeholder="如 system/user/index" />
        </el-form-item>
        <el-form-item v-if="form.type !== MENU_TYPE.BUTTON" label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="权限码">
          <el-input v-model="form.permissionCode" placeholder="如 admin:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item v-if="form.type !== MENU_TYPE.BUTTON" label="可见">
          <el-switch v-model="form.visible" :active-value="1" :inactive-value="0" />
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
  createMenuApi,
  MENU_TYPE,
  MENU_TYPE_TEXT,
  STATUS,
  STATUS_TEXT,
  type MenuItem,
  type MenuForm,
} from '@daydayup/shared';
import { httpClient } from '@/api/client';

const menuApi = createMenuApi(httpClient);

const loading = ref(false);
const treeData = ref<MenuItem[]>([]);

async function loadTree() {
  loading.value = true;
  try {
    treeData.value = await menuApi.tree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '菜单加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadTree);

const dialogVisible = ref(false);
const dialogTitle = ref('');
const isEdit = ref(false);
const editingId = ref<number | null>(null);
const submitting = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<{
  parentId: number | undefined;
  type: string;
  name: string;
  code: string;
  path: string;
  component: string;
  icon: string;
  permissionCode: string;
  sort: number;
  visible: number;
}>({
  parentId: undefined,
  type: MENU_TYPE.MENU,
  name: '',
  code: '',
  path: '',
  component: '',
  icon: '',
  permissionCode: '',
  sort: 0,
  visible: 1,
});

const rules: FormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
};

function resetForm() {
  form.parentId = undefined;
  form.type = MENU_TYPE.MENU;
  form.name = '';
  form.code = '';
  form.path = '';
  form.component = '';
  form.icon = '';
  form.permissionCode = '';
  form.sort = 0;
  form.visible = 1;
}

function openCreateRoot() {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增顶级菜单';
  resetForm();
  dialogVisible.value = true;
}

function openCreateChild(row: MenuItem) {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增子菜单';
  resetForm();
  form.parentId = row.id;
  dialogVisible.value = true;
}

function openEdit(row: MenuItem) {
  isEdit.value = true;
  editingId.value = row.id;
  dialogTitle.value = '编辑菜单';
  resetForm();
  form.parentId = row.parentId || undefined;
  form.type = row.type ?? MENU_TYPE.MENU;
  form.name = row.name;
  form.code = row.code;
  form.path = row.path ?? '';
  form.component = row.component ?? '';
  form.icon = row.icon ?? '';
  form.permissionCode = row.permissionCode ?? '';
  form.sort = row.sort ?? 0;
  form.visible = row.visible ?? 1;
  dialogVisible.value = true;
}

async function submitForm() {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    submitting.value = true;
    try {
      const payload: MenuForm = {
        parentId: form.parentId ?? 0,
        type: form.type,
        name: form.name,
        code: form.code,
        path: form.path || undefined,
        component: form.component || undefined,
        icon: form.icon || undefined,
        permissionCode: form.permissionCode || undefined,
        sort: form.sort,
        visible: form.visible,
      };
      if (isEdit.value && editingId.value != null) {
        await menuApi.update(editingId.value, payload);
        ElMessage.success('更新成功');
      } else {
        await menuApi.create(payload);
        ElMessage.success('创建成功');
      }
      dialogVisible.value = false;
      loadTree();
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '操作失败');
    } finally {
      submitting.value = false;
    }
  });
}

async function remove(row: MenuItem) {
  try {
    await ElMessageBox.confirm(`确认删除菜单「${row.name}」？若有子菜单将一并受影响。`, '警告', { type: 'warning' });
  } catch {
    return;
  }
  try {
    await menuApi.remove(row.id);
    ElMessage.success('删除成功');
    loadTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
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
</style>
