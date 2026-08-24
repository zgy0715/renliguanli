<template>
  <div class="global-search-panel">
    <el-input
      v-model="keyword"
      placeholder="搜索员工姓名、工号、功能入口..."
      :prefix-icon="Search"
      clearable
      size="large"
      @input="onSearch"
      @keyup.enter="jumpFirst"
    />
    <div class="search-tabs">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        class="search-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key; onSearch()"
      >
        {{ tab.label }}
      </div>
    </div>
    <div class="search-result">
      <template v-if="activeTab === 'employee'">
        <div v-if="employees.length" class="result-list">
          <div
            v-for="emp in employees"
            :key="emp.id"
            class="result-item"
            @click="goEmployee(emp)"
          >
            <el-avatar :size="28" :style="{ background: avatarColor(emp.id) }">{{ emp.name.charAt(0) }}</el-avatar>
            <div class="result-info">
              <div class="result-title">{{ emp.name }}
                <el-tag size="small" class="ml-4" :type="statusTagType(emp.status)">{{ statusText(emp.status) }}</el-tag>
              </div>
              <div class="result-desc">{{ emp.employeeNo }} · {{ emp.departmentName }} · {{ emp.positionName }}</div>
            </div>
            <el-icon class="result-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-else-if="searched && !loading" description="未找到相关员工" :image-size="60" />
      </template>
      <template v-else>
        <div class="result-list">
          <div v-for="fn in filteredFunctions" :key="fn.path" class="result-item" @click="goFunction(fn)">
            <el-icon class="func-icon"><component :is="fn.icon" /></el-icon>
            <div class="result-info">
              <div class="result-title">{{ fn.title }}</div>
              <div class="result-desc">功能模块入口</div>
            </div>
            <el-icon class="result-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowRight } from '@element-plus/icons-vue'
import { employeeApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { EmployeeVO } from '@/types'

const router = useRouter()
const authStore = useAuthStore()
const keyword = ref('')
const employees = ref<EmployeeVO[]>([])
const loading = ref(false)
const searched = ref(false)

/** 员工搜索需 /employees 权限，EMPLOYEE 角色不可见该 Tab，避免触发 403 */
const canSearchEmployee = computed(() => ['ADMIN', 'HR', 'MANAGER'].includes(authStore.user?.role || ''))
const activeTab = ref<'employee' | 'function'>(canSearchEmployee.value ? 'employee' : 'function')

const tabs = computed<{ key: 'employee' | 'function'; label: string }[]>(() =>
  canSearchEmployee.value
    ? [
        { key: 'employee', label: '员工' },
        { key: 'function', label: '功能' },
      ]
    : [{ key: 'function', label: '功能' }],
)

const functions = [
  { title: '工作台', path: '/dashboard', icon: 'HomeFilled' },
  { title: '组织架构', path: '/org', icon: 'Share' },
  { title: '员工档案', path: '/personnel/employees', icon: 'UserFilled' },
  { title: '考勤排班', path: '/attendance', icon: 'Calendar' },
  { title: '薪酬核算', path: '/payroll', icon: 'Money' },
  { title: '绩效管理', path: '/performance', icon: 'TrendCharts' },
  { title: '培训发展', path: '/training', icon: 'Reading' },
  { title: '招聘管理', path: '/recruitment', icon: 'Search' },
  { title: '智能助手', path: '/assistant', icon: 'MagicStick' },
  { title: '员工自助', path: '/self-service', icon: 'User' },
  { title: '系统管理', path: '/system', icon: 'Setting' },
]

const filteredFunctions = computed(() => {
  if (!keyword.value) return functions.slice(0, 6)
  return functions.filter((f) => f.title.includes(keyword.value))
})

async function onSearch() {
  if (activeTab.value !== 'employee') return
  searched.value = true
  if (!keyword.value) {
    employees.value = []
    return
  }
  loading.value = true
  try {
    const res = await employeeApi.search({ keyword: keyword.value, page: 1, size: 8 })
    employees.value = res.list
  } catch {
    employees.value = []
  } finally {
    loading.value = false
  }
}

function jumpFirst() {
  if (activeTab.value === 'function' && filteredFunctions.value.length) {
    goFunction(filteredFunctions.value[0])
  } else if (employees.value.length) {
    goEmployee(employees.value[0])
  }
}

function goEmployee(emp: EmployeeVO) {
  router.push({ path: '/personnel/employees', query: { id: emp.id } })
}

function goFunction(fn: { path: string }) {
  router.push(fn.path)
}

const avatarColors = ['#2f6bf0', '#23c17f', '#f59f00', '#f53f3f', '#7b61ff', '#00b8d9']
function avatarColor(id: number) {
  return avatarColors[id % avatarColors.length]
}

const statusMap: Record<string, string> = {
  ACTIVE: '在职',
  PROBATION: '试用',
  LEFT: '离职',
  SUSPEND: '停职',
}
function statusText(s: string) {
  return statusMap[s] || s
}
function statusTagType(s: string): 'success' | 'warning' | 'info' | 'danger' {
  if (s === 'ACTIVE') return 'success'
  if (s === 'PROBATION') return 'warning'
  if (s === 'LEFT') return 'info'
  return 'danger'
}
</script>

<style scoped>
.global-search-panel {
  padding: 4px;
}

.search-tabs {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.search-tab {
  font-size: 13px;
  padding: 4px 12px;
  border-radius: 6px;
  cursor: pointer;
  color: var(--hrms-text-2);
  background: var(--hrms-bg-subtle);
}

.search-tab.active {
  background: var(--hrms-primary-bg);
  color: var(--hrms-primary);
}

.search-result {
  margin-top: 10px;
  max-height: 380px;
  overflow-y: auto;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.result-item:hover {
  background: var(--hrms-bg-subtle);
}

.result-info {
  flex: 1;
  min-width: 0;
}

.result-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--hrms-text-1);
  display: flex;
  align-items: center;
}

.result-desc {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-arrow {
  color: var(--hrms-text-3);
}

.func-icon {
  font-size: 18px;
  color: var(--hrms-primary);
  width: 28px;
  display: flex;
  justify-content: center;
}

.ml-4 {
  margin-left: 6px;
}
</style>
