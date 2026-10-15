import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/WorkbenchView.vue'),
        meta: { title: '工作台', icon: 'HomeFilled', roles: ['ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'] },
      },
      // ===== 组织人事 =====
      {
        path: 'org',
        name: 'Org',
        component: () => import('@/views/org/OrgView.vue'),
        meta: { title: '组织架构', icon: 'Share', roles: ['ADMIN', 'HR', 'MANAGER'] },
      },
      {
        path: 'personnel/employees',
        name: 'Employees',
        component: () => import('@/views/personnel/EmployeeListView.vue'),
        meta: { title: '员工档案', icon: 'UserFilled', roles: ['ADMIN', 'HR', 'MANAGER'] },
      },
      // ===== 考勤排班 =====
      {
        path: 'attendance',
        name: 'Attendance',
        component: () => import('@/views/attendance/AttendanceView.vue'),
        meta: { title: '考勤排班', icon: 'Calendar', roles: ['ADMIN', 'HR', 'MANAGER'] },
      },
      // ===== 薪酬福利 =====
      {
        path: 'payroll',
        name: 'Payroll',
        component: () => import('@/views/payroll/PayrollView.vue'),
        meta: { title: '薪酬核算', icon: 'Money', roles: ['ADMIN', 'HR'] },
      },
      // ===== 绩效 =====
      {
        path: 'performance',
        name: 'Performance',
        component: () => import('@/views/performance/PerformanceView.vue'),
        meta: { title: '绩效管理', icon: 'TrendCharts', roles: ['ADMIN', 'HR', 'MANAGER'] },
      },
      // ===== 培训发展 =====
      {
        path: 'training',
        name: 'Training',
        component: () => import('@/views/training/TrainingView.vue'),
        meta: { title: '培训发展', icon: 'Reading', roles: ['ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'] },
      },
      // ===== 招聘人才 =====
      {
        path: 'recruitment',
        name: 'Recruitment',
        component: () => import('@/views/recruitment/RecruitmentView.vue'),
        meta: { title: '招聘管理', icon: 'Search', roles: ['ADMIN', 'HR', 'MANAGER'] },
      },
      // ===== 智能助手 =====
      {
        path: 'assistant',
        name: 'Assistant',
        component: () => import('@/views/assistant/AssistantView.vue'),
        meta: { title: '智能助手', icon: 'MagicStick', roles: ['ADMIN', 'HR', 'MANAGER', 'EMPLOYEE'] },
      },
      // ===== 员工自助（仅普通员工角色） =====
      {
        path: 'self-service',
        name: 'SelfService',
        component: () => import('@/views/selfservice/SelfServiceView.vue'),
        meta: { title: '员工自助', icon: 'User', roles: ['EMPLOYEE'] },
      },
      // ===== 系统管理 =====
      {
        path: 'system',
        name: 'System',
        component: () => import('@/views/system/SystemView.vue'),
        meta: { title: '系统管理', icon: 'Setting', roles: ['ADMIN'] },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/error/NotFoundView.vue'),
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局守卫：未登录跳转登录页；已登录则按路由 meta.roles 二次鉴权
router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const token = authStore.token
  if (to.path !== '/login' && !token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && token) {
    return { path: '/' }
  }
  // 整页刷新后 store.user 为空，先拉取一次用户信息以拿到角色
  if (token && !authStore.user) {
    try {
      await authStore.fetchMe()
    } catch {
      authStore.logout()
      return { path: '/login' }
    }
  }
  const roles = to.meta?.roles as string[] | undefined
  if (roles && !roles.includes(authStore.user?.role || '')) {
    ElMessage.error('无权限访问该页面')
    return { path: '/dashboard' }
  }
  return true
})

export default router
