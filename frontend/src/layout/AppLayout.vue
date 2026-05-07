<template>
  <div class="layout" :class="{ 'layout--staff': isStaff }">
    <aside v-if="showBottomNav" class="desktop-shell">
      <div class="desktop-shell__brand">
        <div class="desktop-shell__logo">
          <el-icon><House /></el-icon>
        </div>
        <div>
          <strong>校园报修</strong>
          <span>{{ isStaff ? '维修工工作台' : '学生服务端' }}</span>
        </div>
      </div>
      <nav class="desktop-shell__nav" aria-label="桌面端导航">
        <button
          v-for="tab in tabs"
          :key="tab.path"
          type="button"
          class="desktop-shell__nav-item"
          :class="{ 'is-active': isActive(tab.path) }"
          @click="go(tab.path)"
        >
          <el-badge
            v-if="isMessageTab(tab.path) && chatUnreadTotal > 0"
            :value="chatUnreadTotal > 99 ? '99+' : chatUnreadTotal"
            :max="99"
            class="desktop-shell__badge"
          >
            <el-icon><component :is="tab.icon" /></el-icon>
          </el-badge>
          <el-icon v-else><component :is="tab.icon" /></el-icon>
          <span>{{ tab.label }}</span>
        </button>
      </nav>
      <div class="desktop-shell__profile">
        <el-avatar :size="36">{{ userInitial }}</el-avatar>
        <div>
          <strong>{{ userDisplayName }}</strong>
          <span>{{ isStaff ? '维修工' : '学生' }}</span>
        </div>
      </div>
    </aside>
    <section class="layout__content">
      <header v-if="showBottomNav" class="desktop-topbar">
        <div class="desktop-topbar__filler" aria-hidden="true" />
        <el-button type="primary" plain round @click="quickAction">
          {{ isStaff ? '刷新工单' : '发布报修' }}
        </el-button>
      </header>
      <main class="layout__main">
        <router-view v-slot="{ Component }">
          <keep-alive :include="keepAliveNames">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </main>
    </section>
    <nav v-if="showBottomNav" class="bottom-nav">
      <div
        v-for="tab in tabs"
        :key="tab.path"
        class="bottom-nav__item"
        :class="{ 'is-active': isActive(tab.path) }"
        @click="go(tab.path)"
      >
        <el-badge
          v-if="isMessageTab(tab.path) && chatUnreadTotal > 0"
          :value="chatUnreadTotal > 99 ? '99+' : chatUnreadTotal"
          :max="99"
          class="bottom-nav__badge"
        >
          <el-icon><component :is="tab.icon" /></el-icon>
        </el-badge>
        <el-icon v-else><component :is="tab.icon" /></el-icon>
        <span>{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { HomeFilled, UploadFilled, ChatDotRound, User, House } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'
import { useSystemNotifyStore } from '@/stores/systemNotify'

defineOptions({ name: 'AppLayout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemNotifyStore = useSystemNotifyStore()
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

const studentTabs = [
  { path: '/home', label: '首页', icon: HomeFilled },
  { path: '/publish', label: '发布', icon: UploadFilled },
  { path: '/message', label: '消息', icon: ChatDotRound },
  { path: '/profile', label: '我的', icon: User }
]

const staffTabs = [
  { path: '/staff/workbench', label: '工作台', icon: HomeFilled },
  { path: '/staff/message', label: '消息', icon: ChatDotRound },
  { path: '/staff/notice', label: '公告', icon: UploadFilled },
  { path: '/profile', label: '我的', icon: User }
]

const isStaff = computed(() => (userStore.userInfo?.role ?? 0) === 1)

const tabs = computed(() => {
  return isStaff.value ? staffTabs : studentTabs
})

const showBottomNav = computed(() => route.meta.showBottomNav !== false)
const chatUnreadTotal = computed(() => systemNotifyStore.chatUnreadTotal)
const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.nickname || u.username)) || (isStaff.value ? '维修工' : '学生')
})
const userInitial = computed(() => userDisplayName.value.slice(0, 1).toUpperCase())

const keepAliveNames = ['Home', 'Publish', 'Message', 'Profile', 'StaffWorkbench']

function isActive(path) {
  if (path === '/home' || path === '/staff/workbench') {
    return route.path === '/home' || route.path === '/staff/workbench'
  }
  return route.path === path || route.path.startsWith(path + '/')
}

function go(path) {
  if (route.path !== path) router.push(path)
}

function isMessageTab(path) {
  return path === '/message' || path === '/staff/message'
}

