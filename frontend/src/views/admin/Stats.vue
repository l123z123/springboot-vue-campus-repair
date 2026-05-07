<template>
  <div class="admin-stats">
    <div v-if="loading" class="admin-stats__loading">
      <el-skeleton :rows="6" animated />
    </div>
    <template v-else>
      <!-- 顶部数字统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card">
          <div class="stat-card__label">今日报修量</div>
          <div class="stat-card__value">{{ stats.todayCount ?? 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-card__label">累计工单</div>
          <div class="stat-card__value">{{ stats.totalCount ?? 0 }}</div>
        </div>
      </div>

      <!-- 第一行：Top10 + 玫瑰图 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :md="12">
          <el-card class="stats-card" shadow="never">
            <div class="stats-card__header">
              <div>
                <div class="stats-card__title">区域报修 Top10</div>
                <div class="stats-card__subtitle">按报修数量排序的前十区域</div>
              </div>
            </div>
            <div ref="locationChartRef" class="stats-chart"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card class="stats-card" shadow="never">
            <div class="stats-card__header">
              <div>
                <div class="stats-card__title">工单状态分布</div>
                <div class="stats-card__subtitle">南丁格尔玫瑰图</div>
              </div>
            </div>
            <div ref="statusChartRef" class="stats-chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第二行：故障类型分布 + 报修时段热力图 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :md="12">
          <el-card class="stats-card" shadow="never">
            <div class="stats-card__header">
              <div>
                <div class="stats-card__title">故障类型分布</div>
                <div class="stats-card__subtitle">按大类与小类展示工单结构</div>
              </div>
            </div>
            <div ref="typeChartRef" class="stats-chart"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card class="stats-card" shadow="never">
            <div class="stats-card__header">
              <div>
                <div class="stats-card__title">报修时段热力图</div>
                <div class="stats-card__subtitle">一周内各时段的报修高峰</div>
              </div>
            </div>
            <div ref="heatmapChartRef" class="stats-chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 第三行：维修工绩效榜 -->
      <el-row :gutter="16" class="stats-row">
        <el-col :xs="24" :md="24">
          <el-card class="stats-card" shadow="never">
            <div class="stats-card__header">
              <div>
                <div class="stats-card__title">维修工绩效榜</div>
                <div class="stats-card__subtitle">完成工单数量与评分</div>
              </div>
            </div>
            <div ref="staffChartRef" class="stats-chart"></div>
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  getTodayStats,
  getTotalStats,
  getLocationTop10,
  getStatusDist,
  getTypeTree,
  getHourHeatmap,
  getStaffPerformance
} from '@/api/dashboard'
import { getRepairStatusLabel } from '@/constants/repairStatus'
import { MOCK_STAFF } from '@/mock/tickets'

defineOptions({ name: 'AdminStats' })

const loading = ref(true)
const stats = ref({
  todayCount: 0,
  totalCount: 0,
  locationTop10: [],
  statusDist: []
})

// 已有图表
const locationChartRef = ref(null)
const statusChartRef = ref(null)
let locationChartInstance = null
let statusChartInstance = null

// 新增图表：故障类型、时段热力、维修工绩效
const typeChartRef = ref(null)
const heatmapChartRef = ref(null)
const staffChartRef = ref(null)
let typeChartInstance = null
let heatmapChartInstance = null
let staffChartInstance = null

const typeTreeData = ref([])
const heatmapMatrix = ref([]) // [hour, day, value]
const staffPerformance = ref([])

onMounted(async () => {
  try {
    const [
      today,
      total,
      locations,
      statusDist,
      categoryTree,
      hourHeatmap,
      staffPerf
    ] = await Promise.all([
      getTodayStats(),
      getTotalStats(),
      getLocationTop10(),
      getStatusDist(),
      getTypeTree(),
      getHourHeatmap(),
      getStaffPerformance()
    ])
    stats.value.todayCount = today ?? 0
    stats.value.totalCount = total ?? 0
    stats.value.locationTop10 = Array.isArray(locations) ? locations : []
    stats.value.statusDist = Array.isArray(statusDist) ? statusDist : []
    if (Array.isArray(categoryTree)) {
      typeTreeData.value = categoryTree
    }
    if (Array.isArray(hourHeatmap)) {
      heatmapMatrix.value = hourHeatmap.map((item) => [item.hour, item.day, item.value])
    }
    let perf = Array.isArray(staffPerf) ? staffPerf : []
    // 后端暂时没有真实绩效数据时，使用更贴近实战的占位数据：
    // 以值班频率高的师傅完成 60~40 单，其余 35~20 单，评分在 4.0~4.9 之间
    if (!perf.length && Array.isArray(MOCK_STAFF)) {
      const baseCount = 20
      const maxExtra = 40 // 顶尖维修工额外 +40 单
      const maxIndex = Math.min(7, MOCK_STAFF.length - 1)
      perf = MOCK_STAFF.slice(0, 8).map((s, idx) => {
        const weight = (maxIndex - idx) / maxIndex // 1 ~ 0
        const count = Math.round(baseCount + maxExtra * weight) // 大约 60~20 单
        const rating = 4 + 0.9 * weight // 4.9~4.0
        return {
          name: s.realName,
          count,
          rating: Number(rating.toFixed(1))
        }
      })
    }
    staffPerformance.value = perf
  } catch (_) {
    stats.value = { todayCount: 0, totalCount: 0, locationTop10: [], statusDist: [] }
    typeTreeData.value = []
    heatmapMatrix.value = []
    staffPerformance.value = []
  } finally {
    loading.value = false
    await nextTick()
    renderLocationChart()
    renderStatusChart()
    renderTypeChart()
    renderHeatmapChart()
    renderStaffChart()
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
  if (typeChartInstance) {
    typeChartInstance.dispose()
    typeChartInstance = null
  }
  if (heatmapChartInstance) {
    heatmapChartInstance.dispose()
    heatmapChartInstance = null
  }
  if (staffChartInstance) {
    staffChartInstance.dispose()
    staffChartInstance = null
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
  if (typeChartInstance) typeChartInstance.resize()
  if (heatmapChartInstance) heatmapChartInstance.resize()
  if (staffChartInstance) staffChartInstance.resize()
}

function renderLocationChart() {
  if (!locationChartRef.value) return
  const data = stats.value.locationTop10 || []
  if (!locationChartInstance) {
    locationChartInstance = echarts.init(locationChartRef.value)
  }
  locationChartInstance.clear()
  locationChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' }
    },
    grid: {
      left: 80,
      right: 24,
      top: 16,
      bottom: 12
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { type: 'dashed', color: '#E5E7EB' } }
    },
    yAxis: {
      type: 'category',
      inverse: true,
      data: data.map((x) => x.name),
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#4B5563' }
    },
    series: [
      {
        type: 'bar',
        data: data.map((x) => x.value),
        barWidth: 18,
        label: {
          show: true,
          position: 'right',
          color: '#111827',
          fontWeight: 500
        },
        itemStyle: {
          borderRadius: [0, 999, 999, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#6366F1' },
            { offset: 1, color: '#A5B4FC' }
          ])
        }
      }
    ]
  })
}

