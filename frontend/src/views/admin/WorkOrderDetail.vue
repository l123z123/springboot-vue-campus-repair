<template>
  <div class="admin-order-detail" v-loading="loading">
    <div class="header-row">
      <h2 class="title">工单详情</h2>
      <el-button type="primary" link @click="goBack">返回列表</el-button>
    </div>

    <el-card class="basic-card" shadow="never">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="工单号">{{ order.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(order.status)">{{ statusText(order.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="紧急程度">
          <el-tag :type="urgencyTagType(order.urgency)">{{ urgencyText(order.urgency) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报修地点">{{ order.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修时间">{{ order.date || '-' }}</el-descriptions-item>
        <el-descriptions-item label="报修人 ID">{{ order.userId || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="content-card" shadow="never">
      <template #header>
        <span>故障描述</span>
      </template>
      <p class="desc">{{ order.description || order.title || '暂无描述' }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRepairDetail } from '@/api/repair'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref({})

function statusText(s) {
  const map = { pending: '待接单', processing: '维修中', done: '已完成', cancelled: '已取消', evaluated: '已评价' }
  return map[s] ?? s ?? '待接单'
}

function statusTagType(s) {
  const map = { pending: 'warning', processing: 'primary', done: 'success', cancelled: 'info', evaluated: 'success' }
  return map[s] ?? 'info'
}

function urgencyText(u) {
  const map = { low: '普通', medium: '一般', high: '紧急' }
  return map[u] ?? u ?? '普通'
}

function urgencyTagType(u) {
  const map = { low: 'info', medium: 'warning', high: 'danger' }
  return map[u] ?? 'info'
}

function goBack() {
  router.push('/admin/workorders')
}

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  loading.value = true
  try {
    const data = await getRepairDetail(id)
    order.value = data || {}
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.admin-order-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
}

.desc {
  white-space: pre-wrap;
}
</style>