function quickAction() {
  if (isStaff.value) {
    router.replace('/staff/workbench')
    try {
      window.dispatchEvent(new Event('campus-repair-refresh-workbench'))
    } catch {
      // ignore
    }
    return
  }
  router.push('/publish')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  padding-bottom: 60px;
  background: var(--el-fill-color-lighter);
}
.layout--staff {
  background: linear-gradient(180deg, #f0fdfa 0%, #f1f5f9 18%, #f8fafc 100%);
}
.layout__main {
  min-height: 100vh;
}
.layout__content {
  min-width: 0;
}
.desktop-shell,
.desktop-topbar {
  display: none;
}

.bottom-nav {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-around;
  height: 58px;
  padding-bottom: env(safe-area-inset-bottom, 0);
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-top: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 -4px 20px rgba(15, 23, 42, 0.05);
  z-index: 999;
}
.layout--staff .bottom-nav {
  background: rgba(255, 255, 255, 0.92);
  border-top-color: rgba(13, 148, 136, 0.1);
}
.bottom-nav__item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 6px 12px;
  min-width: 58px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.2s;
  border-radius: 12px;
}
.bottom-nav__item:hover,
.bottom-nav__item.is-active {
  color: var(--el-color-primary);
}
.layout--staff .bottom-nav__item.is-active {
  color: #0d9488;
}
.layout--staff .bottom-nav__item.is-active::after {
  content: '';
  position: absolute;
  bottom: 4px;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 3px;
  border-radius: 99px;
  background: linear-gradient(90deg, #14b8a6, #0d9488);
}
.bottom-nav__item .el-icon {
  font-size: 22px;
  transition: transform 0.2s;
}
.bottom-nav__badge :deep(.el-badge__content) {
  border: none;
}
.bottom-nav__item.is-active .el-icon {
  transform: scale(1.06);
}
.layout--staff .bottom-nav__item.is-active .el-icon {
  color: #0d9488;
}

@media (min-width: 960px) {
  .layout {
    display: grid;
    grid-template-columns: 248px minmax(0, 1fr);
    min-height: 100vh;
    padding-bottom: 0;
    background: #eef2f7;
  }
  .layout--staff {
    background: #eef7f6;
  }
  .desktop-shell {
    position: sticky;
    top: 0;
    display: flex;
    flex-direction: column;
    height: 100vh;
    padding: 22px 16px;
    background: linear-gradient(180deg, #172554 0%, #1e3a8a 100%);
    color: #e2e8f0;
    box-shadow: 10px 0 30px rgba(15, 23, 42, 0.12);
    z-index: 30;
  }
  .layout--staff .desktop-shell {
    background: linear-gradient(180deg, #134e4a 0%, #0f766e 100%);
  }
  .desktop-shell__brand {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 8px 22px;
  }
  .desktop-shell__brand strong,
  .desktop-shell__profile strong {
    display: block;
    color: #fff;
    font-size: 16px;
  }
  .desktop-shell__brand span,
  .desktop-shell__profile span {
    display: block;
    margin-top: 2px;
    color: rgba(226, 232, 240, 0.74);
    font-size: 12px;
  }
  .desktop-shell__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 42px;
    height: 42px;
    border-radius: 14px;
    background: rgba(255, 255, 255, 0.14);
    font-size: 22px;
  }
  .desktop-shell__nav {
    display: grid;
    gap: 8px;
    margin-top: 10px;
  }
  .desktop-shell__nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
    border: 0;
    border-radius: 14px;
    padding: 12px 14px;
    background: transparent;
    color: rgba(226, 232, 240, 0.82);
    font-size: 14px;
    cursor: pointer;
    text-align: left;
    transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
  }
  .desktop-shell__nav-item:hover,
  .desktop-shell__nav-item.is-active {
    color: #fff;
    background: rgba(255, 255, 255, 0.14);
    transform: translateX(2px);
  }
  .desktop-shell__nav-item .el-icon {
    font-size: 19px;
  }
  .desktop-shell__profile {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: auto;
    padding: 14px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.12);
  }
  .layout__content {
    min-height: 100vh;
    padding: 24px;
    overflow: hidden;
  }
  .desktop-topbar {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 16px;
    max-width: 1240px;
    margin: 0 auto 18px;
    padding: 14px 22px;
    background: rgba(255, 255, 255, 0.88);
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 20px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
    backdrop-filter: blur(12px);
  }
  .desktop-topbar__filler {
    flex: 1;
    min-height: 1px;
  }
  .layout__main {
    max-width: 1240px;
    min-height: auto;
    margin: 0 auto;
  }
  .bottom-nav {
    display: none;
  }
}
</style>
