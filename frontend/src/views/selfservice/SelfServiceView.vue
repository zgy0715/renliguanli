<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">员工自助</h2>
        <p class="page-subtitle">档案 · 考勤 · 工资条 · 假期 · 证明 · 一站式自助</p>
      </div>
    </div>

    <!-- 顶部个人信息卡 -->
    <div class="hrms-card profile-banner">
      <el-avatar :size="64" :style="{ background: avatarColor(profile.id) }">{{ profile.name?.charAt(0) }}</el-avatar>
      <div class="profile-info">
        <div class="profile-name">{{ profile.name }} <el-tag size="small" effect="light">{{ levelText(profile.level) }}</el-tag></div>
        <div class="profile-meta">{{ profile.employeeNo }} · {{ profile.department }} · {{ profile.position }}</div>
        <div class="profile-tags">
          <el-tag v-for="t in (profile.tags || '').split(',').filter(Boolean)" :key="t" size="small" effect="plain" class="p-tag">{{ t }}</el-tag>
        </div>
      </div>
      <div class="profile-stat">
        <div class="p-stat-item">
          <div class="p-stat-value">{{ profile.hireDate }}</div>
          <div class="p-stat-label">入职日期</div>
        </div>
        <div class="p-stat-item">
          <div class="p-stat-value">{{ profile.gridCell }}</div>
          <div class="p-stat-label">九宫格定位</div>
        </div>
        <div class="p-stat-item">
          <div class="p-stat-value">{{ statusText(profile.status) }}</div>
          <div class="p-stat-label">在职状态</div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="self-tabs">
      <!-- 假期余额 -->
      <el-tab-pane label="假期余额" name="leave">
        <div class="hrms-card">
          <el-row :gutter="16">
            <el-col :span="6" v-for="l in leaveItems" :key="l.key">
              <div class="leave-card">
                <div class="leave-name">{{ l.label }}</div>
                <div class="leave-remain">{{ l.remain }}<span class="leave-unit">天</span></div>
                <el-progress :percentage="l.usedRate" :stroke-width="8" :color="l.remain > 0 ? '#2f6bf0' : '#f53f3f'" />
                <div class="leave-detail">已用 {{ l.used }} / 共 {{ l.total }}</div>
              </div>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 我的考勤 -->
      <el-tab-pane label="我的考勤" name="attendance">
        <div class="hrms-card">
          <div class="tab-toolbar">
            <el-date-picker v-model="attMonth" type="month" value-format="YYYY-MM" :clearable="false" @change="loadAttendance" />
            <el-button :icon="Download" @click="exportAttendance">导出打卡记录</el-button>
          </div>
          <el-table :data="attRows" stripe size="default">
            <el-table-column prop="workDate" label="日期" width="120" />
            <el-table-column label="星期" width="90">
              <template #default="{ row }">{{ weekday(row.workDate) }}</template>
            </el-table-column>
            <el-table-column prop="checkIn" label="上班打卡" width="110">
              <template #default="{ row }">{{ row.checkIn || '-' }}</template>
            </el-table-column>
            <el-table-column prop="checkOut" label="下班打卡" width="110">
              <template #default="{ row }">{{ row.checkOut || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="overtimeHours" label="加班(h)" width="90" align="center">
              <template #default="{ row }">{{ row.overtimeHours ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" />
          </el-table>
          <div class="att-summary" v-if="attRows.length">
            <span>本月出勤 <b>{{ attRows.length }}</b> 天</span>
            <span>迟到 <b class="c-warn">{{ countStatus('LATE') }}</b> 次</span>
            <span>早退 <b class="c-warn">{{ countStatus('EARLY') }}</b> 次</span>
            <span>缺勤 <b class="c-bad">{{ countStatus('ABSENT') }}</b> 次</span>
            <span>出勤率 <b>{{ attRate }}%</b></span>
          </div>
        </div>
      </el-tab-pane>

      <!-- 我的工资条 -->
      <el-tab-pane label="我的工资条" name="payslips">
        <div class="hrms-card">
          <el-table :data="payslips" stripe>
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column label="应发" width="130" align="right">
              <template #default="{ row }">{{ money(row.gross) }}</template>
            </el-table-column>
            <el-table-column label="实发" width="130" align="right">
              <template #default="{ row }"><span class="c-green">{{ money(row.net) }}</span></template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="payDate" label="发放日期" width="120" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="go('/payroll')">查看明细</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我的绩效 -->
      <el-tab-pane label="我的绩效" name="performance">
        <div class="hrms-card">
          <el-table :data="myPerf" stripe>
            <el-table-column prop="period" label="考核周期" width="140" />
            <el-table-column prop="mode" label="考核模式" width="110" />
            <el-table-column prop="score" label="得分" width="100">
              <template #default="{ row }"><b>{{ row.score }}</b></template>
            </el-table-column>
            <el-table-column label="评级" width="90" align="center">
              <template #default="{ row }"><StatusTag :status="row.rating" /></template>
            </el-table-column>
            <el-table-column prop="interviewNote" label="面谈记录" min-width="220" show-overflow-tooltip />
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 电子证明 -->
      <el-tab-pane label="电子证明" name="certificates">
        <div class="hrms-card">
          <div class="tab-toolbar">
            <el-button type="primary" :icon="Plus" @click="certVisible = true">申请证明</el-button>
          </div>
          <el-table :data="certificates" stripe>
            <el-table-column label="类型" width="140">
              <template #default="{ row }">{{ certType(row.type) }}</template>
            </el-table-column>
            <el-table-column prop="purpose" label="用途" min-width="200" />
            <el-table-column prop="status" label="状态" width="110" align="center">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="effectiveDate" label="生效日期" width="120" />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 申请证明 -->
    <el-dialog v-model="certVisible" title="申请电子证明" width="480px">
      <el-form label-width="80px">
        <el-form-item label="证明类型">
          <el-select v-model="certForm.type" style="width: 100%">
            <el-option label="在职证明" value="EMPLOYMENT" />
            <el-option label="收入证明" value="INCOME" />
          </el-select>
        </el-form-item>
        <el-form-item label="用途">
          <el-input v-model="certForm.purpose" placeholder="如：购房贷款、签证办理等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certVisible = false">取消</el-button>
        <el-button type="primary" @click="applyCert">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Plus } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import { selfServiceApi } from '@/api'

