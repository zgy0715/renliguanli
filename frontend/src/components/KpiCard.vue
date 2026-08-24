<template>
  <div class="kpi-card">
    <div class="kpi-card__label">
      <el-icon v-if="icon" :style="{ color: accent }"><component :is="icon" /></el-icon>
      {{ label }}
    </div>
    <div class="kpi-card__value">
      {{ value }}<span v-if="unit" class="kpi-card__unit">{{ unit }}</span>
    </div>
    <div v-if="trend" class="kpi-card__trend" :class="trendType === 'up' ? 'kpi-card__trend--up' : 'kpi-card__trend--down'">
      <el-icon>
        <CaretTop v-if="trendType === 'up'" />
        <CaretBottom v-else />
      </el-icon>
      {{ trendText }}
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

defineProps<{
  label: string
  value: string | number
  unit?: string
  icon?: string | Component
  accent?: string
  trend?: string
  trendText?: string
  trendType?: 'up' | 'down'
}>()
</script>
