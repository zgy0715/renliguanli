<template>
  <div class="hrms-card chart-card">
    <div class="chart-card__header">
      <div>
        <span class="chart-card__title">{{ title }}</span>
        <span v-if="subtitle" class="chart-card__subtitle">{{ subtitle }}</span>
      </div>
      <div class="chart-card__extra">
        <slot name="extra" />
      </div>
    </div>
    <div class="chart-card__body">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.chart-card {
  padding: 16px 18px;
}

.chart-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.chart-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--hrms-text-1);
}

.chart-card__subtitle {
  font-size: 12px;
  color: var(--hrms-text-3);
  margin-left: 8px;
}

.chart-card__body {
  width: 100%;
}
</style>
