<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">薪酬核算</h2>
        <p class="page-subtitle">复杂薪酬规则 · 一键批量核算 · 成本结构透明</p>
      </div>
      <div class="flex gap-8">
        <el-date-picker v-model="month" type="month" value-format="YYYY-MM" :clearable="false" @change="loadAll" />
        <el-button type="primary" :icon="MagicStick" :loading="calculating" @click="runCalculate">一键核算</el-button>
      </div>
    </div>

    <!-- KPI -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="4"><KpiCard label="应发总额" :value="fmtMoney(summary.totalGross)" unit="元" icon="Money" accent="#2f6bf0" /></el-col>
      <el-col :span="4"><KpiCard label="实发总额" :value="fmtMoney(summary.totalNet)" unit="元" icon="Wallet" accent="#23c17f" /></el-col>
      <el-col :span="4"><KpiCard label="五险一金" :value="fmtMoney(summary.totalSocial)" unit="元" icon="Box" accent="#f59f00" /></el-col>
      <el-col :span="4"><KpiCard label="代扣个税" :value="fmtMoney(summary.totalTax)" unit="元" icon="Document" accent="#f53f3f" /></el-col>
      <el-col :span="4"><KpiCard label="人均实发" :value="fmtMoney(summary.avgNet)" unit="元" icon="TrendCharts" accent="#7b61ff" /></el-col>
      <el-col :span="4"><KpiCard label="核算人数" :value="summary.employeeCount" unit="人" icon="UserFilled" accent="#00b8d9" /></el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 成本构成 -->
      <el-col :span="10">
        <ChartCard title="成本构成" subtitle="应发项占比">
          <BaseChart :option="costOption" height="300px" />
        </ChartCard>
      </el-col>
      <!-- 部门成本 -->
      <el-col :span="14">
        <ChartCard title="部门人力成本" subtitle="应发总额 Top 10">
          <BaseChart :option="deptCostOption" height="300px" />
        </ChartCard>
      </el-col>
    </el-row>

    <!-- 核算表 -->
    <div class="hrms-card table-card">
      <div class="table-toolbar">
        <el-radio-group v-model="statusFilter" size="small" @change="applyFilter">
          <el-radio-button label="">全部 {{ listRows.length }}</el-radio-button>
          <el-radio-button label="DRAFT">草稿 {{ statusCount.DRAFT }}</el-radio-button>
          <el-radio-button label="CALCULATED">已核算 {{ statusCount.CALCULATED }}</el-radio-button>
          <el-radio-button label="CONFIRMED">已确认 {{ statusCount.CONFIRMED }}</el-radio-button>
          <el-radio-button label="PAID">已发放 {{ statusCount.PAID }}</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="filteredRows" v-loading="loading" stripe>
        <el-table-column label="员工" min-width="150">
          <template #default="{ row }">
            <div class="emp-cell">
              <el-avatar :size="28" :style="{ background: avatarColor(row.employeeId) }">{{ row.name.charAt(0) }}</el-avatar>
              <div>
                <div class="emp-name">{{ row.name }}</div>
                <div class="emp-no">{{ row.employeeNo }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" min-width="110" />
        <el-table-column prop="position" label="职位" min-width="110" />
        <el-table-column prop="baseSalary" label="基本工资" width="100" align="right">
          <template #default="{ row }">{{ money(row.baseSalary) }}</template>
        </el-table-column>
        <el-table-column prop="performanceSalary" label="绩效工资" width="100" align="right">
          <template #default="{ row }">{{ money(row.performanceSalary) }}</template>
        </el-table-column>
        <el-table-column prop="grossAmount" label="应发" width="100" align="right">
          <template #default="{ row }"><span class="money-primary">{{ money(row.grossAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="扣减" width="90" align="right">
          <template #default="{ row }">{{ money(row.social + row.tax) }}</template>
        </el-table-column>
        <el-table-column prop="netAmount" label="实发" width="100" align="right">
          <template #default="{ row }"><span class="money-net">{{ money(row.netAmount) }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openPayslip(row)">工资条</el-button>
            <el-button v-if="row.status === 'CALCULATED'" link type="success" size="small" @click="setStatus(row, 'CONFIRMED')">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="warning" size="small" @click="setStatus(row, 'PAID')">发放</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="该月暂无薪酬记录，请点击右上角「一键核算」" :image-size="90" /></template>
      </el-table>
    </div>

    <!-- 工资条抽屉 -->
    <el-drawer v-model="payslipVisible" size="440px" :title="`工资条 · ${month}月`">
      <template v-if="payslip">
        <div class="slip-head">
          <div class="slip-emp">{{ payslip.employee.name }}</div>
          <div class="slip-meta">{{ payslip.employee.employeeNo }} · {{ payslip.employee.department }} · {{ payslip.employee.position }}</div>
          <StatusTag :status="payslip.record.status" />
        </div>
        <div class="slip-section">
          <div class="slip-title">应发项</div>
          <div class="slip-row"><span>基本工资</span><span>{{ money(payslip.record.baseSalary) }}</span></div>
          <div class="slip-row"><span>岗位工资</span><span>{{ money(payslip.record.positionSalary) }}</span></div>
          <div class="slip-row"><span>绩效工资</span><span>{{ money(payslip.record.performanceSalary) }}</span></div>
          <div class="slip-row"><span>技能补贴</span><span>{{ money(payslip.record.allowance) }}</span></div>
          <div class="slip-row"><span>加班费</span><span>{{ money(payslip.record.overtimePay) }}</span></div>
          <div class="slip-row"><span>奖金</span><span>{{ money(payslip.record.bonus) }}</span></div>
          <div class="slip-row slip-total"><span>应发合计</span><span>{{ money(payslip.record.grossAmount) }}</span></div>
        </div>
        <div class="slip-section">
          <div class="slip-title">扣减项</div>
          <div class="slip-row"><span>五险（养老）</span><span>-{{ money(payslip.record.socialPension) }}</span></div>
          <div class="slip-row"><span>五险（医疗）</span><span>-{{ money(payslip.record.socialMedical) }}</span></div>
          <div class="slip-row"><span>五险（失业）</span><span>-{{ money(payslip.record.socialUnemployment) }}</span></div>
          <div class="slip-row"><span>公积金</span><span>-{{ money(payslip.record.housingFund) }}</span></div>
          <div class="slip-row"><span>个人所得税</span><span>-{{ money(payslip.record.tax) }}</span></div>
          <div class="slip-row"><span>其他扣款</span><span>-{{ money(payslip.record.deduction) }}</span></div>
          <div class="slip-row slip-total"><span>扣减合计</span><span>{{ money(payslip.record.deductionTotal) }}</span></div>
        </div>
        <div class="slip-net">
          <span>实发工资</span>
          <span class="slip-net-value">¥{{ money(payslip.record.netAmount) }}</span>
        </div>
        <div class="slip-foot">* 本工资条由系统自动生成，如有疑问请联系 HR</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import KpiCard from '@/components/KpiCard.vue'
import ChartCard from '@/components/ChartCard.vue'
import BaseChart from '@/components/BaseChart.vue'
import StatusTag from '@/components/StatusTag.vue'
import { payrollApi } from '@/api'

const month = ref(new Date().toISOString().slice(0, 7))
const loading = ref(false)
const calculating = ref(false)
const statusFilter = ref('')

const listRows = ref<any[]>([])
const filteredRows = ref<any[]>([])
const statusCount = reactive({ DRAFT: 0, CALCULATED: 0, CONFIRMED: 0, PAID: 0 })
const summary = reactive({ totalGross: 0, totalNet: 0, totalSocial: 0, totalTax: 0, avgNet: 0, employeeCount: 0 })
const costBreakdown = ref<any>({})
const deptCost = ref<any[]>([])

const payslipVisible = ref(false)
const payslip = ref<any>(null)

const avatarColors = ['#2f6bf0', '#23c17f', '#f59f00', '#f53f3f', '#7b61ff', '#00b8d9', '#e864a8', '#2d7d46']
function avatarColor(id: number) {
  return avatarColors[id % avatarColors.length]
}

function money(v: number | null | undefined) {
  return (v ?? 0).toLocaleString()
}

function fmtMoney(v: number) {
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
}

const costOption = computed(() => {
  const items = [
    { name: '基本工资', value: costBreakdown.value.base },
    { name: '岗位工资', value: costBreakdown.value.position },
    { name: '绩效工资', value: costBreakdown.value.performance },
    { name: '补贴', value: costBreakdown.value.allowance },
    { name: '加班费', value: costBreakdown.value.overtime },
    { name: '奖金', value: costBreakdown.value.bonus },
  ]
  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '66%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
        data: items,
        color: ['#2f6bf0', '#4d7ff5', '#7b61ff', '#00b8d9', '#f59f00', '#23c17f'],
      },
    ],
  }
})

