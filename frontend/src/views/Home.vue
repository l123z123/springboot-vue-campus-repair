<template>
  <div class="home-page">
    <!-- 顶部欢迎区域 -->
    <header class="home-header">
      <div class="header-content">
        <div class="welcome-section">
          <div class="welcome-avatar">
            <div class="avatar-placeholder">
              <el-icon size="28"><User /></el-icon>
            </div>
          </div>
          <div class="welcome-text">
            <p class="welcome-greeting">你好</p>
            <p class="welcome-name">{{ userDisplayName }}</p>
            <p class="welcome-tip">今天有什么需要帮助的吗？</p>
          </div>
        </div>
      </div>
      
      <!-- 搜索框 -->
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索历史报修..."
          clearable
          class="home-search"
          size="large"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
    </header>

    <!-- 功能网格 -->
    <section class="home-section">
      <h3 class="section-title">常用功能</h3>
      <div class="home-grid">
        <el-tooltip
          v-for="item in gridEntries"
          :key="item.key"
          :content="item.tooltip"
          :disabled="!item.tooltip"
          placement="top"
          :show-after="200"
        >
          <div
            class="grid-item"
            :class="{ 
              'grid-item--disabled': item.disabled,
              'grid-item--urgent': item.key === 'urgent'
            }"
            @click="!item.disabled && item.click && item.click()"
          >
            <div class="grid-item__icon-wrapper">
              <div class="grid-item__icon">
                <el-icon><component :is="item.icon" /></el-icon>
              </div>
            </div>
            <span class="grid-item__label">{{ item.label }}</span>
          </div>
        </el-tooltip>
      </div>
    </section>

    <!-- 快捷入口底部装饰 -->
    <div class="home-footer">
      <p class="footer-text">校园报修管理系统</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Lightning, Calendar, Bell, QuestionFilled, DataAnalysis, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useAuth } from '@/composables/useAuth'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'Home' })

const router = useRouter()
const userStore = useUserStore()
const { hasRole } = useAuth()

const searchKeyword = ref('')

const userDisplayName = computed(() => {
  const u = userStore.userInfo
  return (u && (u.realName || u.username)) || '用户'
})

const isAdmin = computed(() => hasRole('ADMIN'))

const handleAdminBlocked = () => {
  ElMessage.info('您是管理员，请前往【数据看板】或后台处理报修')
}

const gridEntries = computed(() => {
  const base = [
    {
      key: 'urgent',
      label: '一键急修',
      icon: Lightning,
      disabled: isAdmin.value,
      tooltip: isAdmin.value ? '管理员无需提交报修' : '紧急情况专用，快速上报',
      click: () => {
        if (isAdmin.value) {
          handleAdminBlocked()
          return
        }
        router.push({ path: '/publish', query: { urgent: 'true' } })
      }
    },
    {
      key: 'book',
      label: '预约维修',
      icon: Calendar,
      disabled: isAdmin.value,
      tooltip: isAdmin.value ? '管理员无需提交报修' : '预约时间上门服务',
      click: () => {
        if (isAdmin.value) {
          handleAdminBlocked()
          return
        }
        // 学生/维修工走预约页面，与一键急修区分
        router.push('/booking')
      }
    },
    { key: 'notice', label: '公告通知', icon: Bell, disabled: false, tooltip: '', click: () => router.push('/notice') },
    { key: 'help', label: '帮助中心', icon: QuestionFilled, disabled: false, tooltip: '', click: () => router.push('/help') }
  ]
  if (hasRole('ADMIN')) {
    base.push({
      key: 'dashboard',
      label: '数据看板',
      icon: DataAnalysis,
      disabled: false,
      tooltip: '',
      click: () => router.push('/admin/dashboard')
    })
    base.push({
      key: 'feedback-manage',
      label: '反馈管理',
      icon: DataAnalysis,
      disabled: false,
      tooltip: '',
      click: () => router.push('/admin/feedback')
    })
  }
  return base
})
</script>

<style scoped>
.home-page {
  padding: 24px 16px 80px 16px;
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

/* 顶部头部区域 */
.home-header {
  margin-bottom: 28px;
}

.header-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 20px;
  padding: 24px 20px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
  margin-bottom: 16px;
}

.welcome-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.welcome-avatar {
  flex-shrink: 0;
}

.avatar-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.welcome-text {
  flex: 1;
  color: white;
}

.welcome-greeting {
  margin: 0;
  font-size: 14px;
  opacity: 1;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.96);
}

