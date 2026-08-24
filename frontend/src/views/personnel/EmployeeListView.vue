<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">员工档案</h2>
        <p class="page-subtitle">全生命周期电子档案 · 一档到底</p>
      </div>
      <div class="flex gap-8">
        <el-button @click="exportCsv" :icon="Download">导出</el-button>
        <el-button v-if="canWrite" type="primary" :icon="Plus" @click="openCreate">新增员工</el-button>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="hrms-card filter-bar">
      <el-form inline>
        <el-form-item>
          <el-input v-model="query.keyword" placeholder="姓名 / 工号 / 手机号" clearable style="width: 200px" :prefix-icon="Search" @keyup.enter="loadData(1)" @clear="loadData(1)" />
        </el-form-item>
        <el-form-item>
          <el-tree-select
            v-model="query.deptId"
            :data="deptTree"
            :props="{ label: 'name', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="全部部门"
            style="width: 180px"
            @change="loadData(1)"
          />
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px" @change="loadData(1)">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="试用" value="PROBATION" />
            <el-option label="离职" value="LEFT" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.education" placeholder="学历" clearable style="width: 120px" @change="loadData(1)">
            <el-option v-for="e in ['博士', '硕士', '本科', '大专', '高中']" :key="e" :label="e" :value="e" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="query.tag" placeholder="人才标签" clearable style="width: 140px" @change="loadData(1)">
            <el-option v-for="t in tags" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData(1)">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="hrms-card table-card">
      <el-table :data="rows" v-loading="loading" stripe style="width: 100%">
        <el-table-column label="员工" min-width="160">
          <template #default="{ row }">
            <div class="emp-cell" @click="openDetail(row)">
              <el-avatar :size="30" :style="{ background: avatarColor(row.id) }">{{ row.name.charAt(0) }}</el-avatar>
              <div>
                <div class="emp-name">{{ row.name }}</div>
                <div class="emp-no">{{ row.employeeNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="departmentName" label="部门" min-width="110" />
        <el-table-column prop="positionName" label="职位" min-width="120" />
        <el-table-column prop="level" label="职级" width="76" align="center">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.level }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">{{ row.gender === 'MALE' ? '男' : '女' }}</template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="70" align="center" />
        <el-table-column prop="hireDate" label="入职日期" width="110" />
        <el-table-column prop="status" label="状态" width="86" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="人才标签" min-width="150">
          <template #default="{ row }">
            <el-tag v-for="t in (row.tags || '').split(',').filter(Boolean)" :key="t" size="small" effect="light" class="tag-item">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="baseSalary" label="月薪(元)" width="110" align="right">
          <template #default="{ row }">{{ row.baseSalary?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="canWrite" link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm v-if="canWrite" title="确认删除该员工档案？" @confirm="remove(row)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无员工数据" :image-size="80" />
        </template>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.page"
          :page-size="query.size"
          :total="total"
          layout="total, prev, pager, next, sizes"
          :page-sizes="[10, 20, 50]"
          @current-change="loadData"
          @size-change="loadData(1)"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="720px" :title="`员工档案 · ${detail?.name || ''}`">
      <template v-if="detail">
        <div class="detail-head">
          <el-avatar :size="56" :style="{ background: avatarColor(detail.id) }">{{ detail.name.charAt(0) }}</el-avatar>
          <div>
            <div class="detail-name">{{ detail.name }} <StatusTag :status="detail.status" /></div>
            <div class="detail-meta">{{ detail.employeeNo }} · {{ detail.departmentName }} · {{ detail.positionName }}</div>
          </div>
        </div>
        <el-tabs v-model="detailTab" class="detail-tabs">
          <el-tab-pane label="基础信息" name="basic">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="工号">{{ detail.employeeNo }}</el-descriptions-item>
              <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
              <el-descriptions-item label="性别">{{ detail.gender === 'MALE' ? '男' : '女' }}</el-descriptions-item>
              <el-descriptions-item label="出生日期">{{ detail.birthDate }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ detail.email }}</el-descriptions-item>
              <el-descriptions-item label="学历">{{ detail.education }}</el-descriptions-item>
              <el-descriptions-item label="学校">{{ detail.school }}</el-descriptions-item>
              <el-descriptions-item label="专业">{{ detail.major }}</el-descriptions-item>
              <el-descriptions-item label="入职日期">{{ detail.hireDate }}</el-descriptions-item>
              <el-descriptions-item label="转正日期">{{ detail.regularDate || '-' }}</el-descriptions-item>
              <el-descriptions-item label="职级">{{ detail.level }}</el-descriptions-item>
              <el-descriptions-item label="直属上级">{{ detail.managerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="人才标签">
                <el-tag v-for="t in (detail.tags || '').split(',').filter(Boolean)" :key="t" size="small" class="tag-item">{{ t }}</el-tag>
              </el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
          <el-tab-pane label="合同管理" name="contract">
            <el-table :data="detailMap.contracts || []" size="small">
              <el-table-column prop="contractNo" label="合同编号" />
              <el-table-column prop="type" label="类型" width="110" />
              <el-table-column prop="startDate" label="开始" width="110" />
              <el-table-column prop="endDate" label="结束" width="110" />
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }"><el-tag size="small" :type="row.status === '有效' ? 'success' : row.status === '即将到期' ? 'warning' : 'info'">{{ row.status }}</el-tag></template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="教育经历" name="education">
            <el-table :data="detailMap.education || []" size="small">
              <el-table-column prop="degree" label="学历" width="100" />
              <el-table-column prop="school" label="学校" />
              <el-table-column prop="major" label="专业" />
              <el-table-column prop="startDate" label="入学时间" width="120" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="异动记录" name="transfer">
            <el-table :data="detailMap.transfers || []" size="small">
              <el-table-column prop="effectiveDate" label="生效日期" width="110" />
              <el-table-column label="异动类型" width="90">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.type === 'PROMOTE' ? 'success' : row.type === 'TRANSFER' ? 'primary' : 'warning'">{{ typeText(row.type) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="fromLevel" label="原职级" width="70" />
              <el-table-column prop="toLevel" label="新职级" width="70" />
              <el-table-column prop="reason" label="原因" min-width="140" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-drawer>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="editVisible" :title="editForm.id ? '编辑员工' : '新增员工'" width="600px">
      <el-form :model="editForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="editForm.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="工号" required><el-input v-model="editForm.employeeNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号"><el-input v-model="editForm.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别">
            <el-select v-model="editForm.gender" style="width: 100%">
              <el-option label="男" value="MALE" />
              <el-option label="女" value="FEMALE" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="部门">
            <el-tree-select v-model="editForm.departmentId" :data="deptTree" :props="{ label: 'name', children: 'children', value: 'id' }" check-strictly clearable style="width: 100%" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="职级">
            <el-select v-model="editForm.level" style="width: 100%">
              <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学历">
            <el-select v-model="editForm.education" style="width: 100%">
              <el-option v-for="e in ['博士', '硕士', '本科', '大专', '高中']" :key="e" :label="e" :value="e" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="月薪(元)"><el-input-number v-model="editForm.baseSalary" :min="0" :step="500" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="入职日期"><el-date-picker v-model="editForm.hireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, Download, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { employeeApi, orgApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { DeptNode, EmployeeVO } from '@/types'

const loading = ref(false)
const rows = ref<EmployeeVO[]>([])
const total = ref(0)
const tags = ref<string[]>([])
const deptTree = ref<DeptNode[]>([])

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
/** 新增/编辑/删除仅 ADMIN、HR 可见，MANAGER 只读，避免点击无权限按钮触发 403 */
const canWrite = computed(() => ['ADMIN', 'HR'].includes(authStore.user?.role || ''))

const query = reactive({
  keyword: '',
  deptId: undefined as number | undefined,
  status: '',
  education: '',
  tag: '',
  page: 1,
  size: 10,
})

const detailVisible = ref(false)
const detail = ref<any>(null)
const detailMap = ref<Record<string, any>>({})
const detailTab = ref('basic')

const editVisible = ref(false)
const editForm = reactive<any>({})

const levels = ['P1', 'P2', 'P3', 'P4', 'P5', 'P6', 'P7', 'P8', 'M1', 'M2', 'M3', 'M4', 'M5']

const avatarColors = ['#2f6bf0', '#23c17f', '#f59f00', '#f53f3f', '#7b61ff', '#00b8d9', '#e864a8', '#2d7d46']
function avatarColor(id: number) {
  return avatarColors[id % avatarColors.length]
}

function typeText(t: string) {
  const map: Record<string, string> = { TRANSFER: '调岗', PROMOTE: '晋升', REGULAR: '转正', SALARY: '调薪', LEAVE: '离职' }
  return map[t] || t
}

async function loadData(page = query.page) {
  query.page = page
  loading.value = true
  try {
    const res = await employeeApi.search({
      ...query,
      deptId: query.deptId || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.list
    total.value = res.total
    // 全局搜索跳转：带 ?id= 直达员工详情（目标可能不在当前页，按 id 直接拉取），打开后清空 query 防重复
    const qid = route.query.id
    if (qid && !Number.isNaN(Number(qid))) {
      openDetailById(Number(qid))
      router.replace({ path: '/personnel/employees' })
    }
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.deptId = undefined
  query.status = ''
  query.education = ''
  query.tag = ''
  loadData(1)
}

async function openDetail(row: EmployeeVO) {
  detail.value = row
  detailTab.value = 'basic'
  detailVisible.value = true
  const res = await employeeApi.detail(row.id)
  detail.value = res.employee
  detailMap.value = res
}

/** 按 id 直达详情（全局搜索跳转用，目标员工可能不在当前列表页） */
async function openDetailById(id: number) {
  detailTab.value = 'basic'
  detailVisible.value = true
  const res = await employeeApi.detail(id)
  detail.value = res.employee
  detailMap.value = res
}

function openCreate() {
  Object.assign(editForm, {
    id: null, name: '', employeeNo: '', phone: '', gender: 'MALE',
    departmentId: undefined, level: 'P2', education: '本科', baseSalary: 8000, hireDate: '',
  })
  editVisible.value = true
}

function openEdit(row: EmployeeVO) {
  Object.assign(editForm, {
    id: row.id, name: row.name, employeeNo: row.employeeNo, phone: row.phone,
    gender: row.gender, departmentId: row.departmentId, level: row.level,
    education: row.education, baseSalary: row.baseSalary, hireDate: row.hireDate,
  })
  editVisible.value = true
}

async function saveEdit() {
  const payload = {
    name: editForm.name,
    employeeNo: editForm.employeeNo,
    phone: editForm.phone,
    gender: editForm.gender,
    level: editForm.level,
    education: editForm.education,
    baseSalary: editForm.baseSalary,
    hireDate: editForm.hireDate,
    department: editForm.departmentId ? { id: editForm.departmentId } : null,
  }
  if (editForm.id) {
    await employeeApi.update(editForm.id, payload)
    ElMessage.success('保存成功')
  } else {
    await employeeApi.create(payload)
    ElMessage.success('新增成功')
  }
  editVisible.value = false
  loadData()
}

async function remove(row: EmployeeVO) {
  await employeeApi.remove(row.id)
  ElMessage.success('已删除')
  loadData()
}

function exportCsv() {
  const headers = ['工号', '姓名', '性别', '部门', '职位', '职级', '学历', '入职日期', '状态', '月薪']
  const lines = rows.value.map((r) => [r.employeeNo, r.name, r.gender === 'MALE' ? '男' : '女', r.departmentName, r.positionName, r.level, r.education, r.hireDate, r.status, r.baseSalary].join(','))
  const csv = '﻿' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '员工档案.csv'
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('已导出当前页数据')
}

onMounted(async () => {
  const [tree, tagList] = await Promise.all([orgApi.tree(), employeeApi.tags()])
  deptTree.value = tree
  tags.value = tagList
  loadData(1)
})
</script>

<style scoped>
.filter-bar {
  padding: 14px 16px 0;
  margin-bottom: 16px;
}

.filter-bar :deep(.el-form-item) {
  margin-bottom: 14px;
}

.table-card {
  padding: 8px 16px 16px;
}

.emp-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.emp-name {
  font-weight: 500;
  color: var(--hrms-text-1);
}

.emp-no {
  font-size: 12px;
  color: var(--hrms-text-3);
}

.tag-item {
  margin-right: 4px;
  margin-bottom: 2px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 14px 0 4px;
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--hrms-border);
  margin-bottom: 16px;
}

.detail-name {
  font-size: 20px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-meta {
  font-size: 13px;
  color: var(--hrms-text-3);
  margin-top: 4px;
}
</style>
