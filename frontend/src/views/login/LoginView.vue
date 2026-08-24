<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-logo">
        <div class="brand-mark">HRMS</div>
        <div>
          <div class="brand-name">企业员工管理系统</div>
          <div class="brand-slogan">HRMS · 全生命周期人力数字化平台</div>
        </div>
      </div>
      <div class="brand-features">
        <div class="feature-item" v-for="f in features" :key="f.title">
          <el-icon :size="22" :color="'#fff'"><component :is="f.icon" /></el-icon>
          <div>
            <div class="feature-title">{{ f.title }}</div>
            <div class="feature-desc">{{ f.desc }}</div>
          </div>
        </div>
      </div>
      <div class="brand-footer">数据驱动决策 · 人才价值可量化 · 全员自助协作</div>
    </div>

    <!-- 右侧登录表单 -->
    <div class="login-form-area">
      <div class="login-card">
        <div class="login-heading">
          <div class="login-title">欢迎回来</div>
          <div class="login-subtitle">请登录您的系统账号</div>
        </div>
        <el-form :model="form" @keyup.enter="handleLogin">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              :prefix-icon="Lock"
            />
          </el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form>
        <el-divider><span class="divider-text">演示账号</span></el-divider>
        <div class="demo-accounts">
          <el-tag
            v-for="acc in demoAccounts"
            :key="acc.username"
            class="demo-tag"
            effect="plain"
            round
            @click="quickLogin(acc.username)"
          >
            {{ acc.label }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, MagicStick, UserFilled, Calendar, Money } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({ username: 'admin', password: '123456' })

const features = [
  { icon: MagicStick, title: '智能助手', desc: '自然语言问数 · 一聊即得' },
  { icon: UserFilled, title: '全生命周期档案', desc: '入职到离职 · 一键全流程线上化' },
  { icon: Calendar, title: '智能考勤排班', desc: '异常自动识别 · 无感打卡' },
  { icon: Money, title: '薪酬核算', desc: '复杂规则零代码配置 · 一键核算' },
]

const demoAccounts = [
  { username: 'admin', label: '系统管理员' },
  { username: 'hr', label: 'HR专员' },
  { username: 'manager1', label: '部门主管' },
  { username: 'employee1', label: '普通员工' },
]

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}

async function quickLogin(username: string) {
  form.username = username
  form.password = '123456'
  await handleLogin()
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  background: var(--hrms-bg);
}

.login-brand {
  flex: 1.2;
  background: linear-gradient(145deg, #1d3fae 0%, #2f6bf0 55%, #4d8bff 100%);
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  color: #fff;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.brand-name {
  font-size: 22px;
  font-weight: 700;
}

.brand-slogan {
  font-size: 13px;
  opacity: 0.75;
  margin-top: 2px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
}

.feature-desc {
  font-size: 13px;
  opacity: 0.7;
  margin-top: 2px;
}

.brand-footer {
  font-size: 13px;
  opacity: 0.6;
  letter-spacing: 1px;
}

.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 380px;
  background: var(--hrms-card);
  border: 1px solid var(--hrms-border);
  border-radius: 16px;
  padding: 36px 32px;
  box-shadow: var(--hrms-shadow-hover);
}

.login-heading {
  margin-bottom: 24px;
}

.login-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--hrms-text-1);
}

.login-subtitle {
  font-size: 13px;
  color: var(--hrms-text-3);
  margin-top: 6px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
  font-weight: 600;
  letter-spacing: 4px;
}

.divider-text {
  font-size: 12px;
  color: var(--hrms-text-3);
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.demo-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.demo-tag:hover {
  background: var(--hrms-primary);
  color: #fff;
  border-color: var(--hrms-primary);
}

@media (max-width: 900px) {
  .login-brand {
    display: none;
  }
}
</style>