function renderTypeChart() {
  if (!typeChartRef.value) return
  if (!typeChartInstance) {
    typeChartInstance = echarts.init(typeChartRef.value)
  }
  const data = typeTreeData.value.length > 0 ? typeTreeData.value : [{ name: '暂无数据', value: 1 }]
  typeChartInstance.clear()
  typeChartInstance.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'sunburst',
        radius: ['18%', '80%'],
        data: data,
        label: {
          rotate: 'radial'
        },
        levels: [
          {},
          { r0: '18%', r: '45%', label: { rotate: 0 } },
          { r0: '45%', r: '80%' }
        ]
      }
    ]
  })
}

function renderHeatmapChart() {
  if (!heatmapChartRef.value) return
  if (!heatmapChartInstance) {
    heatmapChartInstance = echarts.init(heatmapChartRef.value)
  }
  const hours = Array.from({ length: 24 }, (_, i) => `${i}时`)
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const data = heatmapMatrix.value.map((item) => [item[0], item[1], item[2]])
  const values = data.map((d) => d[2])
  let max = 10
  let min = 0
  if (values.length > 0) {
    max = Math.max(...values)
    min = Math.min(...values)
  }
  heatmapChartInstance.clear()
  heatmapChartInstance.setOption({
    tooltip: {
      position: 'top'
    },
    grid: {
      top: 30,
      left: 60,
      right: 20,
      bottom: 30
    },
    xAxis: {
      type: 'category',
      data: hours,
      splitArea: { show: true }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: { show: true }
    },
    visualMap: {
      min,
      max,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: 0,
      inRange: {
        color: ['#E0F2FE', '#22C55E', '#4C1D95']
      }
    },
    series: [
      {
        type: 'heatmap',
        data,
        label: { show: false },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowColor: 'rgba(0,0,0,0.3)'
          }
        }
      }
    ]
  })
}

function renderStaffChart() {
  if (!staffChartRef.value) return
  if (!staffChartInstance) {
    staffChartInstance = echarts.init(staffChartRef.value)
  }
  // 按完成工单数量从高到低排序后再展示
  const data = [...staffPerformance.value].sort((a, b) => (b.count || 0) - (a.count || 0))
  const names = data.map((x) => x.name)
  const counts = data.map((x) => x.count)
  staffChartInstance.clear()
  staffChartInstance.setOption({
    grid: { left: 80, right: 40, top: 20, bottom: 30 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter(params) {
        const item = data[params[0].dataIndex]
        return `${item.name}<br/>完成工单：${item.count} 单<br/>平均评分：${item.rating}`
      }
    },
    xAxis: {
      type: 'value',
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { type: 'dashed', color: '#E5E7EB' } }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: { show: false },
      axisTick: { show: false }
    },
    series: [
      {
        name: '完成工单数',
        type: 'bar',
        data: counts,
        barWidth: 18,
        itemStyle: {
          borderRadius: [0, 999, 999, 0],
          color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
            { offset: 0, color: '#10B981' },
            { offset: 1, color: '#6EE7B7' }
          ])
        }
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
    series: [
      {
        type: 'pie',
        radius: ['25%', '70%'],
        center: ['50%', '50%'],
        roseType: 'radius',
        data,
        itemStyle: {
          borderRadius: 6
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 13,
          lineHeight: 20
        },
        labelLine: {
          show: true,
          length: 20,
          length2: 12,
          smooth: true
        }
      }
    ]
  })
}
</script>

<style scoped>
.admin-stats {
  min-height: 400px;
}

.admin-stats__loading {
  padding: 20px 0;
}

.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  padding: 18px 20px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.stat-card__label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}

.stats-row {
  margin-top: 8px;
}

.stats-card {
  margin-top: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.stats-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stats-card__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.stats-card__subtitle {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 2px;
}

.stats-chart {
  width: 100%;
  min-height: 320px;
}
</style>
