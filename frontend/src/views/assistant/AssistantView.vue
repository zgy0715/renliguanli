<template>
  <div class="assistant-page">
    <!-- 顶部：助手标识 + 会话操作 -->
    <div class="assistant-header">
      <div class="assistant-title">
        <div class="assistant-avatar"><el-icon :size="20"><MagicStick /></el-icon></div>
        <div>
          <div class="assistant-name">HR 智能助手</div>
          <div class="assistant-sub">自然语言问数 · 数据实时 · 支持多轮追问</div>
        </div>
      </div>
      <div class="assistant-actions">
        <el-tooltip content="刷新会话" placement="top">
          <el-button size="small" circle :icon="RefreshLeft" @click="onRestart" />
        </el-tooltip>
        <el-tooltip content="清空对话" placement="top">
          <el-button size="small" circle :icon="Delete" @click="onClear" />
        </el-tooltip>
      </div>
    </div>

    <!-- 消息区 -->
    <div ref="scrollRef" class="assistant-messages">
      <!-- 欢迎语 -->
      <div v-if="messages.length === 0 && !loading" class="assistant-welcome">
        <div class="welcome-avatar"><el-icon :size="30"><MagicStick /></el-icon></div>
        <div class="welcome-title">你好，我是 HR 智能助手 👋</div>
        <div class="welcome-desc">用中文直接提问，我帮你查数据、出图表、给结论。试试下面的问题：</div>
        <div class="welcome-starters">
          <button v-for="s in starters" :key="s" class="starter-chip" @click="send(s)">{{ s }}</button>
        </div>
      </div>

      <!-- 对话气泡 -->
      <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role === 'user' ? 'msg-user' : 'msg-assistant'">
        <div v-if="m.role === 'assistant'" class="msg-avatar"><el-icon :size="16"><MagicStick /></el-icon></div>
        <div class="msg-body">
          <div class="bubble" :class="m.role">
            <div class="bubble-text" v-html="renderText(m.content)" />
            <div v-if="m.reply?.chart" class="bubble-chart hrms-card">
              <BaseChart :option="toEchartsOption(m.reply.chart)" :height="chartHeight(m.reply.chart)" />
            </div>
            <div v-if="m.reply?.table" class="bubble-table">
              <table>
                <thead>
                  <tr>
                    <th v-for="c in m.reply.table.columns" :key="c.key">{{ c.label }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, ri) in m.reply.table.rows" :key="ri">
                    <td v-for="c in m.reply.table.columns" :key="c.key">{{ row[c.key] }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
          <!-- 推荐追问 -->
          <div v-if="m.reply?.suggestions?.length" class="suggestion-chips">
            <button v-for="s in m.reply.suggestions" :key="s" class="suggestion-chip" @click="send(s)">{{ s }}</button>
          </div>
        </div>
      </div>

      <!-- 输入中动画 -->
      <div v-if="loading" class="msg-row msg-assistant">
        <div class="msg-avatar"><el-icon :size="16"><MagicStick /></el-icon></div>
        <div class="msg-body">
          <div class="bubble typing-bubble">
            <span class="dot" /><span class="dot" /><span class="dot" />
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="assistant-input">
      <div class="input-box">
        <el-input
          v-model="question"
          placeholder="输入你的问题，例如：各部门人数？平均薪资多少？招聘漏斗？"
          :disabled="loading"
          clearable
          @keyup.enter="sendCurrent"
        >
          <template #prefix><el-icon><ChatLineRound /></el-icon></template>
        </el-input>
        <el-button type="primary" :loading="loading" @click="sendCurrent">发送</el-button>
      </div>
      <div class="input-hint">支持「在职多少人 / 各部门人数 / 平均薪资 / 招聘漏斗 / 年龄结构 / 人才盘点 / 我的考勤」等提问，数据实时来自系统</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ChatLineRound, Delete, MagicStick, RefreshLeft } from '@element-plus/icons-vue'
