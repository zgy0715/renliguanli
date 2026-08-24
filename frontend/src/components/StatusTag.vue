<template>
  <el-tag size="small" :type="tagType" effect="light" round>
    {{ text }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status: string }>()

const MAP: Record<string, { text: string; type: 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  // 员工状态
  ACTIVE: { text: '在职', type: 'success' },
  PROBATION: { text: '试用', type: 'warning' },
  LEFT: { text: '离职', type: 'info' },
  SUSPEND: { text: '停职', type: 'danger' },
  // 考勤
  NORMAL: { text: '正常', type: 'success' },
  LATE: { text: '迟到', type: 'warning' },
  EARLY: { text: '早退', type: 'warning' },
  ABSENT: { text: '缺勤', type: 'danger' },
  LEAVE: { text: '请假', type: 'info' },
  FIELD: { text: '外勤', type: 'primary' },
  // 薪酬
  DRAFT: { text: '草稿', type: 'info' },
  CALCULATED: { text: '已核算', type: 'primary' },
  CONFIRMED: { text: '已确认', type: 'warning' },
  PAID: { text: '已发放', type: 'success' },
  // 审批
  PENDING: { text: '待审批', type: 'warning' },
  APPROVED: { text: '已通过', type: 'success' },
  REJECTED: { text: '已驳回', type: 'danger' },
  // 绩效
  S: { text: 'S', type: 'success' },
  A: { text: 'A', type: 'primary' },
  B: { text: 'B', type: 'warning' },
  C: { text: 'C', type: 'info' },
  D: { text: 'D', type: 'danger' },
}

const tagType = computed(() => MAP[props.status]?.type || 'info')
const text = computed(() => MAP[props.status]?.text || props.status)
</script>
