<template>
  <div class="notice-page" :class="{ 'notice-page--staff': isStaffTab }">
    <header class="notice-header" :class="{ 'is-center': isStaffTab }">
      <el-button
        v-if="!isStaffTab"
        type="primary"
        link
        class="notice-header__back"
        @click="router.back()"
      >
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span v-else class="notice-header__spacer" />
      <span class="notice-header__title">{{ isStaffTab ? '工作公告' : '公告通知' }}</span>
      <span v-if="!isStaffTab" class="notice-header__spacer" />
      <span v-else class="notice-header__spacer" />
    </header>
    <div class="notice-body">
      <div v-for="(item, i) in noticeList" :key="i" class="notice-item">
        <h4 class="notice-item__title">{{ item.title }}</h4>
        <p class="notice-item__content">{{ item.content }}</p>
        <p class="notice-item__time">{{ item.time }}</p>
      </div>
      <div v-if="!noticeList.length" class="notice-empty">
        <el-icon :size="48" color="#ccc"><Bell /></el-icon>
        <p>暂无公告</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ArrowLeft, Bell } from '@element-plus/icons-vue'

const STORAGE_KEY = 'noticeList'
const DEFAULT_NOTICES = [
  { title: '校园报修系统上线通知', content: '即日起，校园报修系统正式上线，师生可通过本系统提交报修申请，维修人员将及时响应处理。', time: '2026-03-01 09:00' },
  { title: '清明节假期维修安排', content: '清明节期间（4月4日-6日）维修服务正常受理，响应时间可能略有延迟，敬请理解。', time: '2026-03-28 10:00' }
]

function loadList() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const arr = JSON.parse(raw)
      if (Array.isArray(arr) && arr.length) return arr
    }
  } catch {}
  return [...DEFAULT_NOTICES]
}

const router = useRouter()
const route = useRoute()
const isStaffTab = computed(() => route.path.startsWith('/staff/'))
const noticeList = ref([])

onMounted(() => {
  noticeList.value = loadList()
})
</script>

<style scoped>
.notice-page {
  min-height: 400px;
}
.notice-page--staff {
  background: linear-gradient(180deg, #f0fdfa 0%, #f1f5f9 100%);
}
.notice-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.notice-header.is-center {
  justify-content: center;
  position: relative;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  border-bottom-color: rgba(13, 148, 136, 0.1);
  padding-top: calc(12px + env(safe-area-inset-top, 0));
}
.notice-header__spacer {
  width: 40px;
  flex-shrink: 0;
}
.notice-header__back {
  margin-right: 8px;
}
.notice-header__title {
  font-size: 16px;
  font-weight: 500;
}
.notice-page--staff .notice-header__title {
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.02em;
}
.notice-body {
  padding: 0;
  max-width: 800px;
}
.notice-item {
  padding: 16px 16px 14px;
  margin-bottom: 12px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--el-border-color-lighter);
  transition: box-shadow 0.2s;
}
.notice-page--staff .notice-item {
  border: 1px solid rgba(13, 148, 136, 0.1);
  background: linear-gradient(165deg, #fff 0%, #f8fafc 100%);
  box-shadow: 0 4px 16px rgba(15, 23, 42, 0.05);
}
.notice-page--staff .notice-item:hover {
  box-shadow: 0 6px 20px rgba(13, 148, 136, 0.08);
}
.notice-item__title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}
.notice-page--staff .notice-item__title {
  display: flex;
  align-items: center;
  gap: 8px;
}
.notice-page--staff .notice-item__title::before {
  content: '';
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, #14b8a6, #0d9488);
  flex-shrink: 0;
}
.notice-item__content {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
.notice-item__time {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
.notice-empty {
  text-align: center;
  padding: 48px 16px;
  color: var(--el-text-color-placeholder);
}
</style>
