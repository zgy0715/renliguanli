<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">绩效管理</h2>
        <p class="page-subtitle">OKR / KPI / 360 多维考核 · 评级分布一目了然</p>
      </div>
      <div class="flex gap-8">
        <el-select v-model="period" style="width: 160px" clearable placeholder="全部考核周期" @change="loadData">
          <el-option v-for="p in periods" :key="p" :label="p" :value="p" />
        </el-select>
      </div>
    </div>

    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">考核记录</div>
          <div class="mini-kpi-value">{{ rows.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">平均得分</div>
          <div class="mini-kpi-value">{{ avgScore }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">S级员工</div>
          <div class="mini-kpi-value accent-green">{{ countRating('S') }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">待面谈</div>
          <div class="mini-kpi-value accent-orange">{{ rows.filter((r) => !r.interviewNote).length }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="8">
        <div class="hrms-card">
          <div class="card-title-row"><span class="card-title">评级分布</span></div>
          <BaseChart :option="ratingOption" height="280px" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="hrms-card">
          <div class="card-title-row"><span class="card-title">考核模式占比</span></div>
          <BaseChart :option="modeOption" height="280px" />
        </div>
      </el-col>
      <el-col :span="8">
        <div class="hrms-card">
          <div class="card-title-row"><span class="card-title">得分区间分布</span></div>
          <BaseChart :option="scoreOption" height="280px" />
        </div>
      </el-col>
    </el-row>

    <div class="hrms-card table-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="period" label="考核周期" width="110" />
        <el-table-column prop="name" label="员工" min-width="120" />
        <el-table-column prop="mode" label="考核模式" width="90">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.mode }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.score || 0" :stroke-width="10" :color="scoreColor(row.score)" :show-text="false" />
            <span class="score-num">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评级" width="80" align="center">
          <template #default="{ row }"><StatusTag :status="row.rating" /></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }"><StatusTag :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="interviewNote" label="面谈记录" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.interviewNote" class="note-text">{{ row.interviewNote }}</span>
            <el-tag v-else size="small" type="warning">待面谈</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import StatusTag from '@/components/StatusTag.vue'
import { analyticsApi, performanceApi } from '@/api'

const period = ref('')
const periods = ['2026-Q1', '2026-Q2', '2026-H1', '2026年度']
const rows = ref<any[]>([])
const loading = ref(false)

const avgScore = computed(() => {
  if (!rows.value.length) return 0
  return (rows.value.reduce((s, r) => s + (r.score || 0), 0) / rows.value.length).toFixed(1)
})

function countRating(rating: string) {
  return rows.value.filter((r) => r.rating === rating).length
}

function scoreColor(score: number) {
  if (score >= 90) return '#23c17f'
  if (score >= 80) return '#2f6bf0'
  if (score >= 70) return '#f59f00'
  return '#f53f3f'
}

const ratingOption = computed(() => {
  const data = ['S', 'A', 'B', 'C', 'D'].map((r) => ({ name: r, value: countRating(r) }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}级: {c}人 ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: '68%',
        center: ['50%', '50%'],
        itemStyle: { borderRadius: 6 },
        label: { formatter: '{b} {c}人' },
        data,
        color: ['#23c17f', '#2f6bf0', '#f59f00', '#8a90a2', '#f53f3f'],
      },
    ],
  }
})

const modeOption = computed(() => {
  const modes = ['OKR', 'KPI', '360']
  const data = modes.map((m) => ({ name: m, value: rows.value.filter((r) => r.mode === m).length }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        itemStyle: { borderRadius: 6 },
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 'bold' } },
        data,
        color: ['#2f6bf0', '#7b61ff', '#00b8d9'],
      },
    ],
  }
})

const scoreOption = computed(() => {
  const buckets = [
    { name: '90+', value: rows.value.filter((r) => (r.score || 0) >= 90).length },
    { name: '80-89', value: rows.value.filter((r) => (r.score || 0) >= 80 && (r.score || 0) < 90).length },
    { name: '70-79', value: rows.value.filter((r) => (r.score || 0) >= 70 && (r.score || 0) < 80).length },
    { name: '<70', value: rows.value.filter((r) => (r.score || 0) < 70).length },
  ]
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 16, top: 24, bottom: 24 },
    xAxis: { type: 'category', data: buckets.map((b) => b.name) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: buckets.map((b) => b.value),
        barWidth: 40,
        itemStyle: { color: '#2f6bf0', borderRadius: [6, 6, 0, 0] },
        label: { show: true, position: 'top' },
      },
    ],
  }
})

async function loadData() {
  loading.value = true
  try {
    rows.value = await performanceApi.list(period.value || undefined)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.mini-kpi {
  padding: 16px 20px;
}

.mini-kpi-label {
  font-size: 13px;
  color: var(--hrms-text-3);
}

.mini-kpi-value {
  font-size: 26px;
  font-weight: 700;
  color: var(--hrms-text-1);
  margin-top: 4px;
}

.accent-green {
  color: var(--hrms-success) !important;
}

.accent-orange {
  color: var(--hrms-warning) !important;
}

.card-title-row {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--hrms-border);
  margin-bottom: 4px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.table-card {
  padding: 8px 16px 16px;
  margin-top: 16px;
}

.score-num {
  margin-left: 8px;
  font-size: 12px;
  color: var(--hrms-text-2);
}

.note-text {
  font-size: 13px;
  color: var(--hrms-text-2);
}
</style>
