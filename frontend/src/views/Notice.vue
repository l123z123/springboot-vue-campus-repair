<template>
  <div class="notice-page">
    <div v-if="loading" class="loading-wrap">
      <el-skeleton :rows="4" animated />
    </div>
    <div v-else-if="noticeList.length" class="notice-grid">
      <div v-for="item in noticeList" :key="item.id" class="notice-card" :class="{ 'is-pinned': item.pinned }">
        <div class="notice-card__head">
          <el-tag v-if="item.pinned" type="danger" size="small" effect="dark">置顶</el-tag>
          <h4 class="notice-card__title">{{ item.title }}</h4>
        </div>
        <p class="notice-card__content">{{ item.content }}</p>
        <p class="notice-card__meta">{{ formatTime(item.createTime) }} · {{ item.author || '后勤管理处' }}</p>
      </div>
      <div v-if="total > noticeList.length" class="pagination-wrap">
        <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadData" />
      </div>
    </div>
    <el-empty v-else description="暂无公告" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoticeList } from '@/api/notice'

defineOptions({ name: 'Notice' })

const loading = ref(false)
const noticeList = ref([])
const page = ref(1)
const pageSize = 20
const total = ref(0)

function formatTime(t) {
  if (!t) return ''
  if (typeof t === 'string') return t.replace('T', ' ').substring(0, 19)
  return t
}

async function loadData() {
  loading.value = true
  try {
    const res = await getNoticeList({ page: page.value, size: pageSize })
    noticeList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    noticeList.value = []
    ElMessage.error(e?.message || '加载公告列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('focus', loadData)
})

onUnmounted(() => {
  window.removeEventListener('focus', loadData)
})
</script>

<style scoped>
.notice-page { width: 100%; }
.loading-wrap { padding: 20px 0; }

.notice-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 16px;
}

.notice-card {
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s;
}
.notice-card:hover { box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1); }

.notice-card.is-pinned {
  grid-column: 1 / -1;
  border-left: 4px solid var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.notice-card__head {
  display: flex; align-items: center; gap: 10px; margin-bottom: 12px;
}
.notice-card__title {
  margin: 0; font-size: 16px; font-weight: 600;
  color: var(--el-text-color-primary);
}

.notice-card__content {
  margin: 0 0 12px 0;
  font-size: 14px; color: var(--el-text-color-regular);
  line-height: 1.7;
}

.notice-card__meta {
  margin: 0; font-size: 12px; color: var(--el-text-color-placeholder);
}

.pagination-wrap { margin-top: 16px; display: flex; justify-content: center; }
</style>
