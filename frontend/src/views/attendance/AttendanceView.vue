<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">考勤排班</h2>
        <p class="page-subtitle">出勤月历 · 异常自动识别 · 一键确认归档</p>
      </div>
      <div class="flex gap-8">
        <el-date-picker
          v-model="month"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          @change="loadAll"
        />
      </div>
    </div>

    <!-- 月度指标 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="4"><KpiCard label="出勤率" :value="stat.attendanceRate" unit="%" icon="CircleCheck" accent="#23c17f" /></el-col>
      <el-col :span="4"><KpiCard label="正常" :value="stat.normal" unit="次" icon="CircleCheckFilled" accent="#2f6bf0" /></el-col>
      <el-col :span="4"><KpiCard label="迟到" :value="stat.late" unit="次" icon="AlarmClock" accent="#f59f00" /></el-col>
      <el-col :span="4"><KpiCard label="早退" :value="stat.early" unit="次" icon="Clock" accent="#e864a8" /></el-col>
      <el-col :span="4"><KpiCard label="缺勤" :value="stat.absent" unit="次" icon="WarningFilled" accent="#f53f3f" /></el-col>
      <el-col :span="4"><KpiCard label="待确认异常" :value="pendingCount" unit="条" icon="Bell" accent="#7b61ff" /></el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 出勤月历 -->
      <el-col :span="14">
        <ChartCard title="出勤月历" subtitle="每格颜色深浅 = 当日出勤人数 · 悬浮查看明细">
          <div class="calendar-grid" v-loading="loading">
            <div class="cal-head" v-for="w in ['一', '二', '三', '四', '五', '六', '日']" :key="w">{{ w }}</div>
            <template v-for="(cell, idx) in calendarDays" :key="idx">
              <div class="cal-blank" v-if="!cell"></div>
              <el-tooltip v-else :content="cellTip(cell)" placement="top">
                <div
                  class="cal-cell"
                  :class="[`cal-cell--${heatLevel(cell.rate)}`, { 'cal-cell--weekend': !cell.isWorkday, 'cal-cell--has-exception': cell.absent + cell.late + cell.early > 0 }]"
                  @click="openDayDetail(cell)"
                >
                  <div class="cal-day">{{ Number(cell.date.slice(-2)) }}</div>
                  <div class="cal-nums" v-if="cell.total > 0">
                    <span class="ok">{{ cell.normal }}</span>
                    <span class="warn" v-if="cell.late">迟{{ cell.late }}</span>
                    <span class="bad" v-if="cell.absent">缺{{ cell.absent }}</span>
                  </div>
                </div>
              </el-tooltip>
            </template>
          </div>
        </ChartCard>
      </el-col>

      <!-- 班次管理 -->
      <el-col :span="10">
        <ChartCard title="班次配置" subtitle="排班规则">
          <el-table :data="shifts" size="small">
            <el-table-column prop="name" label="班次" width="110" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column label="上班" width="76">
              <template #default="{ row }">{{ (row.workStart || '').slice(0, 5) || '-' }}</template>
            </el-table-column>
            <el-table-column label="下班" width="76">
              <template #default="{ row }">{{ (row.workEnd || '').slice(0, 5) || '-' }}</template>
            </el-table-column>
            <el-table-column label="标准工时" width="90">
              <template #default="{ row }">{{ row.standardHours ? row.standardHours + 'h' : '-' }}</template>
            </el-table-column>
          </el-table>
          <div class="legend-box">
            <div class="legend-item"><span class="dot ok"></span>正常</div>
            <div class="legend-item"><span class="dot warn"></span>迟到/早退</div>
            <div class="legend-item"><span class="dot bad"></span>缺勤</div>
            <div class="legend-item"><span class="dot leave"></span>请假/外勤</div>
          </div>
        </ChartCard>
      </el-col>
    </el-row>

    <!-- Tab：异常处理 / 月度汇总 -->
    <div class="hrms-card table-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="异常处理" name="exceptions">
          <el-table :data="exceptions" v-loading="loading" size="default">
            <el-table-column prop="date" label="日期" width="110" />
            <el-table-column label="员工" min-width="140">
              <template #default="{ row }">
                <div class="emp-cell">
                  <el-avatar :size="26" :style="{ background: avatarColor(row.employeeId) }">{{ row.name.charAt(0) }}</el-avatar>
                  <div>
                    <div class="emp-name">{{ row.name }}</div>
                    <div class="emp-no">{{ row.employeeNo }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" min-width="120" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }"><StatusTag :status="row.status" /></template>
            </el-table-column>
            <el-table-column prop="checkIn" label="签到" width="90" />
            <el-table-column prop="checkOut" label="签退" width="90" />
            <el-table-column prop="remark" label="说明" min-width="120" />
            <el-table-column label="确认状态" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="row.confirmed ? 'success' : 'info'">{{ row.confirmed ? '已确认' : '待确认' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button v-if="!row.confirmed" link type="success" size="small" @click="confirm(row, true)">确认正常</el-button>
                <el-button v-else link type="warning" size="small" @click="confirm(row, false)">撤销</el-button>
              </template>
            </el-table-column>
            <template #empty><el-empty description="本月暂无异常记录" :image-size="80" /></template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="月度汇总" name="monthly">
          <el-table :data="monthlyRows" v-loading="loading" size="default">
            <el-table-column label="员工" min-width="150">
              <template #default="{ row }">
                <div class="emp-cell">
                  <el-avatar :size="26" :style="{ background: avatarColor(row.id) }">{{ row.name.charAt(0) }}</el-avatar>
                  <div>
                    <div class="emp-name">{{ row.name }}</div>
                    <div class="emp-no">{{ row.employeeNo }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="department" label="部门" min-width="120" />
            <el-table-column prop="totalDays" label="出勤天数" width="90" align="center" />
            <el-table-column prop="normal" label="正常" width="70" align="center" />
            <el-table-column prop="late" label="迟到" width="70" align="center" />
            <el-table-column prop="early" label="早退" width="70" align="center" />
            <el-table-column prop="absent" label="缺勤" width="70" align="center" />
            <el-table-column prop="leave" label="请假" width="70" align="center" />
            <el-table-column prop="overtime" label="加班(h)" width="90" align="center" />
            <el-table-column label="出勤率" width="110" align="center">
              <template #default="{ row }">
                <el-progress :percentage="row.rate" :stroke-width="8" :color="row.rate >= 95 ? '#23c17f' : row.rate >= 85 ? '#f59f00' : '#f53f3f'" :format="(p: number) => p + '%'" />
              </template>
            </el-table-column>
            <el-table-column label="异常" width="90" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.hasException" type="danger" size="small">有</el-tag>
                <el-tag v-else type="success" size="small">无</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 当日明细弹窗 -->
    <el-dialog v-model="dayDetailVisible" :title="dayDetail ? `${dayDetail.date} 出勤明细` : ''" width="480px">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="正常">{{ dayDetail?.normal }}</el-descriptions-item>
        <el-descriptions-item label="迟到">{{ dayDetail?.late }}</el-descriptions-item>
        <el-descriptions-item label="早退">{{ dayDetail?.early }}</el-descriptions-item>
        <el-descriptions-item label="缺勤">{{ dayDetail?.absent }}</el-descriptions-item>
        <el-descriptions-item label="请假">{{ dayDetail?.leave }}</el-descriptions-item>
        <el-descriptions-item label="外勤">{{ dayDetail?.field }}</el-descriptions-item>
        <el-descriptions-item label="出勤率">{{ dayDetail?.rate }}%</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import { attendanceApi } from '@/api'

/** 本地时区月份（toISOString 是 UTC，东八区月初会偏差一天） */
function localMonth(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
const month = ref(localMonth(new Date()))
const loading = ref(false)
const activeTab = ref('exceptions')

const stat = reactive({ normal: 0, late: 0, early: 0, absent: 0, attendanceRate: 0 })
const calendarDays = ref<any[]>([])
const exceptions = ref<any[]>([])
const monthlyRows = ref<any[]>([])
const shifts = ref<any[]>([])

const dayDetailVisible = ref(false)
const dayDetail = ref<any>(null)

const avatarColors = ['#2f6bf0', '#23c17f', '#f59f00', '#f53f3f', '#7b61ff', '#00b8d9', '#e864a8', '#2d7d46']
function avatarColor(id: number) {
  return avatarColors[id % avatarColors.length]
}

const pendingCount = computed(() => exceptions.value.filter((e) => !e.confirmed).length)

function heatLevel(rate: number) {
  if (!rate || rate <= 0) return 0 // 无出勤（周末/未排班）不高亮
  if (rate >= 95) return 4
  if (rate >= 90) return 3
  if (rate >= 85) return 2
  return 1
}

function cellTip(cell: any) {
  return `${cell.date} · 出勤${cell.normal} 迟到${cell.late} 早退${cell.early} 缺勤${cell.absent} 请假${cell.leave} 外勤${cell.field}`
}

function openDayDetail(cell: any) {
  if (cell.total === 0) return
  dayDetail.value = cell
  dayDetailVisible.value = true
}

async function confirm(row: any, confirmed: boolean) {
  await attendanceApi.confirm(row.id, confirmed)
  ElMessage.success(confirmed ? '已确认，异常归档' : '已撤销确认')
  loadExceptions()
}

function buildCalendar(cal: any) {
  const days = cal.days as any[]
  if (!days.length) {
    calendarDays.value = []
    return
  }
  const firstWeekday = new Date(days[0].date).getDay() // 0=周日
  const blanks = (firstWeekday + 6) % 7 // 对齐到周一
  const arr: any[] = []
  for (let i = 0; i < blanks; i++) arr.push(null)
  arr.push(...days)
  while (arr.length % 7 !== 0) arr.push(null)
  calendarDays.value = arr
  Object.assign(stat, cal.stat)
}

async function loadAll() {
  loading.value = true
  try {
    const [cal, exc, mon, sh] = await Promise.all([
      attendanceApi.calendar(month.value),
      attendanceApi.exceptions(month.value),
      attendanceApi.monthly(month.value),
      attendanceApi.shifts(),
    ])
    buildCalendar(cal)
    exceptions.value = exc
    monthlyRows.value = mon.rows
    shifts.value = sh
  } finally {
    loading.value = false
  }
}

async function loadExceptions() {
  exceptions.value = await attendanceApi.exceptions(month.value)
}

onMounted(loadAll)
</script>

<style scoped>
.table-card {
  padding: 8px 16px 16px;
  margin-top: 16px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
}

.cal-head {
  text-align: center;
  font-size: 12px;
  color: var(--hrms-text-3);
  padding: 4px 0;
}

.cal-blank {
  height: 58px;
}

.cal-cell {
  height: 58px;
  border-radius: 8px;
  border: 1px solid var(--hrms-border);
  padding: 5px 8px;
  cursor: pointer;
  transition: all 0.15s;
  position: relative;
}

.cal-cell:hover {
  transform: translateY(-2px);
  box-shadow: var(--hrms-shadow-hover);
}

.cal-cell--weekend {
  background: var(--hrms-bg);
}

.cal-cell--1 { background: rgba(47, 107, 240, 0.06); }
.cal-cell--2 { background: rgba(47, 107, 240, 0.18); }
.cal-cell--3 { background: rgba(47, 107, 240, 0.34); }
.cal-cell--4 { background: rgba(47, 107, 240, 0.55); }

.cal-cell--has-exception {
  border-color: var(--hrms-danger);
}

.cal-day {
  font-size: 13px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.cal-nums {
  font-size: 11px;
  display: flex;
  gap: 6px;
  margin-top: 4px;
  flex-wrap: wrap;
}

.cal-nums .ok { color: var(--hrms-success); }
.cal-nums .warn { color: var(--hrms-warning); }
.cal-nums .bad { color: var(--hrms-danger); }

.legend-box {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding: 8px 12px;
  background: var(--hrms-bg);
  border-radius: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--hrms-text-3);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot.ok { background: var(--hrms-primary); opacity: 0.5; }
.dot.warn { background: var(--hrms-warning); }
.dot.bad { background: var(--hrms-danger); }
.dot.leave { background: var(--hrms-text-3); }

.emp-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.emp-name {
  font-weight: 500;
  font-size: 13px;
  color: var(--hrms-text-1);
}

.emp-no {
  font-size: 11px;
  color: var(--hrms-text-3);
}
</style>
