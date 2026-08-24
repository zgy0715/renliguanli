/** 全局类型定义 */

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  list: T[]
}

export interface UserInfo {
  id: number
  username: string
  displayName: string
  role: string
  employeeId: number | null
  avatarColor: number
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  displayName: string
  role: string
  employeeId: number | null
  expire: number
}

export interface EmployeeVO {
  id: number
  employeeNo: string
  name: string
  gender: string
  birthDate: string | null
  phone: string
  email: string
  departmentId: number | null
  departmentName: string | null
  positionId: number | null
  positionName: string | null
  level: string
  hireDate: string | null
  regularDate: string | null
  status: string
  education: string
  baseSalary: number
  tags: string
  gridCell: string | null
  managerId: number | null
  managerName: string | null
  profileScore: number | null
  avatar: string | null
}

export interface MemberNode {
  id: number
  employeeNo: string
  name: string
  positionName: string | null
  level: string
}

export interface DeptNode {
  id: number
  name: string
  code: string
  parentId: number | null
  leaderId: number | null
  count: number
  directCount: number
  members?: MemberNode[]
  children: DeptNode[]
}

export interface KpiSummary {
  totalEmployees: number
  probation: number
  newHires: number
  resignations: number
  attritionRate: number
  avgSalary: number
  attendanceRate: number
  pendingLeave: number
  payrollMonth: string
}

export interface TodoItem {
  id: number
  title: string
  type: string
  priority: string
  dueTime: string
}

export interface ChartItem {
  name: string
  value: number
}

// ==================== 智能助手 ====================
export interface AssistantSeries {
  name: string
  type: string
  data: number[]
}

export type AssistantChartType = 'bar' | 'line' | 'pie' | 'funnel' | 'radar'

export interface AssistantChart {
  type: AssistantChartType
  title: string
  categories?: string[]
  series?: AssistantSeries[]
  items?: ChartItem[]
  indicators?: string[]
  values?: number[]
}

export interface AssistantTable {
  columns: { key: string; label: string }[]
  rows: Record<string, unknown>[]
}

export interface AssistantReply {
  intent: string
  title: string | null
  reply: string
  chart: AssistantChart | null
  table: AssistantTable | null
  suggestions: string[]
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  time?: number
  reply?: AssistantReply
}

export const STATUS_TEXT: Record<string, string> = {
  ACTIVE: '在职',
  PROBATION: '试用',
  LEFT: '离职',
  SUSPEND: '停职',
  NORMAL: '正常',
  LATE: '迟到',
  EARLY: '早退',
  ABSENT: '缺勤',
  LEAVE: '请假',
  FIELD: '外勤',
}