const deptCostOption = computed(() => ({
  tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].name}<br/>${p[0].marker}¥${fmtMoney(p[0].value)}` },
  grid: { left: 90, right: 30, top: 8, bottom: 20 },
  xAxis: { type: 'value', axisLabel: { formatter: (v: number) => (v >= 10000 ? v / 10000 + '万' : v) } },
  yAxis: { type: 'category', data: deptCost.value.map((i) => i.name).reverse(), axisTick: { show: false } },
  series: [
    {
      type: 'bar',
      data: deptCost.value.map((i) => i.value).reverse(),
      barWidth: 16,
      itemStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 1, y2: 0,
          colorStops: [{ offset: 0, color: '#5b8cf5' }, { offset: 1, color: '#2f6bf0' }],
        },
        borderRadius: [0, 6, 6, 0],
      },
      label: { show: true, position: 'right', formatter: (p: any) => (p.value >= 10000 ? (p.value / 10000).toFixed(1) + '万' : p.value) },
    },
  ],
}))

function applyFilter() {
  if (!statusFilter.value) filteredRows.value = listRows.value
  else filteredRows.value = listRows.value.filter((r) => r.status === statusFilter.value)
}

async function loadAll() {
  loading.value = true
  try {
    const [list, summ, dept] = await Promise.all([
      payrollApi.list(month.value),
      payrollApi.summary(month.value),
      payrollApi.deptCost(month.value),
    ])
    listRows.value = list.rows
    applyFilter()
    Object.assign(summary, {
      totalGross: summ.totalGross || 0,
      totalNet: summ.totalNet || 0,
      totalSocial: summ.totalSocial || 0,
      totalTax: summ.totalTax || 0,
      avgNet: summ.avgNet || 0,
      employeeCount: summ.employeeCount || 0,
    })
    Object.assign(statusCount, summ.statusCount || {})
    costBreakdown.value = summ.costBreakdown || {}
    deptCost.value = dept.items || []
  } finally {
    loading.value = false
  }
}

async function runCalculate() {
  await ElMessageBox.confirm(`将对 ${month.value} 月执行薪酬核算，覆盖该月已有记录，确认继续？`, '一键核算', { type: 'warning' })
  calculating.value = true
  try {
    const count = await payrollApi.calculate(month.value)
    ElMessage.success(`核算完成，共 ${count} 人`)
    loadAll()
  } finally {
    calculating.value = false
  }
}

async function setStatus(row: any, status: string) {
  // 调后端持久化状态（此前仅本地改动，刷新即丢），成功后重载全量数据
  try {
    await payrollApi.updateStatus(row.id, status)
    ElMessage.success(status === 'CONFIRMED' ? '已确认' : '已发放')
    loadAll()
  } catch {
    /* 拦截器已统一提示 */
  }
}

async function openPayslip(row: any) {
  const res = await payrollApi.payslip(row.employeeId, month.value)
  payslip.value = res
  payslipVisible.value = true
}

onMounted(loadAll)
</script>

<style scoped>
.table-card {
  padding: 8px 16px 16px;
  margin-top: 16px;
}

.table-toolbar {
  padding: 10px 0;
}

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

.money-primary {
  color: var(--hrms-primary);
  font-weight: 600;
}

.money-net {
  color: var(--hrms-success);
  font-weight: 600;
}

.slip-head {
  text-align: center;
  padding-bottom: 16px;
  border-bottom: 1px dashed var(--hrms-border);
  margin-bottom: 8px;
}

.slip-emp {
  font-size: 20px;
  font-weight: 700;
}

.slip-meta {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin: 6px 0;
}

.slip-section {
  margin-bottom: 12px;
}

.slip-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--hrms-primary);
  margin-bottom: 6px;
  padding-left: 8px;
  border-left: 3px solid var(--hrms-primary);
}

.slip-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 13px;
  color: var(--hrms-text-2);
}

.slip-total {
  border-top: 1px dashed var(--hrms-border);
  font-weight: 600;
  color: var(--hrms-text-1);
}

.slip-net {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #2f6bf0, #4d8bff);
  color: #fff;
  padding: 14px 20px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
}

.slip-net-value {
  font-size: 24px;
  font-weight: 800;
}

.slip-foot {
  margin-top: 12px;
  font-size: 12px;
  color: var(--hrms-text-3);
  text-align: center;
}
</style>
