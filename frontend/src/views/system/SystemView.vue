<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">系统管理</h2>
        <p class="page-subtitle">用户与角色 · 操作留痕 · 数据字典</p>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- 用户管理 -->
      <el-tab-pane label="用户与角色" name="users">
        <div class="hrms-card">
          <el-table :data="users" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="username" label="用户名" min-width="120" />
            <el-table-column prop="displayName" label="显示名称" min-width="120" />
            <el-table-column label="角色" width="120">
              <template #default="{ row }">
                <el-tag size="small" :type="roleType(row.role)" effect="light">{{ roleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="password" label="密码" width="90">
              <template #default="{ row }"><span class="pwd-mask">{{ row.password }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginAt" label="最近登录" width="170">
              <template #default="{ row }">{{ fmtTime(row.lastLoginAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 数据字典 -->
      <el-tab-pane label="数据字典" name="dicts">
        <div class="hrms-card">
          <div class="tab-toolbar">
            <el-select v-model="dictType" size="default" clearable placeholder="全部字典类型" style="width: 200px" @change="loadDicts">
              <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
            </el-select>
          </div>
          <el-table :data="dicts" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="type" label="字典类型" min-width="160">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.type }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="label" label="显示名" min-width="120" />
            <el-table-column prop="value" label="值" min-width="120" />
            <el-table-column prop="sort" label="排序" width="80" align="center" />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <div class="hrms-card">
          <el-table :data="logs" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column prop="operatorName" label="操作人" width="110" />
            <el-table-column prop="module" label="模块" width="120">
              <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.module }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-tag size="small" :type="actionType(row.action)">{{ actionText(row.action) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="240" show-overflow-tooltip />
            <el-table-column prop="ip" label="IP" width="130" />
            <el-table-column prop="createdAt" label="时间" width="170">
              <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { systemApi } from '@/api'

const activeTab = ref('users')
const users = ref<any[]>([])
const dicts = ref<any[]>([])
const logs = ref<any[]>([])
const dictType = ref('')

const dictTypes = ['gender', 'education', 'attendance_status', 'payroll_status', 'contract_type', 'training_category', 'recruit_channel']

function roleText(r: string) {
  return { ADMIN: '系统管理员', HR: 'HR专员', MANAGER: '部门主管', EMPLOYEE: '普通员工' }[r] || r
}
function roleType(r: string) {
  return ({ ADMIN: 'danger', HR: 'primary', MANAGER: 'warning', EMPLOYEE: 'info' } as Record<string, any>)[r] || 'info'
}
function actionText(a: string) {
  return { CREATE: '新增', UPDATE: '修改', DELETE: '删除', LOGIN: '登录', EXPORT: '导出' }[a] || a
}
function actionType(a: string) {
  return ({ CREATE: 'success', UPDATE: 'primary', DELETE: 'danger', LOGIN: 'info', EXPORT: 'warning' } as Record<string, any>)[a] || 'info'
}
function fmtTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : '-'
}

async function loadDicts() {
  dicts.value = await systemApi.dicts(dictType.value || undefined)
}

onMounted(async () => {
  const [u, d, l] = await Promise.all([systemApi.users(), systemApi.dicts(), systemApi.logs()])
  users.value = u
  dicts.value = d
  logs.value = l
})
</script>

<style scoped>
.tab-toolbar {
  margin-bottom: 12px;
}

.pwd-mask {
  font-family: monospace;
  color: var(--hrms-text-3);
}
</style>