const router = useRouter()

function go(path: string) {
  router.push(path)
}

const activeTab = ref('leave')
const profile = ref<any>({})
const leave = ref<any>({})
const attRows = ref<any[]>([])
// 本地时区月份（toISOString 为 UTC，东八区月初会错一天）
const attNow = new Date()
const attMonth = ref(`${attNow.getFullYear()}-${String(attNow.getMonth() + 1).padStart(2, '0')}`)
const payslips = ref<any[]>([])
const myPerf = ref<any[]>([])
const certificates = ref<any[]>([])
const certVisible = ref(false)
const certForm = reactive({ type: 'EMPLOYMENT', purpose: '' })

const leaveItems = computed(() => [
  { key: 'annual', label: '年假', total: leave.value.annualLeaveTotal || 0, used: leave.value.annualLeaveUsed || 0, remain: leave.value.annualLeaveRemain ?? 0, usedRate: leave.value.annualLeaveTotal ? Math.round(((leave.value.annualLeaveUsed || 0) / leave.value.annualLeaveTotal) * 100) : 0 },
  { key: 'sick', label: '病假', total: leave.value.sickLeave || 0, used: 0, remain: leave.value.sickLeave || 0, usedRate: 0 },
  { key: 'personal', label: '事假', total: leave.value.personalLeave || 0, used: 0, remain: leave.value.personalLeave || 0, usedRate: 0 },
  { key: 'comp', label: '调休', total: leave.value.compTime || 0, used: 0, remain: leave.value.compTime || 0, usedRate: 0 },
])

