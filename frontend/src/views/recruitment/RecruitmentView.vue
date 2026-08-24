<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">招聘管理</h2>
        <p class="page-subtitle">候选人全流程跟进 · 各渠道效率透明</p>
      </div>
    </div>

    <!-- 需求列表 -->
    <el-row :gutter="16" class="mb-16">
      <el-col :span="6" v-for="job in jobs" :key="job.id">
        <div class="hrms-card job-card">
          <div class="job-head">
            <span class="job-title">{{ job.title }}</span>
            <el-tag size="small" :type="jobType(job.status)">{{ jobText(job.status) }}</el-tag>
          </div>
          <div class="job-progress">
            <el-progress :percentage="filledRate(job)" :stroke-width="10" :color="job.status === 'FILLED' ? '#23c17f' : '#2f6bf0'" />
          </div>
          <div class="job-meta">已入职 <b>{{ job.filled }}</b> / {{ job.headcount }} 人</div>
        </div>
      </el-col>
    </el-row>

    <!-- 候选人看板 -->
    <div class="hrms-card kanban-card">
      <div class="card-title-row">
        <span class="card-title">候选人漏斗</span>
        <el-radio-group v-model="channelFilter" size="small" @change="loadCandidates">
          <el-radio-button label="">全部渠道</el-radio-button>
          <el-radio-button v-for="c in channels" :key="c" :label="c">{{ c }}</el-radio-button>
        </el-radio-group>
      </div>
      <div class="kanban">
        <div class="kanban-col" v-for="col in kanbanColumns" :key="col.status">
          <div class="kanban-head">
            <span class="kanban-title">{{ col.label }}</span>
            <span class="kanban-count">{{ grouped(col.status).length }}</span>
          </div>
          <div
            class="cand-card"
            v-for="c in grouped(col.status)"
            :key="c.id"
            @click="selectCandidate(c)"
            :class="{ 'cand-card--active': selected?.id === c.id }"
          >
            <div class="cand-top">
              <div class="cand-avatar" :style="{ background: avatarColor(c.id) }">{{ c.name.charAt(0) }}</div>
              <div class="cand-info">
                <div class="cand-name">{{ c.name }}</div>
                <div class="cand-pos">{{ c.position }}</div>
              </div>
              <el-rate :model-value="Math.round((c.resumeScore || 0) / 20)" :max="5" disabled size="small" />
            </div>
            <div class="cand-meta">{{ c.education }} · {{ c.workYears }}年经验 · {{ c.channel }}</div>
            <div class="cand-actions">
              <el-button v-for="a in nextActions[col.status] || []" :key="a.status" link size="small" :type="a.type" @click.stop="moveCandidate(c, a.status)">
                {{ a.label }}
              </el-button>
            </div>
          </div>
          <div class="cand-empty" v-if="!grouped(col.status).length">暂无候选人</div>
        </div>
      </div>
    </div>

    <!-- 候选人详情 -->
    <el-drawer v-model="detailVisible" size="420px" :title="selected?.name || '候选人'">
      <template v-if="selected">
        <div class="cand-detail-head">
          <el-avatar :size="56" :style="{ background: avatarColor(selected.id) }">{{ selected.name.charAt(0) }}</el-avatar>
          <div>
            <div class="cand-detail-name">{{ selected.name }}</div>
            <div class="cand-detail-pos">{{ selected.position }}</div>
          </div>
        </div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="手机号">{{ selected.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ selected.email }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ selected.education }}</el-descriptions-item>
          <el-descriptions-item label="工作年限">{{ selected.workYears }} 年</el-descriptions-item>
          <el-descriptions-item label="招聘渠道">{{ selected.channel }}</el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ selected.applyDate }}</el-descriptions-item>
          <el-descriptions-item label="简历评分">{{ selected.resumeScore }} 分</el-descriptions-item>
          <el-descriptions-item label="备注">{{ selected.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { recruitmentApi } from '@/api'

const jobs = ref<any[]>([])
const candidates = ref<any[]>([])
const allCandidates = ref<any[]>([])
const channelFilter = ref('')
const selected = ref<any>(null)
const detailVisible = ref(false)

const kanbanColumns = [
  { status: '简历筛选', label: '简历筛选', color: '#2f6bf0' },
  { status: '初试', label: '初试' },
  { status: '复试', label: '复试' },
  { status: 'HR面', label: 'HR面' },
  { status: 'Offer', label: 'Offer' },
  { status: '入职', label: '已入职' },
  { status: '淘汰', label: '已淘汰' },
]

const nextActions: Record<string, { status: string; label: string; type: string }[]> = {
  简历筛选: [
    { status: '初试', label: '进入初试', type: 'primary' },
    { status: '淘汰', label: '淘汰', type: 'danger' },
  ],
  初试: [
    { status: '复试', label: '进入复试', type: 'primary' },
    { status: '淘汰', label: '淘汰', type: 'danger' },
  ],
  复试: [
    { status: 'HR面', label: 'HR面', type: 'primary' },
    { status: '淘汰', label: '淘汰', type: 'danger' },
  ],
  HR面: [
    { status: 'Offer', label: '发Offer', type: 'success' },
    { status: '淘汰', label: '淘汰', type: 'danger' },
  ],
  Offer: [
    { status: '入职', label: '确认入职', type: 'success' },
    { status: '淘汰', label: '未入职', type: 'danger' },
  ],
}

const channels = computed(() => [...new Set(allCandidates.value.map((c) => c.channel).filter(Boolean))])

function jobText(s: string) {
  return { OPEN: '招聘中', CLOSED: '已关闭', FILLED: '已满编' }[s] || s
}
function jobType(s: string) {
  return ({ OPEN: 'primary', CLOSED: 'info', FILLED: 'success' } as Record<string, any>)[s] || 'info'
}
function filledRate(job: any) {
  return Math.round(((job.filled || 0) / Math.max(1, job.headcount)) * 100)
}
function grouped(status: string) {
  return candidates.value.filter((c) => c.status === status)
}
function avatarColor(id: number) {
  const colors = ['#2f6bf0', '#23c17f', '#f59f00', '#7b61ff', '#f53f3f', '#00b8d9', '#e864a8']
  return colors[id % colors.length]
}
function selectCandidate(c: any) {
  selected.value = c
  detailVisible.value = true
}
async function moveCandidate(c: any, status: string) {
  await recruitmentApi.updateStatus(c.id, status)
  c.status = status
  ElMessage.success(`已推进至「${status}」`)
}
async function loadCandidates() {
  candidates.value = await recruitmentApi.candidates({ channel: channelFilter.value || undefined })
}

onMounted(async () => {
  const [js, cs] = await Promise.all([recruitmentApi.jobs(), recruitmentApi.candidates()])
  jobs.value = js
  allCandidates.value = cs
  candidates.value = cs
})
</script>

<style scoped>
.job-card {
  padding: 16px 18px;
}

.job-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.job-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.job-progress {
  margin: 12px 0 6px;
}

.job-meta {
  font-size: 12px;
  color: var(--hrms-text-3);
}

.kanban-card {
  padding: 16px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--hrms-border);
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.kanban {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
  overflow-x: auto;
}

.kanban-col {
  background: var(--hrms-bg);
  border-radius: 10px;
  padding: 10px;
  min-width: 140px;
}

.kanban-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px 10px;
}

.kanban-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.kanban-count {
  font-size: 12px;
  color: var(--hrms-text-3);
  background: var(--hrms-border);
  border-radius: 10px;
  padding: 1px 8px;
}

.cand-card {
  background: var(--hrms-card);
  border: 1px solid var(--hrms-border);
  border-radius: 8px;
  padding: 10px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.cand-card:hover {
  border-color: var(--hrms-primary);
  transform: translateY(-1px);
  box-shadow: var(--hrms-shadow-hover);
}

.cand-card--active {
  border-color: var(--hrms-primary);
  box-shadow: 0 0 0 2px rgba(47, 107, 240, 0.15);
}

.cand-top {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cand-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.cand-info {
  flex: 1;
  min-width: 0;
}

.cand-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.cand-pos {
  font-size: 11px;
  color: var(--hrms-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cand-meta {
  font-size: 11px;
  color: var(--hrms-text-3);
  margin-top: 6px;
}

.cand-actions {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
}

.cand-empty {
  text-align: center;
  font-size: 12px;
  color: var(--hrms-text-4);
  padding: 16px 0;
}

.cand-detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}

.cand-detail-name {
  font-size: 18px;
  font-weight: 700;
}

.cand-detail-pos {
  font-size: 13px;
  color: var(--hrms-text-3);
  margin-top: 4px;
}
</style>
