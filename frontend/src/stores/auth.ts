import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('hrms-token') || '')
  const user = ref<UserInfo | null>(null)

  async function login(username: string, password: string) {
    const res = await authApi.login(username, password)
    token.value = res.token
    localStorage.setItem('hrms-token', res.token)
    await fetchMe()
  }

  async function fetchMe() {
    const info = await authApi.me()
    user.value = info as unknown as UserInfo
    return info
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('hrms-token')
  }

  function hasRole(...roles: string[]) {
    return user.value && roles.includes(user.value.role)
  }

  return { token, user, login, logout, fetchMe, hasRole }
})
