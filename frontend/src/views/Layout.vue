<template>
  <div class="layout">
    <main class="layout__main">
      <router-view v-slot="{ Component }">
        <keep-alive :include="keepAliveNames">
          <component :is="Component" />
        </keep-alive>
      </router-view>
    </main>
    <nav v-if="showBottomNav" class="bottom-nav">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="bottom-nav__item"
        :class="{ 'is-active': isActive(tab.path) }"
        @click="go(tab.path)"
      >
        <el-icon><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, UploadFilled, ChatDotRound, User } from '@element-plus/icons-vue'
import { useWebSocket } from '@/composables/useWebSocket'

const route = useRoute()
const router = useRouter()
let disconnect = () => {}

onMounted(() => {
  if (localStorage.getItem('token')) {
    const { connect } = useWebSocket()
    disconnect = connect()
  }
})
onUnmounted(() => {
  disconnect()
})

const tabs = [
  { path: '/home', label: '首页', icon: HomeFilled },
  { path: '/publish', label: '报修', icon: UploadFilled },
  { path: '/message', label: '消息', icon: ChatDotRound },
  { path: '/profile', label: '我的', icon: User }
]

const showBottomNav = computed(() => route.meta.showBottomNav !== false)

const keepAliveNames = ['Home', 'Publish', 'Message', 'Profile']

function isActive(path) {
  if (path === '/message') return route.path === '/message'
  return route.path === path
}

function go(path) {
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  padding-bottom: 60px;
  background: var(--el-fill-color-lighter);
}
.layout__main {
  min-height: 100vh;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 56px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  z-index: 999;
}
.bottom-nav__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.2s;
}
.bottom-nav__item:hover,
.bottom-nav__item.is-active {
  color: var(--el-color-primary);
}
.bottom-nav__item .el-icon {
  font-size: 22px;
}
</style>