import BaseChart from '@/components/BaseChart.vue'
import { assistantApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { AssistantChart, ChatMessage } from '@/types'

const messages = ref<ChatMessage[]>([])
const question = ref('')
const loading = ref(false)
const scrollRef = ref<HTMLElement>()

/** 快捷提问按角色区分：员工仅展示其有权访问的公司公开信息与个人自助问题 */
const MANAGEMENT_STARTERS = [
  '现在在职多少人？',
  '各部门人数？',
  '平均薪资多少？',
  '招聘漏斗怎样？',
  '年龄结构分布？',
  '人才盘点九宫格？',
]
const EMPLOYEE_STARTERS = [
  '现在在职多少人？',
  '我的考勤怎样？',
  '我的工资条',
  '我的绩效',
  '有哪些培训课程？',
]
const authStore = useAuthStore()
const starters = computed(() =>
  ['ADMIN', 'HR', 'MANAGER'].includes(authStore.user?.role || '') ? MANAGEMENT_STARTERS : EMPLOYEE_STARTERS,
)

/** 把后端返回的图表结构转成 ECharts option */
function toEchartsOption(chart: AssistantChart): Record<string, unknown> {
  const grid = { left: 8, right: 16, top: 34, bottom: 8, containLabel: true }
  switch (chart.type) {
    case 'bar':
      return {
        tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
        legend: { top: 0 },
        grid,
        xAxis: { type: 'category', data: chart.categories },
        yAxis: { type: 'value' },
        series: (chart.series || []).map((s) => ({ ...s, barMaxWidth: 36 })),
      }
    case 'line':
      return {
        tooltip: { trigger: 'axis' },
        legend: { top: 0 },
        grid,
        xAxis: { type: 'category', boundaryGap: false, data: chart.categories },
        yAxis: { type: 'value' },
        series: (chart.series || []).map((s) => ({ ...s, smooth: true, symbolSize: 6 })),
      }
    case 'pie':
      return {
        tooltip: { trigger: 'item', formatter: '{b}: {c}人' },
        legend: { bottom: 0, type: 'scroll' },
        series: [{ type: 'pie', radius: ['38%', '66%'], center: ['50%', '50%'], label: { formatter: '{b}: {c}人' }, data: chart.items || [] }],
      }
    case 'funnel':
      return {
        tooltip: { trigger: 'item', formatter: '{b}: {c}人' },
        series: [{ type: 'funnel', left: '8%', right: '8%', top: 20, bottom: 8, minSize: '20%', maxSize: '100%', label: { position: 'inside', formatter: '{b} {c}' }, data: chart.items || [] }],
      }
    case 'radar':
      return {
        tooltip: {},
        radar: { indicator: (chart.indicators || []).map((name) => ({ name, max: 100 })), radius: '62%', splitNumber: 4 },
        series: [{ type: 'radar', symbolSize: 5, areaStyle: { opacity: 0.18 }, data: [{ name: '公司', value: chart.values || [] }] }],
      }
    default:
      return {}
  }
}

function chartHeight(chart: AssistantChart): string {
  return chart.type === 'radar' ? '260px' : '280px'
}

/** 富文本：先转义 HTML 防注入，再应用 **加粗** 与换行 */
function renderText(text: string) {
  const esc = (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  const bold = esc.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  return bold.replace(/\n/g, '<br/>')
}

function scrollBottom() {
  nextTick(() => {
    if (scrollRef.value) scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  })
}

async function send(text: string) {
  const q = (text || '').trim()
  if (!q || loading.value) return
  messages.value.push({ role: 'user', content: q })
  question.value = ''
  scrollBottom()
  loading.value = true
  try {
    const res = await assistantApi.ask(q)
    messages.value.push({ role: 'assistant', content: res.reply, reply: res })
  } catch {
    messages.value.push({ role: 'assistant', content: '请求失败，请确认后端已启动后重试。' })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

function sendCurrent() {
  send(question.value)
}

async function onRestart() {
  messages.value = []
  await loadHistory()
}

async function onClear() {
  try {
    await assistantApi.clearHistory()
  } catch { /* ignore */ }
  messages.value = []
  question.value = ''
}

async function loadHistory() {
  try {
    const list = await assistantApi.history()
    messages.value = (list || []).map((m) => ({
      role: m.role,
      content: m.content,
      time: m.time,
      reply: m.reply,
    }))
  } catch { /* ignore */ }
  scrollBottom()
}

onMounted(loadHistory)
</script>

<style scoped>
.assistant-page {
  height: calc(100vh - 56px);
  display: flex;
  flex-direction: column;
  background: var(--hrms-bg);
}

/* ===== 头部 ===== */
.assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: var(--hrms-card);
  border-bottom: 1px solid var(--hrms-border);
}
.assistant-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.assistant-avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--hrms-primary), var(--hrms-primary-dark));
  box-shadow: 0 4px 12px rgba(47, 107, 240, 0.35);
}
.assistant-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--hrms-text-1);
}
.assistant-sub {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin-top: 2px;
}
.assistant-actions {
  display: flex;
  gap: 8px;
}

