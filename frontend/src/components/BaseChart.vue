<template>
  <div ref="chartRef" class="base-chart" :style="{ height }"></div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useAppStore } from '@/stores/app'

const props = withDefaults(
  defineProps<{
    option: Record<string, unknown>
    height?: string
  }>(),
  { height: '280px' },
)

const chartRef = ref<HTMLElement>()
const appStore = useAppStore()
let chart: echarts.ECharts | null = null

function render() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const dark = appStore.theme === 'dark'
  // 文本颜色随主题
  const mergedOption = {
    textStyle: { color: dark ? '#a4a9b8' : '#4e5969' },
    ...props.option,
  }
  chart.setOption(mergedOption as echarts.EChartsCoreOption, true)
}

function handleResize() {
  chart?.resize()
}

watch(
  () => props.option,
  () => nextTick(render),
  { deep: true },
)

watch(
  () => appStore.theme,
  () => nextTick(render),
)

onMounted(() => {
  render()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.base-chart {
  width: 100%;
}
</style>