.welcome-name {
  margin: 4px 0 0 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.welcome-tip {
  margin: 6px 0 0 0;
  font-size: 13px;
  opacity: 1;
  color: rgba(255, 255, 255, 0.92);
  font-weight: 400;
}

/* 搜索框 */
.search-section {
  margin-top: 0;
}

.home-search {
  width: 100%;
}

.home-search :deep(.el-input__wrapper) {
  border-radius: 14px;
  background: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.home-search :deep(.el-input__wrapper:hover) {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.home-search :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.25);
}

/* 功能区域 */
.home-section {
  margin-top: 24px;
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  padding-left: 4px;
  border-left: 4px solid var(--el-color-primary);
  border-radius: 2px;
}

.home-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* 功能卡片 */
.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 12px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.grid-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, var(--el-color-primary-light-5), var(--el-color-primary));
  transform: scaleX(0);
  transition: transform 0.35s ease;
  transform-origin: left;
}

.grid-item:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}

.grid-item:hover::before {
  transform: scaleX(1);
}

.grid-item:active {
  transform: translateY(-2px) scale(0.98);
}

.grid-item__icon-wrapper {
  position: relative;
  z-index: 1;
}

.grid-item__icon {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--el-color-primary-light-8), var(--el-color-primary-light-6));
  color: var(--el-color-primary);
  border-radius: 16px;
  font-size: 26px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
  transition: all 0.35s ease;
}

.grid-item:hover .grid-item__icon {
  transform: scale(1.1) rotate(5deg);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.25);
}

.grid-item__label {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  text-align: center;
  letter-spacing: 0.3px;
}

/* 一键急修特殊样式 */
.grid-item--urgent .grid-item__icon {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  color: white;
  box-shadow: 0 4px 12px rgba(238, 90, 36, 0.3);
}

.grid-item--urgent:hover .grid-item__icon {
  box-shadow: 0 6px 16px rgba(238, 90, 36, 0.45);
}

.grid-item--urgent::before {
  background: linear-gradient(90deg, #ff6b6b, #ee5a24);
}

/* 禁用状态 */
.grid-item--disabled {
  cursor: not-allowed;
  opacity: 0.55;
  background: var(--el-fill-color-light);
}

.grid-item--disabled:hover {
  transform: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.grid-item--disabled:hover::before {
  transform: scaleX(0);
}

.grid-item--disabled .grid-item__icon {
  background: var(--el-fill-color);
  color: var(--el-text-color-placeholder);
  box-shadow: none;
}

.grid-item--disabled:hover .grid-item__icon {
  transform: none;
  box-shadow: none;
}

/* 底部装饰 */
.home-footer {
  margin-top: 40px;
  text-align: center;
}

.footer-text {
  margin: 0;
  font-size: 12px;
  color: #64748b;
  letter-spacing: 0.5px;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .home-page {
    padding: 20px 14px 80px 14px;
  }
  
  .home-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 12px;
  }
  
  .grid-item {
    padding: 20px 8px;
  }
  
  .grid-item__icon {
    width: 50px;
    height: 50px;
    font-size: 22px;
  }
  
  .header-content {
    padding: 20px 16px;
  }
  
  .avatar-placeholder {
    width: 52px;
    height: 52px;
  }
  
  .welcome-name {
    font-size: 20px;
  }
}

@media (max-width: 360px) {
  .home-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 960px) {
  .home-page {
    min-height: auto;
    padding: 0;
    background: transparent;
  }

  .home-header {
    display: grid;
    grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.65fr);
    gap: 18px;
    margin-bottom: 24px;
  }

  .header-content {
    min-height: 176px;
    margin: 0;
    padding: 30px;
    display: flex;
    align-items: center;
    border-radius: 24px;
  }

  .welcome-section {
    gap: 22px;
  }

  .avatar-placeholder {
    width: 76px;
    height: 76px;
  }

  .welcome-greeting {
    font-size: 15px;
  }

  .welcome-name {
    font-size: 30px;
  }

  .welcome-tip {
    font-size: 14px;
  }

  .search-section {
    min-height: 176px;
    display: flex;
    align-items: center;
    padding: 28px;
    background: #fff;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 24px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .home-section {
    margin-top: 0;
    padding: 26px;
    background: #fff;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 24px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .section-title {
    font-size: 18px;
    margin-bottom: 20px;
  }

  .home-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 18px;
  }

  .grid-item {
    min-height: 160px;
    justify-content: center;
    border: 1px solid rgba(148, 163, 184, 0.14);
  }

  .home-footer {
    margin-top: 24px;
  }
}
</style>
