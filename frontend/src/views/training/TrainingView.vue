<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">培训发展</h2>
        <p class="page-subtitle">课程库 · 学习记录 · 能力成长可视化</p>
      </div>
    </div>

    <el-row :gutter="16" class="mb-16">
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">课程总数</div>
          <div class="mini-kpi-value">{{ courses.length }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">累计学习人次</div>
          <div class="mini-kpi-value">{{ totalLearners }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">课程平均评分</div>
          <div class="mini-kpi-value accent-green">{{ avgRating }}</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="hrms-card mini-kpi">
          <div class="mini-kpi-label">完成率</div>
          <div class="mini-kpi-value accent-orange">{{ doneRate }}%</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="hrms-card">
          <div class="card-title-row">
            <span class="card-title">课程库</span>
          </div>
          <el-table :data="courses" size="default" :row-class-name="rowClass">
            <el-table-column label="课程" min-width="180">
              <template #default="{ row }">
                <div class="course-cell">
                  <div class="course-cover" :style="{ background: coverColor(row.id) }">{{ row.name.charAt(0) }}</div>
                  <div>
                    <div class="course-name">{{ row.name }}</div>
                    <div class="course-meta">{{ row.category }} · {{ row.teacher }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="hours" label="时长(h)" width="80" align="center" />
            <el-table-column label="形式" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.online ? 'primary' : 'warning'">{{ row.online ? '线上' : '线下' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="learnerCount" label="人次" width="80" align="center" />
            <el-table-column label="评分" width="100" align="center">
              <template #default="{ row }">
                <span class="star">★</span> {{ row.avgRating ?? '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === '上架' ? 'success' : 'info'">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>

      <el-col :span="10">
        <div class="hrms-card mb-16">
          <div class="card-title-row"><span class="card-title">课程分类分布</span></div>
          <BaseChart :option="categoryOption" height="240px" />
        </div>
        <div class="hrms-card">
          <div class="card-title-row"><span class="card-title">最新学习记录</span></div>
          <el-table :data="recentRecords" size="small">
            <el-table-column prop="courseName" label="课程" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === '已完成' ? 'success' : row.status === '进行中' ? 'warning' : 'info'">{{ recordStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="80" align="center">
              <template #default="{ row }">{{ row.progress || 0 }}%</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BaseChart from '@/components/BaseChart.vue'
import { trainingApi } from '@/api'

const courses = ref<any[]>([])
const records = ref<any[]>([])

const totalLearners = computed(() => courses.value.reduce((s, c) => s + (c.learnerCount || 0), 0))
const avgRating = computed(() => {
  const rated = courses.value.filter((c) => c.avgRating != null)
  if (!rated.length) return 0
  return (rated.reduce((s, c) => s + c.avgRating, 0) / rated.length).toFixed(1)
})
const doneRate = computed(() => {
  if (!records.value.length) return 0
  return Math.round((records.value.filter((r) => r.status === '已完成').length / records.value.length) * 100)
})
const recentRecords = computed(() => records.value.slice(0, 8))

function recordStatus(s: string) {
  return { '已完成': '已完成', '进行中': '学习中', '未开始': '未开始' }[s] || s
}

function rowClass({ row }: any) {
  return row.avgRating >= 4.5 ? 'hot-row' : ''
}

const coverColors = ['#2f6bf0', '#7b61ff', '#23c17f', '#00b8d9', '#f59f00', '#e864a8', '#f53f3f', '#2d7d46']
function coverColor(id: number) {
  return coverColors[id % coverColors.length]
}

const categoryOption = computed(() => {
  const cats = new Map<string, number>()
  courses.value.forEach((c) => cats.set(c.category, (cats.get(c.category) || 0) + 1))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c}门 ({d}%)' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        itemStyle: { borderRadius: 6 },
        label: { formatter: '{b} {c}' },
        data: [...cats.entries()].map(([name, value]) => ({ name, value })),
        color: ['#2f6bf0', '#7b61ff', '#23c17f', '#f59f00', '#e864a8', '#00b8d9'],
      },
    ],
  }
})

onMounted(async () => {
  const [cs, rs] = await Promise.all([trainingApi.courses(), trainingApi.records()])
  courses.value = cs
  records.value = rs
})
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

.hrms-card {
  padding: 16px;
}

.course-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-cover {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex-shrink: 0;
}

.course-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--hrms-text-1);
}

.course-meta {
  font-size: 11px;
  color: var(--hrms-text-3);
  margin-top: 2px;
}

.star {
  color: #f5a623;
}

:deep(.hot-row td) {
  background: rgba(245, 159, 0, 0.05);
}
</style>
