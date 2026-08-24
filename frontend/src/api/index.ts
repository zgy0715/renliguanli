import { http } from './request'
import type {
  AssistantReply,
  ChatMessage,
  DeptNode,
  EmployeeVO,
  KpiSummary,
  LoginResponse,
  PageResult,
  TodoItem,
} from '@/types'

// ==================== 认证 ====================
export const authApi = {
  login: (username: string, password: string) =>
    http.post<LoginResponse>('/auth/login', { username, password }),
  me: () => http.get('/auth/me'),
}

// ==================== 工作台 ====================
export const dashboardApi = {
  summary: () => http.get<KpiSummary>('/dashboard/summary'),
  headcountTrend: () => http.get('/dashboard/headcount-trend'),
  payrollTrend: () => http.get('/dashboard/payroll-trend'),
  todos: () => http.get<TodoItem[]>('/dashboard/todos'),
  announcements: () => http.get('/dashboard/announcements'),
}

// ==================== 组织 ====================
export const orgApi = {
  tree: () => http.get<DeptNode[]>('/departments/tree'),
}

// ==================== 员工 ====================
export const employeeApi = {
  search: (params: Record<string, unknown>) => http.get<PageResult<EmployeeVO>>('/employees', params),
  detail: (id: number) => http.get<Record<string, unknown>>(`/employees/${id}`),
  tags: () => http.get<string[]>('/employees/tags'),
  create: (data: unknown) => http.post('/employees', data),
  update: (id: number, data: unknown) => http.put(`/employees/${id}`, data),
  remove: (id: number) => http.delete(`/employees/${id}`),
}

// ==================== 考勤 ====================
export const attendanceApi = {
  calendar: (month: string) => http.get('/attendance/calendar', { month }),
  monthly: (month: string, deptId?: number) => http.get('/attendance/monthly', { month, deptId }),
  exceptions: (month: string) => http.get('/attendance/exceptions', { month }),
  shifts: () => http.get('/attendance/shifts'),
  confirm: (id: number, confirmed: boolean) =>
    http.put(`/attendance/records/${id}/confirm?confirmed=${confirmed}`),
}

// ==================== 薪酬 ====================
export const payrollApi = {
  list: (month: string) => http.get('/payroll/list', { month }),
  summary: (month: string) => http.get('/payroll/summary', { month }),
  deptCost: (month: string) => http.get('/payroll/dept-cost', { month }),
  payslip: (employeeId: number, month: string) =>
    http.get('/payroll/payslip', { employeeId, month }),
  calculate: (month: string) => http.post(`/payroll/calculate?month=${month}`),
  /** 确认/发放：持久化单条薪酬状态 */
  updateStatus: (id: number, status: string) =>
    http.put(`/payroll/records/${id}/status?status=${encodeURIComponent(status)}`),
}

// ==================== 智能助手 ====================
export const assistantApi = {
  ask: (question: string) => http.post<AssistantReply>('/assistant/ask', { question }),
  history: () => http.get<ChatMessage[]>('/assistant/history'),
  clearHistory: () => http.delete('/assistant/history'),
}

// ==================== 数据分析 ====================
export const analyticsApi = {
  structure: (dimension: string) => http.get('/analytics/structure', { dimension }),
  deptHeadcount: () => http.get('/analytics/dept-headcount'),
  attritionTrend: () => http.get('/analytics/attrition-trend'),
  nineBox: () => http.get('/analytics/nine-box'),
  recruitmentFunnel: () => http.get('/analytics/recruitment-funnel'),
  recruitmentChannel: () => http.get('/analytics/recruitment-channel'),
  attendanceTrend: () => http.get('/analytics/attendance-trend'),
  performanceDistribution: (period: string) =>
    http.get('/analytics/performance-distribution', { period }),
  deptEfficiency: () => http.get('/analytics/dept-efficiency'),
  trainingStats: () => http.get('/analytics/training-stats'),
  payrollCostTrend: () => http.get('/analytics/payroll-cost-trend'),
  capabilityRadar: (dept: string) => http.get('/analytics/capability-radar', { dept }),
}

// ==================== 绩效 / 培训 / 招聘 ====================
export const performanceApi = {
  list: (period?: string) => http.get('/performance/list', { period }),
}

export const trainingApi = {
  courses: () => http.get('/training/courses'),
  records: (employeeId?: number) => http.get('/training/records', { employeeId }),
}

export const recruitmentApi = {
  candidates: (params?: Record<string, unknown>) => http.get('/recruitment/candidates', params),
  jobs: () => http.get('/recruitment/jobs'),
  updateStatus: (id: number, status: string) =>
    http.put(`/recruitment/candidates/${id}/status?status=${encodeURIComponent(status)}`),
}

// ==================== 自助服务 ====================
export const selfServiceApi = {
  profile: () => http.get('/self-service/profile'),
  leaveBalance: () => http.get('/self-service/leave-balance'),
  payslips: () => http.get('/self-service/payslips'),
  certificates: () => http.get('/self-service/certificates'),
  applyCertificate: (type: string, purpose: string) =>
    http.post(`/self-service/certificates?type=${type}&purpose=${encodeURIComponent(purpose)}`),
  myAttendance: (month?: string) => http.get('/self-service/attendance', { month }),
  myPerformance: () => http.get('/self-service/performance'),
}

// ==================== 系统 ====================
export const systemApi = {
  users: () => http.get('/system/users'),
  logs: () => http.get('/system/logs'),
  dicts: (type?: string) => http.get('/system/dicts', { type }),
}

export const positionApi = {
  list: () => http.get('/positions'),
}