/* ===== 消息区 ===== */
.assistant-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px 24px 12px;
}

/* 欢迎语 */
.assistant-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 0 24px;
}
.welcome-avatar {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--hrms-primary), var(--hrms-primary-dark));
  box-shadow: 0 8px 24px rgba(47, 107, 240, 0.4);
  margin-bottom: 16px;
}
.welcome-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--hrms-text-1);
}
.welcome-desc {
  font-size: 13px;
  color: var(--hrms-text-3);
  margin-top: 8px;
}
.welcome-starters {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  margin-top: 20px;
  max-width: 640px;
}
.starter-chip {
  padding: 8px 16px;
  font-size: 13px;
  color: var(--hrms-primary);
  background: var(--hrms-primary-bg);
  border: 1px solid transparent;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}
.starter-chip:hover {
  border-color: var(--hrms-primary);
  transform: translateY(-1px);
}

/* 气泡行 */
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.msg-user {
  justify-content: flex-end;
}
.msg-avatar {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, var(--hrms-primary), var(--hrms-primary-dark));
}
.msg-body {
  max-width: min(720px, 82%);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.msg-user .msg-body {
  align-items: flex-end;
}
.bubble {
  padding: 12px 16px;
  border-radius: var(--hrms-radius);
  line-height: 1.7;
  font-size: 14px;
  color: var(--hrms-text-1);
}
.bubble.assistant {
  background: var(--hrms-card);
  border: 1px solid var(--hrms-border);
  border-top-left-radius: 4px;
  box-shadow: var(--hrms-shadow);
}
.bubble.user {
  background: var(--hrms-primary);
  color: #fff;
  border-top-right-radius: 4px;
}
.bubble-text :deep(strong) {
  color: var(--hrms-primary);
  font-weight: 700;
}
.bubble.user .bubble-text :deep(strong) {
  color: #fff;
  font-weight: 700;
}

/* 气泡内图表 / 表格 */
.bubble-chart {
  margin-top: 12px;
  padding: 10px;
  background: var(--hrms-bg-subtle);
  border: 1px solid var(--hrms-border);
}
.bubble-table {
  margin-top: 10px;
  overflow-x: auto;
}
.bubble-table table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.bubble-table th,
.bubble-table td {
  padding: 7px 10px;
  border: 1px solid var(--hrms-border);
  text-align: center;
}
.bubble-table th {
  background: var(--hrms-primary-bg);
  color: var(--hrms-text-1);
  font-weight: 600;
}
.bubble-table td {
  color: var(--hrms-text-2);
}

/* 推荐追问 */
.suggestion-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.suggestion-chip {
  padding: 5px 12px;
  font-size: 12px;
  color: var(--hrms-primary);
  background: var(--hrms-primary-bg);
  border: 1px solid var(--hrms-border);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s;
}
.suggestion-chip:hover {
  border-color: var(--hrms-primary);
}

/* 输入中动画 */
.typing-bubble {
  display: flex;
  gap: 5px;
  align-items: center;
}
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--hrms-text-3);
  animation: typing 1.2s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: 0.15s; }
.dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-4px); opacity: 1; }
}

/* ===== 输入区 ===== */
.assistant-input {
  padding: 12px 24px 16px;
  background: var(--hrms-card);
  border-top: 1px solid var(--hrms-border);
}
.input-box {
  display: flex;
  gap: 10px;
}
.input-hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--hrms-text-3);
  text-align: center;
}
</style>
