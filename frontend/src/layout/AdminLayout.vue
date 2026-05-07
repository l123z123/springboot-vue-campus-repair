<template>
  <div class="admin-layout">
    <!-- 顶栏 60px -->
    <header class="admin-header">
      <div class="admin-header__left">
        <el-button
          class="collapse-btn"
          text
          @click="sidebarCollapsed = !sidebarCollapsed"
        >
          <el-icon><component :is="sidebarCollapsed ? Fold : Expand" /></el-icon>
        </el-button>
        <span class="admin-header__title">校园保修管理系统</span>
      </div>
      <div class="admin-header__right">
        <el-popover
          placement="bottom-end"
          :width="360"
          trigger="click"
          @show="onNotifyPopoverShow"
        >
          <template #reference>
            <el-badge
              :value="notifyUnread"
              :max="99"
              :hidden="notifyUnread === 0"
              class="notice-badge"
            >
              <el-button text class="header-btn" aria-label="系统通知">
                <el-icon><Bell /></el-icon>
              </el-button>
            </el-badge>
          </template>
          <div class="admin-notify-popover">
            <div class="admin-notify-popover__head">
              <span>实时系统通知</span>
              <el-button v-if="notifyList.length" link type="primary" size="small" @click="markNotifyRead"
                >全部已读
              </el-button>
            </div>
            <el-scrollbar v-if="notifyList.length" max-height="300px">
              <div
                v-for="it in notifyList"
                :key="it.id"
                class="admin-notify-item"
                :class="{ 'is-unread': !it.read }"
                @click="onNotifyItemClick(it)"
              >
                <div class="admin-notify-item__top">
                  <div class="admin-notify-item__t">{{ it.title }}</div>
                  <el-tag
                    v-if="it.status != null"
                    class="admin-notify-item__tag"
                    :type="notifyStatusTagType(it.status)"
                    size="small"
                  >
                    {{ notifyStatusText(it.status) }}
                  </el-tag>
                </div>
                <div v-if="it.orderId && !it.orderIds?.length" class="admin-notify-item__oid">工单号：#{{ it.orderId }}</div>
                <div
                  v-else-if="it.orderIds && it.orderIds.length"
                  class="admin-notify-item__oids"
                  @click.stop
                >
                  <span class="admin-notify-item__oids-label">涉及工单</span>
                  <div class="admin-notify-item__oid-chips">
                    <el-button
                      v-for="oid in it.orderIds.slice(0, 15)"
                      :key="oid"
                      type="primary"
                      link
                      size="small"
                      class="oid-chip"
                      @click="goOrderDetail(oid, it.id)"
                    >
                      #{{ formatOrderNo(oid) }}
                    </el-button>
                    <span v-if="it.orderIds.length > 15" class="oid-more"
                      >等共 {{ it.orderIds.length }} 单，可去工单管理筛选「待办」</span
                    >
                  </div>
                </div>
                <div class="admin-notify-item__s">{{ it.summary }}</div>
                <div class="admin-notify-item__m">{{ it.time }}</div>
              </div>
            </el-scrollbar>
            <el-empty v-else description="暂无通知（保持后台运行，有新工单/超时将推送）" :image-size="64" />
          </div>
        </el-popover>
        <el-dropdown trigger="click" @command="handleUserCommand">
          <div class="user-dropdown">
            <el-avatar :size="36" class="user-avatar" :src="userAvatarUrl">
              {{ userAvatarUrl ? '' : (userDisplayName?.charAt(0) || 'A') }}
            </el-avatar>
            <span class="user-name">{{ userDisplayName || '管理员' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="admin-body">
      <!-- 左侧栏 220px / 64px -->
      <aside
        class="admin-sidebar"
        :class="{ 'is-collapsed': sidebarCollapsed }"
      >
        <el-menu
          :default-active="route.path"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
          router
          @select="handleMenuSelect"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>首页概览</template>
          </el-menu-item>
          <el-menu-item index="/admin/tickets">
            <el-icon><List /></el-icon>
            <template #title>工单管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <template #title>用户管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/stats">
            <el-icon><PieChart /></el-icon>
            <template #title>数据统计</template>
          </el-menu-item>
          <el-menu-item index="/admin/feedback">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>反馈管理</template>
          </el-menu-item>
          <el-menu-item index="/admin/settings">
            <el-icon><Setting /></el-icon>
            <template #title>系统设置</template>
          </el-menu-item>
        </el-menu>
      </aside>

      <!-- 主内容区 -->
      <main class="admin-main">
        <div class="admin-main__inner">
          <el-breadcrumb v-if="breadcrumbs.length" separator="/" class="breadcrumb">
            <el-breadcrumb-item v-for="(item, i) in breadcrumbs" :key="i">
              <router-link v-if="i < breadcrumbs.length - 1" :to="item.path">
                {{ item.title }}
              </router-link>
              <span v-else>{{ item.title }}</span>
            </el-breadcrumb-item>
          </el-breadcrumb>
          <div class="admin-content">
            <router-view />
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Fold, Expand, Bell, ArrowDown, DataAnalysis, List, User, PieChart, ChatDotRound, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useSystemNotifyStore } from '@/stores/systemNotify'
import { useWebSocket } from '@/composables/useWebSocket'
import { normalizeImageUrl } from '@/utils/image'
import { getRepairStatusLabel } from '@/constants/repairStatus'

defineOptions({ name: 'AdminLayout' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const systemNotifyStore = useSystemNotifyStore()

const sidebarCollapsed = ref(false)
const notifyUnread = computed(() => systemNotifyStore.unreadCount)
const notifyList = computed(() => systemNotifyStore.items.slice(0, 50))

let disconnectWs = () => {}

const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.nickname || u.username)) || '管理员'
})

