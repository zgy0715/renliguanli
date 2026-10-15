<template>
  <div class="page-container workbench">
    <!-- 欢迎横幅 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="welcome-greet">{{ greeting }}</div>
        <div class="welcome-sub">{{ greetingSub }}</div>
      </div>
      <div class="welcome-right">
        <div class="welcome-date">{{ todayText }}</div>
        <el-button-group>
          <el-button v-if="isCompanyView" size="small" type="primary" plain @click="go('/personnel/employees')">员工档案</el-button>
          <el-button v-if="isCompanyView" size="small" type="primary" plain @click="go('/attendance')">考勤排班</el-button>
          <el-button v-if="isCompanyView" size="small" type="primary" plain @click="go('/payroll')">薪酬核算</el-button>
          <el-button size="small" type="primary" plain @click="go('/assistant')">智能助手</el-button>
        </el-button-group>
      </div>
    </div>

    <!-- KPI 行 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="4" v-for="kpi in kpis" :key="kpi.label">
        <KpiCard :label="kpi.label" :value="kpi.value" :unit="kpi.unit" :icon="kpi.icon" :accent="kpi.accent" />
      </el-col>
    </el-row>

    <!-- 可拖拽组件区 -->
    <el-row :gutter="16" class="widget-grid">
      <el-col :span="w.span" v-for="(w, i) in widgets" :key="w.key">
        <div
          class="hrms-card widget-card"
          :draggable="true"
          @dragstart="dragStart(i)"
          @dragover.prevent
          @drop="dropAt(i)"
          @dragenter.prevent
        >
          <div class="widget-head">
            <div class="widget-title">
              <el-icon><component :is="w.icon" /></el-icon>
              {{ w.title }}
            </div>
            <div class="widget-actions">
              <el-dropdown trigger="click" @command="(cmd) => handleWidgetCmd(cmd, w)">
                <el-icon class="widget-more"><MoreFilled /></el-icon>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="move-left" :disabled="i === 0">左移</el-dropdown-item>
                    <el-dropdown-item command="move-right" :disabled="i === widgets.length - 1">右移</el-dropdown-item>
                    <el-dropdown-item command="hide" divided>隐藏此组件</el-dropdown-item>
                    <el-dropdown-item command="restore" divided>恢复默认布局</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          <div class="widget-body">
            <!-- 人员趋势 -->
            <template v-if="w.key === 'headcount'">
              <BaseChart :option="headcountOption" height="260px" />
            </template>
            <!-- 薪酬趋势 -->
            <template v-else-if="w.key === 'payroll'">
              <BaseChart :option="payrollOption" height="260px" />
            </template>
            <!-- 我的待办 -->
            <template v-else-if="w.key === 'todos'">
              <div v-loading="loading">
                <div class="todo-empty" v-if="!todos.length">太棒了，暂无待办事项 🎉</div>
                <div class="todo-item" v-for="t in todos" :key="t.id" :class="`todo-item--${priorityKey(t.priority)}`">
                  <div class="todo-dot"></div>
                  <div class="todo-main">
                    <div class="todo-title">{{ t.title }}</div>
                    <div class="todo-meta">{{ typeText(t.type) }}<template v-if="t.dueTime"> · {{ t.dueTime }}</template></div>
                  </div>
                  <el-tag size="small" :type="priorityType(t.priority)">{{ priorityText(t.priority) }}</el-tag>
                </div>
              </div>
            </template>
            <!-- 企业公告 -->
            <template v-else-if="w.key === 'announcements'">
              <div v-loading="loading">
                <div class="todo-empty" v-if="!announcements.length">暂无公告</div>
                <div class="ann-item" v-for="a in announcements" :key="a.id" @click="openAnnouncement(a)">
                  <el-tag size="small" effect="plain" class="ann-type">{{ typeText(a.type) }}</el-tag>
                  <div class="ann-title" :class="{ 'ann-title--pin': a.pinned }">
                    <el-icon v-if="a.pinned"><Top /></el-icon>{{ a.title }}
                  </div>
                  <div class="ann-time">{{ fmtTime(a.publishTime) }}</div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="annVisible" :title="currentAnn?.title" width="560px">
      <div class="ann-content">{{ currentAnn?.content }}</div>
      <div class="ann-meta">{{ typeText(currentAnn?.type) }} · {{ fmtTime(currentAnn?.publishTime) }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MoreFilled, Top, UserFilled, TrendCharts, Money, List, Bell, Plus, Minus, Calendar, CircleCheck, Clock } from '@element-plus/icons-vue'
