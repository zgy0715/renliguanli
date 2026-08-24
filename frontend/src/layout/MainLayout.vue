<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : 'var(--hrms-sidebar-width)'" class="layout-aside">
      <div class="logo" @click="router.push('/dashboard')">
        <div class="logo-mark">HRMS</div>
        <transition name="fade">
          <span v-show="!appStore.sidebarCollapsed" class="logo-text">人力管理系统</span>
        </transition>
      </div>
      <el-menu
        :default-active="route.path"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        router
        class="layout-menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="'/' + item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Expand v-if="appStore.sidebarCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 全局搜索 -->
          <el-popover placement="bottom-start" :width="420" trigger="click" :show-arrow="false" popper-class="search-popover">
            <template #reference>
              <div class="global-search">
                <el-icon><Search /></el-icon>
                <span>搜索员工 / 工号 / 功能...</span>
                <span class="search-kbd">Ctrl K</span>
              </div>
            </template>
            <GlobalSearch />
          </el-popover>

          <!-- 待办铃铛 -->
          <el-badge :value="todoCount" :hidden="todoCount === 0" class="header-badge">
            <el-icon class="header-icon" @click="router.push('/dashboard')"><Bell /></el-icon>
          </el-badge>

          <!-- 主题切换 -->
          <el-icon class="header-icon" @click="appStore.toggleTheme()">
            <Sunny v-if="appStore.theme === 'dark'" />
            <Moon v-else />
          </el-icon>

          <!-- 用户菜单 -->
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :style="{ background: avatarBg }">
                {{ (authStore.user?.displayName || 'U').charAt(0) }}
              </el-avatar>
              <span class="user-name">{{ authStore.user?.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-tag size="small" :type="roleTagType">{{ roleName }}</el-tag>
                </el-dropdown-item>
                <el-dropdown-item command="self">我的工作台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useAuthStore } from '@/stores/auth'
import { dashboardApi } from '@/api'
import GlobalSearch from '@/components/GlobalSearch.vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const avatarColors = ['#2f6bf0', '#23c17f', '#f59f00', '#f53f3f', '#7b61ff', '#00b8d9', '#e864a8', '#ff8a00', '#2d7d46', '#5b8cf5']

/** 头像底色：后端未下发 avatarColor 时按用户名哈希取色，避免全员同色 */
const avatarBg = computed(() => {
  const c = authStore.user?.avatarColor
  if (c != null && c >= 0 && c < avatarColors.length) return avatarColors[c]
  const name = authStore.user?.displayName || authStore.user?.username || ''
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  return avatarColors[h % avatarColors.length]
})

const roleName = computed(() => {
  const map: Record<string, string> = {
    ADMIN: '系统管理员',
    HR: 'HR专员',
    MANAGER: '部门主管',
    EMPLOYEE: '普通员工',
  }
  return map[authStore.user?.role || ''] || '未知角色'
})

const roleTagType = computed(() => {
  const map: Record<string, 'danger' | 'primary' | 'warning' | 'info'> = {
    ADMIN: 'danger',
    HR: 'primary',
    MANAGER: 'warning',
    EMPLOYEE: 'info',
  }
  return map[authStore.user?.role || ''] || 'info'
})

// 菜单按角色过滤
const menus = computed(() => {
  const role = authStore.user?.role || 'EMPLOYEE'
  const allRoutes = router.options.routes
    .find((r) => r.path === '/')?.children || []
  return allRoutes
    .filter((r) => !r.meta?.roles || (r.meta.roles as string[]).includes(role))
    .map((r) => ({ path: r.path, title: r.meta?.title as string, icon: (r.meta?.icon as string) || 'Menu' }))
})

const todoCount = ref(0)

async function loadTodos() {
  try {
    const todos = await dashboardApi.todos()
    todoCount.value = todos.length
  } catch {
    todoCount.value = 0
  }
}

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  } else if (cmd === 'self') {
    router.push('/self-service')
  }
}

onMounted(async () => {
  if (authStore.token && !authStore.user) {
    try {
      await authStore.fetchMe()
    } catch {
      /* 已由拦截器处理 */
    }
  }
  loadTodos()
})
</script>

<style scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}

.layout-aside {
  background: var(--hrms-card);
  border-right: 1px solid var(--hrms-border);
  transition: width 0.25s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
  border-bottom: 1px solid var(--hrms-border);
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: linear-gradient(135deg, #2f6bf0, #5b8cf5);
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  letter-spacing: -0.5px;
}

.logo-text {
  font-size: 15px;
  font-weight: 600;
  color: var(--hrms-text-1);
  white-space: nowrap;
}

.layout-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px 0;
  background: transparent;
  --el-menu-bg-color: transparent;
  --el-menu-hover-bg-color: var(--hrms-primary-bg);
}

.layout-menu :deep(.el-menu-item) {
  margin: 2px 8px;
  border-radius: 8px;
  height: 44px;
}

.layout-menu :deep(.el-menu-item.is-active) {
  background: var(--hrms-primary-bg);
  color: var(--hrms-primary);
  font-weight: 500;
}

.layout-header {
  height: 56px;
  background: var(--hrms-card);
  border-bottom: 1px solid var(--hrms-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 18px;
  cursor: pointer;
  color: var(--hrms-text-2);
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: var(--hrms-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.global-search {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 12px;
  background: var(--hrms-bg-subtle);
  border: 1px solid var(--hrms-border);
  border-radius: 8px;
  color: var(--hrms-text-3);
  font-size: 13px;
  cursor: pointer;
  width: 260px;
  transition: border-color 0.2s;
}

.global-search:hover {
  border-color: var(--hrms-primary);
}

.search-kbd {
  margin-left: auto;
  font-size: 11px;
  background: var(--hrms-border);
  padding: 1px 6px;
  border-radius: 4px;
  color: var(--hrms-text-3);
}

.header-icon {
  font-size: 18px;
  color: var(--hrms-text-2);
  cursor: pointer;
  transition: color 0.2s;
}

.header-icon:hover {
  color: var(--hrms-primary);
}

.header-badge {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 8px;
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--hrms-bg-subtle);
}

.user-name {
  font-size: 14px;
  color: var(--hrms-text-1);
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout-main {
  padding: 0;
  overflow-y: auto;
  background: var(--hrms-bg);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
