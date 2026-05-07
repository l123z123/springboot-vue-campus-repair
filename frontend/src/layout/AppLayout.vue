<template>
  <div class="app-layout" :class="{ 'app-layout--staff': isStaff }">
    <header class="app-header">
      <div class="app-header__left">
        <el-button class="collapse-btn" text @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon><component :is="sidebarCollapsed ? Expand : Fold" /></el-icon>
        </el-button>
        <span class="app-header__title">校园报修管理系统</span>
        <el-tag v-if="isStaff" size="small" effect="plain" type="success">维修工</el-tag>
        <el-tag v-else size="small" effect="plain" type="primary">学生</el-tag>
      </div>
      <div class="app-header__right">
        <el-popover placement="bottom-end" :width="360" trigger="click" @show="systemNotifyStore.markAllRead()">
          <template #reference>
            <el-badge :value="notifyUnread" :max="99" :hidden="notifyUnread === 0" class="notice-badge">
              <el-button text class="header-btn" aria-label="系统通知">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </template>
          <div class="notify-popover">
            <div class="notify-popover__head">
              <span>实时通知</span>
              <el-button v-if="notifyList.length" link type="primary" size="small" @click="systemNotifyStore.markAllRead()">全部已读</el-button>
            </div>
            <el-scrollbar v-if="notifyList.length" max-height="300px">
              <div v-for="it in notifyList" :key="it.id" class="notify-item" :class="{ 'is-unread': !it.read }" @click="onNotifyClick(it)">
                <div class="notify-item__title">{{ it.title }}</div>
                <div v-if="it.summary" class="notify-item__summary">{{ it.summary }}</div>
                <div class="notify-item__time">{{ it.time }}</div>
              </div>
            </el-scrollbar>
            <el-empty v-else description="暂无通知" :image-size="64" />
          </div>
        </el-popover>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="user-dropdown">
            <el-avatar :size="32">{{ userInitial }}</el-avatar>
            <span class="user-name">{{ userDisplayName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="app-body">
      <aside class="app-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <el-menu
          :default-active="route.path"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          router
        >
          <template v-if="isStaff">
            <el-menu-item index="/staff/workbench">
              <el-icon><HomeFilled /></el-icon>
              <template #title>工作台</template>
            </el-menu-item>
            <el-menu-item index="/staff/tickets">
              <el-icon><Tickets /></el-icon>
              <template #title>工单管理</template>
            </el-menu-item>
            <el-menu-item index="/staff/message">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>消息中心</template>
            </el-menu-item>
            <el-menu-item index="/staff/notice">
              <el-icon><Bell /></el-icon>
              <template #title>公告通知</template>
            </el-menu-item>
            <el-menu-item index="/help">
              <el-icon><QuestionFilled /></el-icon>
              <template #title>帮助反馈</template>
            </el-menu-item>
          </template>
          <template v-else>
            <el-menu-item index="/home">
              <el-icon><HomeFilled /></el-icon>
              <template #title>首页</template>
            </el-menu-item>
            <el-menu-item index="/publish">
              <el-icon><UploadFilled /></el-icon>
              <template #title>发布报修</template>
            </el-menu-item>
            <el-menu-item index="/orders">
              <el-icon><Tickets /></el-icon>
              <template #title>工单管理</template>
            </el-menu-item>
            <el-menu-item index="/message">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>消息中心</template>
            </el-menu-item>
            <el-menu-item index="/notice">
              <el-icon><Bell /></el-icon>
              <template #title>公告通知</template>
            </el-menu-item>
            <el-menu-item index="/help">
              <el-icon><QuestionFilled /></el-icon>
              <template #title>帮助反馈</template>
            </el-menu-item>
          </template>
        </el-menu>
      </aside>

      <main class="app-main">
        <div class="app-main__inner">
          <el-breadcrumb v-if="pageTitle" separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
          <div class="app-content">
            <router-view v-slot="{ Component }">
              <transition name="fade" mode="out-in">
                <keep-alive :include="['Home', 'OrderManage', 'StaffTickets', 'Message', 'Profile', 'StaffWorkbench']">
                  <component :is="Component" />
                </keep-alive>
              </transition>
            </router-view>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, ArrowDown, HomeFilled, UploadFilled, ChatDotRound, User, QuestionFilled, Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSystemNotifyStore } from '@/stores/systemNotify'
import { useWebSocket } from '@/composables/useWebSocket'

defineOptions({ name: 'AppLayout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemNotifyStore = useSystemNotifyStore()

const sidebarCollapsed = ref(false)
let disconnectWs = () => {}

const isStaff = computed(() => (userStore.userInfo?.role ?? 0) === 1)
const notifyUnread = computed(() => systemNotifyStore.unreadCount)
const notifyList = computed(() => systemNotifyStore.items.slice(0, 50))

const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.nickname || u.username)) || (isStaff.value ? '维修工' : '学生')
})
const userInitial = computed(() => userDisplayName.value.slice(0, 1).toUpperCase())

const pageTitle = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  if (matched.length) return matched[matched.length - 1].meta.title
  return route.meta?.title || ''
})

function handleUserCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'settings') {
    router.push('/settings')
  }
}

function onNotifyClick(it) {
  if (it?.orderId) {
    router.push({ name: 'OrderDetail', params: { id: String(it.orderId) } })
  }
  systemNotifyStore.markIdRead(it.id)
}

function checkCollapse() {
  sidebarCollapsed.value = window.innerWidth < 1200
}

onMounted(() => {
  checkCollapse()
  window.addEventListener('resize', checkCollapse)
  if (userStore.token) {
    const { connect } = useWebSocket()
    disconnectWs = connect()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkCollapse)
  disconnectWs()
})
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.app-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
  z-index: 100;
}

.app-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-header__title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.header-btn { font-size: 20px; }
.collapse-btn { font-size: 20px; color: #606266; }

.app-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-dropdown:hover { background: #f5f7fa; }
.user-name { font-size: 14px; color: #606266; max-width: 100px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.app-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.app-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #304156;
  transition: width 0.28s;
  overflow: hidden;
}
.app-sidebar.is-collapsed { width: 64px; }
.app-sidebar :deep(.el-menu) { border-right: none; }
.app-sidebar :deep(.el-menu-item.is-active) { background-color: rgba(64, 158, 255, 0.1) !important; }

.app-main {
  flex: 1;
  overflow: auto;
  background: #f5f7fa;
}

.app-main__inner {
  padding: 20px;
  min-height: 100%;
}

.breadcrumb {
  margin-bottom: 16px;
}

.app-content {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: calc(100vh - 200px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

/* 通知弹窗 */
.notify-popover { padding-right: 4px; }
.notify-popover__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}
.notify-item {
  padding: 10px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}
.notify-item:hover { background: #f8fafc; }
.notify-item.is-unread { background: rgba(64, 158, 255, 0.05); }
.notify-item.is-unread .notify-item__title { font-weight: 700; }
.notify-item:last-child { border-bottom: none; }
.notify-item__title { font-size: 13px; color: #303133; }
.notify-item__summary { font-size: 12px; color: #909399; margin-top: 4px; }
.notify-item__time { font-size: 11px; color: #c0c4cc; margin-top: 4px; }

/* transition */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