import KpiCard from '@/components/KpiCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import { useAuthStore } from '@/stores/auth'
import { dashboardApi, selfServiceApi } from '@/api'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const todos = ref<any[]>([])
const announcements = ref<any[]>([])
const summary = ref<any>(null)
const headcountTrend = ref<any>({ months: [], headcounts: [], hires: [], leaves: [] })
const payrollTrend = ref<any>({ months: [], costs: [] })
const myAtt = ref<any[]>([])
const myPayslips = ref<any[]>([])

const annVisible = ref(false)
const currentAnn = ref<any>(null)

const role = computed(() => authStore.user?.role || 'EMPLOYEE')
const isCompanyView = computed(() => ['ADMIN', 'HR', 'MANAGER'].includes(role.value))

const todayText = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const greeting = computed(() => {
  const h = new Date().getHours()
  const part = h < 6 ? '凌晨好' : h < 12 ? '上午好' : h < 18 ? '下午好' : '晚上好'
  const name = authStore.user?.displayName || ''
  if (role.value === 'ADMIN') return `${part}，${name}！`
  if (role.value === 'HR') return `${part}，${name}！今天也要为团队保驾护航`
  if (role.value === 'MANAGER') return `${part}，${name}！团队管理一切顺利`
  return `${part}，${name}！`
})

const greetingSub = computed(() => {
  if (role.value === 'ADMIN') return '整体经营数据平稳，请关注今日待办与人才动态。'
  if (role.value === 'HR') return `系统为您汇总了 ${todos.value.length} 条待办，先处理高优事项。`
  if (role.value === 'MANAGER') return '今日团队出勤平稳，可随时查看部门人力数据。'
  const total = myAtt.value.length
  const abnormal = myAtt.value.filter((r) => r.status === 'LATE' || r.status === 'EARLY' || r.status === 'ABSENT').length
  return `本月出勤 ${total} 天，其中异常 ${abnormal} 天，注意保重身体。`
})

const kpis = computed(() => {
  if (!isCompanyView.value) {
    const total = myAtt.value.length
    const normal = myAtt.value.filter((r) => r.status === 'NORMAL').length
    const rate = total ? Math.round((normal / total) * 100) : 100
    return [
      { label: '本月出勤', value: total, unit: '天', icon: Calendar, accent: '#2f6bf0' },
      { label: '出勤率', value: rate, unit: '%', icon: CircleCheck, accent: '#23c17f' },
      { label: '工资条', value: myPayslips.value.length, unit: '份', icon: Money, accent: '#7b61ff' },
      { label: '待办事项', value: todos.value.length, unit: '条', icon: List, accent: '#f59f00' },
      { label: '企业公告', value: announcements.value.length, unit: '条', icon: Bell, accent: '#00b8d9' },
      { label: '本年度假余额', value: myLeaveRemain, unit: '天', icon: Clock, accent: '#f53f3f' },
    ]
  }
  return [
    { label: '在职员工', value: summary.value?.totalEmployees ?? '-', unit: '人', icon: UserFilled, accent: '#2f6bf0' },
    { label: '试用期', value: summary.value?.probation ?? '-', unit: '人', icon: UserFilled, accent: '#00b8d9' },
    { label: '本月新入职', value: summary.value?.newHires ?? '-', unit: '人', icon: Plus, accent: '#23c17f' },
    { label: '本月离职', value: summary.value?.resignations ?? '-', unit: '人', icon: Minus, accent: '#f59f00' },
    { label: '离职率', value: summary.value?.attritionRate ?? '-', unit: '%', icon: TrendCharts, accent: '#f53f3f' },
    { label: '整体出勤率', value: summary.value?.attendanceRate ?? '-', unit: '%', icon: Calendar, accent: '#7b61ff' },
  ]
})

const myLeaveRemain = ref(0)

// ============ 可拖拽组件 ============
const DEFAULT_WIDGETS = [
  { key: 'headcount', title: '人员规模趋势', icon: TrendCharts, span: 16 },
  { key: 'todos', title: '我的待办', icon: List, span: 8 },
  { key: 'payroll', title: '薪酬成本趋势', icon: Money, span: 16 },
  { key: 'announcements', title: '企业公告', icon: Bell, span: 8 },
]