const attRate = computed(() => {
  if (!attRows.value.length) return 0
  const normal = attRows.value.filter((r) => r.status === 'NORMAL').length
  return Math.round((normal / attRows.value.length) * 100)
})

function levelText(l: string) {
  return l ? `职级 ${l}` : ''
}
function statusText(s: string) {
  return { ACTIVE: '在职', PROBATION: '试用', LEFT: '离职' }[s] || s
}
function weekday(d: string) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][new Date(d).getDay()]
}
function certType(t: string) {
  return { EMPLOYMENT: '在职证明', INCOME: '收入证明' }[t] || t
}
function countStatus(s: string) {
  return attRows.value.filter((r) => r.status === s).length
}
function money(v: number) {
  return (v ?? 0).toLocaleString()
}
function avatarColor(id: number) {
  const colors = ['#2f6bf0', '#23c17f', '#f59f00', '#7b61ff', '#f53f3f', '#00b8d9']
  return colors[(id || 0) % colors.length]
}

function exportAttendance() {
  const headers = ['日期', '上班打卡', '下班打卡', '状态', '加班小时']
  const lines = attRows.value.map((r) => [r.workDate, r.checkIn || '', r.checkOut || '', r.status, r.overtimeHours ?? ''].join(','))
  const csv = '﻿' + [headers.join(','), ...lines].join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `考勤记录-${attMonth.value}.csv`
  a.click()
  URL.revokeObjectURL(a.href)
  ElMessage.success('已导出')
}

async function loadAttendance() {
  attRows.value = await selfServiceApi.myAttendance(attMonth.value)
}

async function applyCert() {
  await selfServiceApi.applyCertificate(certForm.type, certForm.purpose)
  ElMessage.success('申请已提交')
  certVisible.value = false
  certForm.purpose = ''
  certificates.value = await selfServiceApi.certificates()
}

onMounted(async () => {
  const [p, lb, ps, pf, certs] = await Promise.all([
    selfServiceApi.profile(),
    selfServiceApi.leaveBalance(),
    selfServiceApi.payslips(),
    selfServiceApi.myPerformance(),
    selfServiceApi.certificates(),
  ])
  profile.value = p
  leave.value = lb
  payslips.value = ps
  myPerf.value = pf
  certificates.value = certs
  await loadAttendance()
})
</script>

<style scoped>
.profile-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  margin-bottom: 16px;
}

.profile-name {
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-meta {
  font-size: 13px;
  color: var(--hrms-text-3);
  margin-top: 6px;
}

.profile-tags {
  margin-top: 8px;
}

.p-tag {
  margin-right: 6px;
}

.profile-stat {
  margin-left: auto;
  display: flex;
  gap: 32px;
}

.p-stat-item {
  text-align: center;
}

.p-stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--hrms-text-1);
}

.p-stat-label {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin-top: 4px;
}

.self-tabs {
  background: var(--hrms-card);
  border: 1px solid var(--hrms-border);
  border-radius: 12px;
  padding: 4px 16px 16px;
}

.leave-card {
  border: 1px solid var(--hrms-border);
  border-radius: 10px;
  padding: 16px;
}

.leave-name {
  font-size: 13px;
  color: var(--hrms-text-3);
}

.leave-remain {
  font-size: 30px;
  font-weight: 800;
  color: var(--hrms-text-1);
  margin: 6px 0 10px;
}

.leave-unit {
  font-size: 13px;
  font-weight: 400;
  color: var(--hrms-text-3);
  margin-left: 2px;
}

.leave-detail {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin-top: 6px;
}

.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.att-summary {
  display: flex;
  gap: 24px;
  padding: 14px;
  margin-top: 12px;
  background: var(--hrms-bg);
  border-radius: 8px;
  font-size: 13px;
  color: var(--hrms-text-2);
}

.c-green {
  color: var(--hrms-success);
  font-weight: 600;
}

.c-warn {
  color: var(--hrms-warning);
}

.c-bad {
  color: var(--hrms-danger);
}
</style>
