<template>
  <div class="admin-stats">
    <div v-if="loading" class="loading-wrap"><el-skeleton :rows="6" animated /></div>
    <template v-else>
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6" v-for="kpi in kpiCards" :key="kpi.key">
          <div class="kpi-card">
            <div class="kpi-card__value">{{ kpi.value }}</div>
            <div class="kpi-card__label">{{ kpi.label }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="chart-title">报修趋势（近30天）</span></template>
            <div ref="trendChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="never" class="chart-card">
            <template #header><span class="chart-title">分类分布</span></template>
            <div ref="categoryChartRef" class="chart-box"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-card shadow="never" class="chart-card">
        <template #header><span class="chart-title">维修工绩效</span></template>
        <el-table :data="staffPerformance" stripe border size="default">
          <template #empty><el-empty description="暂无数据" :image-size="60" /></template>
          <el-table-column type="index" label="排名" width="60" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="count" label="完成工单数" width="120" sortable />
          <el-table-column label="平均评分" width="140">
            <template #default="{row}">
              <el-rate :model-value="Number(row.rating)||0" :max="5" disabled size="small" show-text />
            </template>
          </el-table-column>
          <el-table-column prop="avgTime" label="平均耗时" min-width="120" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getTodayStats, getTotalStats, getLocationTop10, getStatusDist, getTypeTree, getStaffPerformance } from '@/api/dashboard'

defineOptions({ name: 'AdminStats' })

const loading = ref(true)
const stats = ref({ todayCount:0, totalCount:0, pending:0, completionRate:'0%' })

const trendChartRef = ref(null)
const categoryChartRef = ref(null)
let trendInstance = null
let categoryInstance = null

const locationData = ref([])
const typeTreeData = ref([])
const staffPerformance = ref([])

const kpiCards = computed(() => [
  { key:'total', label:'总工单数', value: stats.value.totalCount },
  { key:'today', label:'今日新增', value: stats.value.todayCount },
  { key:'pending', label:'待处理', value: stats.value.pending },
  { key:'rate', label:'完成率', value: stats.value.completionRate }
])

onMounted(async () => {
  try {
    const [today, total, locations, statusDist, categoryTree, staffPerf] = await Promise.all([
      getTodayStats(), getTotalStats(), getLocationTop10(), getStatusDist(), getTypeTree(), getStaffPerformance()
    ])
    stats.value.todayCount = today ?? 0
    stats.value.totalCount = total ?? 0
    const fromStatus = Array.isArray(statusDist) ? statusDist : []
    const doneCount = fromStatus.filter(s => ['已完成','已评价','学生确认','维修完成'].includes(s.label||s.name)).reduce((a,b) => a + (b.value||0), 0)
    stats.value.pending = fromStatus.filter(s => ['待审核','审核中','待派单'].includes(s.label||s.name)).reduce((a,b) => a + (b.value||0), 0)
    stats.value.completionRate = total ? Math.round(doneCount / total * 100) + '%' : '0%'
    locationData.value = Array.isArray(locations) ? locations : []
    typeTreeData.value = Array.isArray(categoryTree) ? categoryTree : []
    staffPerformance.value = Array.isArray(staffPerf) ? staffPerf : []
  } catch {
    stats.value = { todayCount:0, totalCount:0, pending:0, completionRate:'0%' }
  } finally {
    loading.value = false
    await nextTick()
    renderTrendChart()
    renderCategoryChart()
    window.addEventListener('resize', handleResize)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose(); categoryInstance?.dispose()
})

function handleResize() { trendInstance?.resize(); categoryInstance?.resize() }

function renderTrendChart() {
  if (!trendChartRef.value) return
  if (!trendInstance) trendInstance = echarts.init(trendChartRef.value)
  const data = locationData.value
  trendInstance.setOption({
    tooltip: { trigger:'axis' },
    grid: { left:50, right:20, top:20, bottom:30 },
    xAxis: { type:'category', data:data.map(d=>d.name), axisLabel:{rotate:45,fontSize:11} },
    yAxis: { type:'value' },
    series: [{ type:'line', data:data.map(d=>d.value), smooth:true, areaStyle:{opacity:0.15}, itemStyle:{color:'#409eff'} }]
  })
}

function renderCategoryChart() {
  if (!categoryChartRef.value) return
  if (!categoryInstance) categoryInstance = echarts.init(categoryChartRef.value)
  const data = typeTreeData.value
  const flat = []
  function walk(list) { list.forEach(item => { flat.push({ name:item.name, value:item.value||1 }); if(item.children) walk(item.children) }) }
  if (data.length) walk(data)
  else flat.push({ name:'暂无数据', value:1 })

  categoryInstance.setOption({
    tooltip: { trigger:'axis', axisPointer:{type:'shadow'} },
    grid: { left:50, right:20, top:10, bottom:30 },
    xAxis: { type:'category', data:flat.map(d=>d.name), axisLabel:{rotate:30,fontSize:11} },
    yAxis: { type:'value' },
    series: [{ type:'bar', data:flat.map(d=>d.value), barWidth:24,
      itemStyle:{ borderRadius:[4,4,0,0], color: new echarts.graphic.LinearGradient(0,0,0,1,[
        {offset:0,color:'#6366F1'},{offset:1,color:'#A5B4FC'}
      ])}
    }]
  })
}
</script>

<style scoped>
.admin-stats { min-height:400px; }
.loading-wrap { padding:20px 0; }

.kpi-row { margin-bottom:20px; }
.kpi-card { padding:20px; background:#fff; border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); text-align:center; }
.kpi-card__value { font-size:32px; font-weight:700; color:var(--el-color-primary); }
.kpi-card__label { font-size:13px; color:var(--el-text-color-secondary); margin-top:4px; }

.chart-row { margin-bottom:16px; }
.chart-card { border-radius:12px; box-shadow:0 2px 12px rgba(0,0,0,0.05); margin-bottom:16px; }
.chart-title { font-size:15px; font-weight:600; }
.chart-box { width:100%; min-height:300px; }
</style>
