<template>
  <div class="dashboard-page">
    <header class="dashboard-header">
      <el-button type="primary" link @click="router.push('/')">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="dashboard-header__title">数据看板</span>
    </header>
    <div v-if="loading" class="dashboard-loading">
      <el-skeleton :rows="5" animated />
    </div>
    <div v-else class="dashboard-body">
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.todayCount ?? 0 }}</div>
          <div class="stat-card__label">今日报修量</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__value">{{ stats.totalCount ?? 0 }}</div>
          <div class="stat-card__label">累计工单</div>
        </div>
      </div>
      <div class="chart-block">
        <h4 class="chart-block__title">区域报修 Top10</h4>
        <div ref="locationChartRef" class="chart-container"></div>
      </div>
      <div class="chart-block">
        <h4 class="chart-block__title">工单状态分布</h4>
        <div ref="statusChartRef" class="chart-container"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getTodayStats, getTotalStats, getLocationTop10, getStatusDist } from '@/api/dashboard'
import { getRepairStatusLabel } from '@/constants/repairStatus'

const router = useRouter()
const loading = ref(true)
const stats = ref({
  todayCount: 0,
  totalCount: 0,
  locationTop10: [],
  statusDist: []
})
const locationChartRef = ref(null)
const statusChartRef = ref(null)
let locationChartInstance = null
let statusChartInstance = null

onMounted(async () => {
  try {
    const [today, total, locations, statusDist] = await Promise.all([
      getTodayStats(),
      getTotalStats(),
      getLocationTop10(),
      getStatusDist()
    ])
    stats.value.todayCount = today ?? 0
    stats.value.totalCount = total ?? 0
    stats.value.locationTop10 = Array.isArray(locations) ? locations : []
    stats.value.statusDist = Array.isArray(statusDist) ? statusDist : []
  } catch (e) {
    stats.value = {
      todayCount: 0,
      totalCount: 0,
      locationTop10: [],
      statusDist: []
    }
  } finally {
    loading.value = false
    await nextTick()
    renderLocationChart()
    renderStatusChart()
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (locationChartInstance) {
    locationChartInstance.dispose()
    locationChartInstance = null
  }
  if (statusChartInstance) {
    statusChartInstance.dispose()
    statusChartInstance = null
  }
})

watch(
  [() => stats.value.locationTop10, () => stats.value.statusDist],
  () => {
    if (loading.value) return
    renderLocationChart()
    renderStatusChart()
  },
  { deep: true }
)

function handleResize() {
  if (locationChartInstance) locationChartInstance.resize()
  if (statusChartInstance) statusChartInstance.resize()
}

function renderLocationChart() {
  if (!locationChartRef.value) return
  const data = stats.value.locationTop10 || []
  if (!locationChartInstance) {
    locationChartInstance = echarts.init(locationChartRef.value)
  }
  locationChartInstance.clear()
  locationChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map((x) => x.name) },
    yAxis: { type: 'value' },
    series: [
      {
        type: 'bar',
        data: data.map((x) => x.value),
        itemStyle: { color: '#409EFF' }
      }
    ]
  })
}

function renderStatusChart() {
  if (!statusChartRef.value) return
  const source = stats.value.statusDist || []
  const data = source.map((item) => ({
    name: item.label || getRepairStatusLabel(item.name),
    value: item.value
  }))
  if (!statusChartInstance) {
    statusChartInstance = echarts.init(statusChartRef.value)
  }
  statusChartInstance.clear()
  statusChartInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 10,
      orient: 'horizontal',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        fontSize: 12
      },
      type: 'scroll'
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '65%'],
        // 关闭自动隐藏，改为平移避让，尽量让所有标签都显示出来
        avoidLabelOverlap: false,
        data,
        label: {
          show: true,
          position: 'outside',
          alignTo: 'edge',
          minMargin: 5,
          formatter: '{b}: {d}%',
          fontSize: 13,
          lineHeight: 20
        },
        labelLine: {
          show: true,
          length: 15,
          length2: 10,
          smooth: true
        },
        labelLayout: {
          hideOverlap: false,
          moveOverlap: 'shiftY'
        }
      }
    ]
  })
}
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: var(--el-fill-color-lighter);
}
.dashboard-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.dashboard-header__title {
  font-size: 16px;
  font-weight: 500;
}
.dashboard-loading {
  padding: 16px;
}
.dashboard-body {
  padding: 16px;
}
.stat-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.stat-card {
  padding: 20px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-color-primary);
}
.stat-card__label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}
.chart-block {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--el-bg-color);
  border-radius: 12px;
}
.chart-block__title {
  margin: 0 0 20px 0;
  font-size: 16px;
}
.chart-container {
  width: 100%;
  min-height: 320px;
}
</style>