const widgets = ref<any[]>([])
let dragIndex = -1

/** 当前角色可见的默认组件：员工无薪酬/人员规模等管理端权限，默认布局不下发对应组件 */
function defaultWidgets() {
  return isCompanyView.value
    ? DEFAULT_WIDGETS
    : DEFAULT_WIDGETS.filter((w) => w.key !== 'payroll' && w.key !== 'headcount')
}

function loadWidgetOrder() {
  const saved = localStorage.getItem('hrms-widget-order')
  if (saved) {
    try {
      const order = JSON.parse(saved)
      const defs = defaultWidgets()
      widgets.value = defs.filter((w) => order.includes(w.key))
      if (widgets.value.length < defs.length) {
        defs.forEach((w) => {
          if (!order.includes(w.key)) widgets.value.push(w)
        })
      }
      return
    } catch {
      /* ignore */
    }
  }
  widgets.value = [...defaultWidgets()]
}

function persistOrder() {
  localStorage.setItem('hrms-widget-order', JSON.stringify(widgets.value.map((w) => w.key)))
}

function dragStart(i: number) {
  dragIndex = i
}

function dropAt(i: number) {
  if (dragIndex < 0 || dragIndex === i) return
  const [moved] = widgets.value.splice(dragIndex, 1)
  widgets.value.splice(i, 0, moved)
  dragIndex = -1
  persistOrder()
}

function handleWidgetCmd(cmd: string, w: any) {
  if (cmd === 'hide') {
    widgets.value = widgets.value.filter((x) => x.key !== w.key)
    persistOrder()
    ElMessage.success('已隐藏，可在布局菜单中恢复')
  } else if (cmd === 'restore') {
    widgets.value = [...defaultWidgets()]
    persistOrder()
    ElMessage.success('已恢复默认布局')
  } else if (cmd === 'move-left' || cmd === 'move-right') {
    const i = widgets.value.findIndex((x) => x.key === w.key)
    const j = cmd === 'move-left' ? i - 1 : i + 1
    if (i >= 0 && j >= 0 && j < widgets.value.length) {
      const [m] = widgets.value.splice(i, 1)
      widgets.value.splice(j, 0, m)
      persistOrder()
    }
  }
}

// ============ 图表 ============
const headcountOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['在职', '入职', '离职'], top: 0 },
  grid: { left: 40, right: 20, top: 30, bottom: 24 },
  xAxis: { type: 'category', data: headcountTrend.value.months, axisTick: { show: false } },
  yAxis: { type: 'value' },
  series: [
    { name: '在职', type: 'line', data: headcountTrend.value.headcounts, smooth: true, lineStyle: { width: 3 }, areaStyle: { opacity: 0.08 } },
    { name: '入职', type: 'bar', data: headcountTrend.value.hires, barWidth: 12, itemStyle: { color: '#23c17f', borderRadius: [3, 3, 0, 0] } },
    { name: '离职', type: 'bar', data: headcountTrend.value.leaves, barWidth: 12, itemStyle: { color: '#f53f3f', borderRadius: [3, 3, 0, 0] } },
  ],
}))

const payrollOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}<br/>${p[0].marker}${fmtMoney(p[0].value)}元` },
  grid: { left: 50, right: 20, top: 24, bottom: 24 },
  xAxis: { type: 'category', data: payrollTrend.value.months, axisTick: { show: false } },
  yAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v >= 10000 ? v / 10000 + '万' : v) } },
  series: [
    {
      type: 'line',
      data: payrollTrend.value.costs,
      smooth: true,
      lineStyle: { width: 3, color: '#7b61ff' },
      itemStyle: { color: '#7b61ff' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(123,97,255,0.25)' }, { offset: 1, color: 'rgba(123,97,255,0)' }],
        },
      },
    },
  ],
}))

// ============ 数据加载 ============
async function loadAll() {
  loading.value = true
  try {
    const [t, a] = await Promise.all([dashboardApi.todos(), dashboardApi.announcements()])
    todos.value = t
    announcements.value = a
    if (isCompanyView.value) {
      const [s, h, p] = await Promise.all([
        dashboardApi.summary(),
        dashboardApi.headcountTrend(),
        dashboardApi.payrollTrend(),
      ])
      summary.value = s
      headcountTrend.value = h
      payrollTrend.value = p
    } else {
      const [att, slips, lb] = await Promise.all([
        selfServiceApi.myAttendance(),
        selfServiceApi.payslips(),
        selfServiceApi.leaveBalance(),
      ])
      myAtt.value = att
      myPayslips.value = slips
      myLeaveRemain.value = lb.annualLeaveRemain
    }
  } finally {
    loading.value = false
  }
}

function typeText(t: string) {
  const map: Record<string, string> = {
    LEAVE: '请假', OVERTIME: '加班', OFFICIAL: '公告', POLICY: '制度', ACTIVITY: '活动',
    TRAINING: '培训', NEWS: '通知', CONTRACT: '合同', BIRTHDAY: '生日', PERFORMANCE: '绩效',
  }
  return map[t] || t || '-'
}

/** 后端待办优先级为中文（高/中/低），统一归一化为英文枚举再映射 */
function normPriority(p: string) {
  const map: Record<string, string> = { 高: 'HIGH', 中: 'MEDIUM', 低: 'LOW' }
  return map[p] || p
}

function priorityText(p: string) {
  return { HIGH: '高', MEDIUM: '中', LOW: '低' }[normPriority(p)] || p
}

function priorityType(p: string) {
  return ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' } as Record<string, any>)[normPriority(p)] || 'info'
}

function priorityKey(p: string) {
  return normPriority(p).toLowerCase()
}

function fmtTime(t: string | null | undefined) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 16)
}

function fmtMoney(v: number) {
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
}

function openAnnouncement(a: any) {
  currentAnn.value = a
  annVisible.value = true
}

function go(path: string) {
  router.push(path)
}

onMounted(() => {
  loadWidgetOrder()
  loadAll()
})
</script>

<style scoped>
.welcome-banner {
  background: linear-gradient(120deg, #2f6bf0, #4d8bff 70%, #6b96f7);
  border-radius: 16px;
  padding: 24px 28px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  box-shadow: 0 6px 20px rgba(47, 107, 240, 0.25);
}

.welcome-greet {
  font-size: 24px;
  font-weight: 700;
}

.welcome-sub {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 6px;
}

.welcome-date {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 10px;
  text-align: right;
}

.welcome-right :deep(.el-button) {
  --el-button-text-color: #fff;
  --el-button-border-color: rgba(255, 255, 255, 0.4);
  --el-button-hover-text-color: #2f6bf0;
  --el-button-hover-border-color: #fff;
  --el-button-hover-bg-color: #fff;
}

.widget-grid {
  row-gap: 16px;
}

.widget-card {
  cursor: default;
  transition: box-shadow 0.2s, transform 0.2s;
  height: 100%;
}

.widget-card:hover {
  box-shadow: var(--hrms-shadow-hover);
}

.widget-card[draggable='true'] .widget-head {
  cursor: grab;
}

.widget-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  margin-bottom: 4px;
  border-bottom: 1px dashed var(--hrms-border);
}

.widget-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.widget-more {
  color: var(--hrms-text-3);
  cursor: pointer;
}

.widget-more:hover {
  color: var(--hrms-primary);
}

.todo-empty {
  text-align: center;
  color: var(--hrms-text-3);
  padding: 30px 0;
  font-size: 13px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--hrms-bg);
}

.todo-item--high .todo-dot {
  background: var(--hrms-danger);
}

.todo-item--medium .todo-dot {
  background: var(--hrms-warning);
}

.todo-item--low .todo-dot {
  background: var(--hrms-text-3);
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.todo-main {
  flex: 1;
  min-width: 0;
}

.todo-title {
  font-size: 13px;
  color: var(--hrms-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-meta {
  font-size: 11px;
  color: var(--hrms-text-3);
  margin-top: 2px;
}

.ann-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 4px;
  border-bottom: 1px solid var(--hrms-bg);
  cursor: pointer;
}

.ann-item:hover .ann-title {
  color: var(--hrms-primary);
}

.ann-type {
  flex-shrink: 0;
}

.ann-title {
  flex: 1;
  font-size: 13px;
  color: var(--hrms-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 3px;
}

.ann-title--pin {
  color: var(--hrms-warning);
  font-weight: 600;
}

.ann-time {
  font-size: 11px;
  color: var(--hrms-text-3);
  flex-shrink: 0;
}

.ann-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--hrms-text-1);
  white-space: pre-wrap;
}

.ann-meta {
  margin-top: 16px;
  font-size: 12px;
  color: var(--hrms-text-3);
}
</style>