const userAvatarUrl = computed(() => {
  const u = userStore.userInfo
  const url = u?.avatarUrl ?? u?.avatar
  return url ? normalizeImageUrl(url) : ''
})

const breadcrumbs = computed(() => {
  const matched = route.matched.filter((r) => r.meta?.title)
  return matched.map((r, i) => {
    const fullPath = ('/' + matched.slice(0, i + 1).map((m) => m.path).join('/')).replace(/\/+/g, '/')
    return { path: fullPath, title: r.meta.title }
  })
})

function handleUserCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
  } else if (cmd === 'profile') {
    router.push('/admin/profile')
  }
}

function handleMenuSelect(path) {
  if (path && path !== route.path) {
    router.push(path)
  }
}

function checkMobile() {
  sidebarCollapsed.value = window.innerWidth < 768
}

function onNotifyPopoverShow() {
  systemNotifyStore.markAllRead()
}

function markNotifyRead() {
  systemNotifyStore.markAllRead()
}

function formatOrderNo(oid) {
  const s = String(oid)
  if (s.length > 10) {
    return `${s.slice(0, 4)}…${s.slice(-4)}`
  }
  return s
}

function goOrderDetail(oid, snId) {
  if (oid == null) return
  router.push({ name: 'RepairDetail', params: { id: String(oid) } })
  if (snId) {
    systemNotifyStore.markIdRead(snId)
  }
}

function onNotifyItemClick(it) {
  if (it?.orderIds?.length) {
    if (it.orderIds.length === 1) {
      goOrderDetail(it.orderIds[0], it.id)
    }
  } else if (it?.orderId != null) {
    goOrderDetail(it.orderId, it.id)
  }
  systemNotifyStore.markIdRead(it.id)
}

function notifyStatusText(status) {
  return getRepairStatusLabel(Number(status))
}

function notifyStatusTagType(status) {
  const s = Number(status)
  if ([8, 7, 6].includes(s)) return 'success'
  if ([9, 10].includes(s)) return 'danger'
  if ([11, 3, 4, 12].includes(s)) return 'warning'
  if ([5].includes(s)) return 'primary'
  return 'info'
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  if (userStore.token) {
    const { connect } = useWebSocket()
    disconnectWs = connect()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  disconnectWs()
})
</script>

<style lang="scss" scoped>
$header-height: 60px;
$sidebar-width: 220px;
$sidebar-collapsed: 64px;
$sidebar-bg: #304156;
$main-bg: #f5f7fa;

.admin-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.admin-header {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--el-border-color-lighter);
  flex-shrink: 0;
}

.admin-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-header__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.collapse-btn {
  font-size: 20px;
}

.admin-header__right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-btn {
  font-size: 18px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.user-avatar {
  background: var(--el-color-primary-light-5);
  color: var(--el-color-primary);
}

.user-name {
  font-size: 14px;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.admin-sidebar {
  width: $sidebar-width;
  flex-shrink: 0;
  background: $sidebar-bg;
  transition: width 0.28s;

  &.is-collapsed {
    width: $sidebar-collapsed;
  }

  :deep(.el-menu) {
    border-right: none;
  }

  :deep(.el-menu-item.is-active) {
    background-color: rgba(64, 158, 255, 0.1) !important;
  }
}

.admin-main {
  flex: 1;
  overflow: auto;
  background: $main-bg;
}

.admin-main__inner {
  padding: 20px;
  min-height: 100%;
}

.breadcrumb {
  margin-bottom: 16px;
}

.admin-content {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  min-height: 400px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

@media (max-width: 767px) {
  .admin-sidebar {
    width: $sidebar-collapsed;
  }

  .admin-sidebar:not(.is-collapsed) {
    width: $sidebar-width;
  }

  .user-name {
    display: none;
  }
}

.admin-notify-popover {
  padding-right: 4px;
  .admin-notify-popover__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    font-size: 15px;
    font-weight: 600;
  }
  .admin-notify-item {
    padding: 10px 10px 9px;
    border-bottom: 1px solid #edf2f7;
    cursor: pointer;
    font-size: 13px;
    border-radius: 10px;
    transition: all 0.2s ease;
    &:last-child {
      border-bottom: none;
    }
    &:hover {
      background: #f8fafc;
    }
    &.is-unread {
      background: linear-gradient(90deg, rgba(59, 130, 246, 0.08), rgba(99, 102, 241, 0.05));
      .admin-notify-item__t {
        font-weight: 700;
      }
    }
  }
  .admin-notify-item__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }
  .admin-notify-item__tag {
    flex-shrink: 0;
  }
  .admin-notify-item__t {
    color: var(--el-text-color-primary);
  }
  .admin-notify-item__oid {
    margin-top: 4px;
    color: #334155;
    font-size: 12px;
    font-weight: 600;
  }
  .admin-notify-item__oids {
    margin-top: 6px;
  }
  .admin-notify-item__oids-label {
    display: block;
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-bottom: 6px;
  }
  .admin-notify-item__oid-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 6px;
    align-items: center;
  }
  .oid-chip {
    padding: 0 2px;
  }
  .oid-more {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  .admin-notify-item__s {
    color: var(--el-text-color-secondary);
    margin-top: 4px;
    line-height: 1.4;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .admin-notify-item__m {
    color: var(--el-text-color-placeholder);
    font-size: 12px;
    margin-top: 4px;
  }
}
</style>
