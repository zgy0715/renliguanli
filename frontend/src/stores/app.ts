import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const theme = ref<string>(localStorage.getItem('hrms-theme') || 'light')
  const sidebarCollapsed = ref<boolean>(localStorage.getItem('hrms-sidebar') === '1')

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
    applyTheme()
    localStorage.setItem('hrms-theme', theme.value)
  }

  function applyTheme() {
    if (theme.value === 'dark') {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('hrms-sidebar', sidebarCollapsed.value ? '1' : '0')
  }

  return { theme, sidebarCollapsed, toggleTheme, applyTheme, toggleSidebar }
})
