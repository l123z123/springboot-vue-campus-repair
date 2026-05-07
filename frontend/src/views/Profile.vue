<template>
  <div
    class="profile-page"
    @touchstart="onPullStart"
    @touchmove="onPullMove"
    @touchend="onPullEnd"
  >
    <!-- 1. 顶部个人信息区 -->
    <header v-if="!loading" class="profile-header">
      <el-avatar :size="70" :src="userAvatarUrl" class="profile-header__avatar profile-header__clickable" @click="goProfileEdit">
        <el-icon><UserFilled /></el-icon>
      </el-avatar>
      <div class="profile-header__info">
        <div class="profile-header__name profile-header__clickable" @click="goProfileEdit">{{ userInfo.name }}</div>
        <div class="profile-header__identity">{{ userInfo.department }} | 学号 {{ userInfo.studentId }}</div>
        <div v-if="userInfo.signature" class="profile-header__signature">{{ userInfo.signature }}</div>
      </div>
    </header>
    <header v-else class="profile-header profile-header--skeleton">
      <el-skeleton :rows="0" animated>
        <template #template>
          <el-skeleton-item variant="circle" style="width: 70px; height: 70px" />
        </template>
      </el-skeleton>
      <div class="profile-header__info" style="flex: 1">
        <el-skeleton animated :rows="3" />
      </div>
    </header>

    <!-- 2. 核心业务区：我的报修记录 -->
    <section ref="recordsSectionRef" class="records-section">
      <div class="pull-refresh__tip" :class="{ 'is-visible': pullState }">
        <el-icon v-if="pullState === 'refreshing'" class="is-loading"><Loading /></el-icon>
        <span>{{ pullTip }}</span>
      </div>
      <div class="records-section__head">
        <h2 class="records-section__title records-section__clickable" @click="scrollToRecords">我的报修记录</h2>
        <span class="records-section__actions">
          <el-button type="primary" link :loading="listLoading" @click="onManualRefresh">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
          <span class="records-section__more" @click="scrollToRecords">查看全部</span>
        </span>
      </div>
      <el-input
        v-model="searchKeyword"
        class="records-section__search"
        placeholder="搜索地点或故障描述..."
        clearable
        @change="onSearch"
        @clear="onSearch"
      />
      <el-tabs v-model="activeTab" class="records-tabs">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="待处理" name="pending" />
        <el-tab-pane label="进行中" name="processing" />
        <el-tab-pane label="已完成" name="done" />
        <el-tab-pane label="已关闭" name="closed" />
      </el-tabs>
      <div v-if="loading" class="records-list">
        <el-skeleton v-for="i in 4" :key="i" animated class="record-skeleton">
          <template #template>
            <div style="display: flex; gap: 12px; padding: 16px">
              <el-skeleton-item variant="image" style="width: 60px; height: 60px; border-radius: 8px" />
              <div style="flex: 1">
                <el-skeleton-item variant="text" style="width: 40%" />
                <el-skeleton-item variant="text" style="width: 90%; margin-top: 8px" />
                <el-skeleton-item variant="text" style="width: 30%; margin-top: 8px" />
              </div>
            </div>
          </template>
        </el-skeleton>
      </div>
      <div v-else class="records-list">
        <transition name="fade" mode="out-in">
          <div v-if="repairRecords.length" :key="activeTab" class="records-list-inner">
            <transition-group name="fadeInUp" tag="div" class="records-list-group">
              <div
                v-for="(item, index) in repairRecords"
                :key="item.id"
                class="record-item"
                :class="{ 'record-item--last': index === repairRecords.length - 1 }"
                @click="goOrderDetail(item.id)"
              >
                <div class="record-item__thumb">
                  <el-image v-if="item.image" :src="item.image" fit="cover" />
                  <el-icon v-else class="record-item__placeholder"><Picture /></el-icon>
                </div>
                <div class="record-item__body">
                  <div class="record-item__location">{{ item.location }}</div>
                  <p class="record-item__desc">{{ item.description }}</p>
                  <div class="record-item__meta">
                    <span class="record-item__time">{{ item.date }}</span>
                    <span class="record-item__urgency">{{ urgencyIcon(item.urgency) }} {{ urgencyLabel(item.urgency) }}</span>
                  </div>
                </div>
                <div class="record-item__tag" @click.stop="activeTab = item.status">
                  <el-tag :type="statusTagType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
                </div>
              </div>
            </transition-group>
          </div>
          <EmptyState
            v-else
            key="empty"
            :icon="DocumentRemove"
            text="暂无报修记录"
            button-text="去报修"
            button-to="/repair/create"
          />
        </transition>
      </div>
      <div v-if="repairRecords.length" class="records-list__footer">
        <el-button v-if="!noMore" text :loading="listLoading" @click="onLoadMore">加载更多</el-button>
        <span v-else class="records-list__no-more">没有更多了</span>
      </div>
    </section>

    <!-- 底部功能菜单 -->
    <div class="profile-footer">
      <div class="profile-footer__item" @click="onSetting">
        <span>⚙️ 设置</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="profile-footer__item" @click="onHelp">
        <span>❓ 帮助与反馈</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="profile-footer__item profile-footer__item--danger" @click="handleLogout">
        <span>🚪 退出登录</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UserFilled, Picture, ArrowRight, DocumentRemove, Loading, Refresh } from '@element-plus/icons-vue'
import EmptyState from '@/components/EmptyState.vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo as fetchUserInfo } from '@/api/user'
import { getRepairList } from '@/api/repair'

defineOptions({ name: 'Profile' })

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const recordsSectionRef = ref(null)

const userInfo = ref({
  name: '',
  studentId: '',
  department: '',
  avatar: '',
  avatarUrl: '',
  signature: ''
})

const activeTab = ref('all')
const repairRecords = ref([])
const loading = ref(true)
const listLoading = ref(false)
const page = ref(1)
const pageSize = 10
const noMore = ref(false)
const searchKeyword = ref('')

// 下拉刷新
const pullState = ref('') // '' | 'pulling' | 'refreshing'
const pullStartY = ref(0)
const pullTip = ref('下拉刷新')

const userAvatarUrl = computed(() => userInfo.value.avatarUrl || userInfo.value.avatar || '')
function statusTagType(status) {
  const map = { pending: 'primary', processing: 'warning', done: 'success', cancelled: 'info', closed: 'info', evaluated: 'success' }
  return map[status] ?? 'info'
}

function statusLabel(status) {
  const map = {
    pending: '待处理',
    audit: '审核中',
    audited: '已审核',
    dispatch: '待派单',
    dispatched: '已派单',
    processing: '维修中',
    done: '维修完成',
    confirmed: '学生确认',
    closed: '已评价',
    rejected: '已拒绝',
    cancelled: '已取消',
    completed: '已完成',
    evaluated: '已评价'
  }
  return map[status] || status
}

function urgencyIcon(urgency) {
  const map = { high: '🔴', medium: '🟠', low: '🟢' }
  return map[urgency] || ''
}

function urgencyLabel(urgency) {
  const map = { high: '高', medium: '中', low: '低' }
  return map[urgency] || ''
}

function goProfileEdit() {
  router.push('/profile/edit')
}

function scrollToRecords() {
  const el = recordsSectionRef.value
  if (el && typeof el.scrollIntoView === 'function') el.scrollIntoView({ behavior: 'smooth' })
}

function goOrderDetail(id) {
  router.push(`/repair/detail/${id}`)
}

function goRepairCreate() {
  router.push('/repair/create')
}

function onSetting() {
  router.push('/settings')
}

function onHelp() {
  router.push('/help')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await userStore.logout()
  ElMessage.success('退出成功')
}

async function loadUserInfo() {
  try {
    const data = await fetchUserInfo()
    userInfo.value = {
      name: data.nickname || data.realName || data.username || '',
      studentId: data.username || '',
      department: data.department || '',
      avatar: data.avatar || '',
      avatarUrl: data.avatarUrl || '',
      signature: data.signature && data.signature !== '暂无签名' ? data.signature : ''
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取用户信息失败')
    userInfo.value = { name: '', studentId: '', department: '', avatar: '', avatarUrl: '', signature: '' }
  }
}

/** 与后端 RepairOrderStatus 对齐：多状态用 statusIn */
const TAB_STATUS_IN = {
  pending: '0,1,11',
  processing: '2,3,4,5,12',
  done: '6,7,8',
  closed: '9,10'
}

async function loadRepairList(reset = false) {
  const tab = activeTab.value
  const statusIn = tab === 'all' ? undefined : TAB_STATUS_IN[tab]
  if (reset) {
    page.value = 1
    noMore.value = false
    repairRecords.value = []
  }
  try {
    const list = await getRepairList({
      statusIn,
      page: page.value,
      size: pageSize,
      keyword: searchKeyword.value || undefined
    })
    const arr = Array.isArray(list) ? list : (list?.records ?? list?.list ?? [])
    if (reset) {
      repairRecords.value = arr
    } else {
      repairRecords.value = repairRecords.value.concat(arr)
    }
    if (arr.length < pageSize) {
      noMore.value = true
    } else {
      page.value += 1
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取报修列表失败')
    repairRecords.value = []
  }
}

async function onManualRefresh() {
  listLoading.value = true
  await loadRepairList(true).finally(() => { listLoading.value = false })
  ElMessage.success('已刷新')
}

async function onLoadMore() {
  if (noMore.value || listLoading.value) return
  listLoading.value = true
  await loadRepairList(false).finally(() => { listLoading.value = false })
}

async function onSearch() {
  listLoading.value = true
  await loadRepairList(true).finally(() => { listLoading.value = false })
}

function isAtTop() {
  return (typeof window !== 'undefined' && (window.scrollY || document.documentElement.scrollTop) <= 2)
}
function onPullStart(e) {
  if (pullState.value === 'refreshing') return
  if (!isAtTop()) return
  pullStartY.value = e.touches[0].clientY
}
function onPullMove(e) {
  if (pullState.value === 'refreshing') return
  if (!isAtTop()) { pullState.value = ''; return }
  const delta = e.touches[0].clientY - pullStartY.value
  if (delta > 40) {
    pullState.value = 'pulling'
    pullTip.value = delta > 80 ? '释放刷新' : '下拉刷新'
  }
}
function onPullEnd(e) {
  if (pullState.value !== 'pulling') return
  const delta = e.changedTouches[0].clientY - pullStartY.value
  if (delta > 80) {
    pullState.value = 'refreshing'
    pullTip.value = '刷新中...'
    loadRepairList(true).then(() => {
      pullState.value = ''
      pullTip.value = '下拉刷新'
      ElMessage.success('已刷新')
    }).catch(() => {
      pullState.value = ''
      pullTip.value = '下拉刷新'
    })
  } else {
    pullState.value = ''
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([loadUserInfo(), loadRepairList(true)])
  } finally {
    loading.value = false
  }
  if (route.query.scroll === 'records') {
    setTimeout(() => scrollToRecords(), 100)
  }
})

// 从编辑页返回时重新拉取用户信息（Profile 被 keep-alive 缓存，onMounted 不会再次触发）
onActivated(() => {
  if (!loading.value) {
    loadUserInfo()
  }
})

watch(activeTab, () => {
  if (loading.value) return
  listLoading.value = true
  loadRepairList(true).finally(() => { listLoading.value = false })
})
</script>

<style scoped>
.profile-page {
  min-height: 400px;
  padding-bottom: 24px;
  background: var(--el-fill-color-lighter);
}

/* 1. 顶部个人信息区 */
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 40px 20px 30px;
  background: linear-gradient(135deg, #5b86e5 0%, #36d1dc 100%);
  border-bottom-left-radius: 24px;
  border-bottom-right-radius: 24px;
}
.profile-header__avatar {
  flex-shrink: 0;
  border: 4px solid white;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}
.profile-header__info {
  flex: 1;
  min-width: 0;
}
.profile-header__name {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  margin-bottom: 6px;
}
.profile-header__identity {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: 4px;
}
.profile-header__signature {
  font-size: 13px;
  color: #fff;
  margin-top: 4px;
}
.profile-header--skeleton {
  display: flex;
  align-items: center;
  gap: 20px;
}
.profile-header--skeleton .el-skeleton {
  flex-shrink: 0;
}
.record-skeleton {
  border-bottom: 1px solid #f0f0f0;
}
.record-skeleton:last-child {
  border-bottom: none;
}

/* 下拉刷新提示 */
.pull-refresh__tip {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translate(-50%, -100%);
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  color: var(--el-color-primary);
  background: #fff;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 100;
  transition: transform 0.2s;
}
.pull-refresh__tip.is-visible {
  transform: translate(-50%, 0);
}
.pull-refresh__tip .is-loading {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 2. 核心业务区 */
.records-section {
  margin: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.records-section__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.records-section__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.records-section__search {
  margin: 8px 0 12px 0;
}
.records-section__title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #000;
}
.records-section__more {
  font-size: 14px;
  color: #334155;
  cursor: pointer;
  min-height: 44px;
  display: inline-flex;
  align-items: center;
}
.records-section__clickable {
  cursor: pointer;
}
.profile-header__clickable {
  cursor: pointer;
}

/* el-tabs：选中蓝色+下划线，未选中灰色 */
.records-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}
.records-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.records-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  color: #475569;
}
.records-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}
.records-tabs :deep(.el-tabs__ink-bar) {
  background: var(--el-color-primary);
}
.records-tabs :deep(.el-tabs__active-bar) {
  background: var(--el-color-primary);
}
.records-tabs :deep(.el-tabs__nav) {
  border: none;
}
.records-tabs :deep(.el-tabs__item:hover) {
  color: var(--el-color-primary);
}
.records-tabs :deep(.el-tabs__content) {
  display: none;
}

.records-list {
  margin: 0 -20px;
}
.records-list-inner {
  display: block;
}
.records-list-group {
  display: block;
}
.records-list__footer {
  margin-top: 8px;
  text-align: center;
}
.records-list__no-more {
  font-size: 12px;
  color: #999;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
.fadeInUp-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fadeInUp-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.list-enter-active,
.list-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, background 0.15s ease;
}
.record-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.record-item:active {
  background: #f5f5f5;
}
.record-item--last,
.record-item:last-child {
  border-bottom: none;
}
.record-item__thumb {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}
.record-item__thumb .el-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.record-item__placeholder {
  font-size: 24px;
  color: var(--el-text-color-placeholder);
}
.record-item__body {
  flex: 1;
  min-width: 0;
}
.record-item__location {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 4px;
}
.record-item__desc {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.record-item__meta {
  font-size: 12px;
  color: #999;
}
.record-item__time {
  margin-right: 8px;
}
.record-item__tag {
  flex-shrink: 0;
}

/* 底部功能菜单 */
.profile-footer {
  margin: 24px 16px 0;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}
.profile-footer__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 50px;
  padding: 0 16px;
  font-size: 15px;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background 0.15s;
}
.profile-footer__item:last-child {
  border-bottom: none;
}
.profile-footer__item:hover {
  background: var(--el-fill-color-lighter);
}
.profile-footer__item:active {
  background: var(--el-fill-color);
}
.profile-footer__item--danger {
  color: var(--el-color-danger);
}
.profile-footer__item .el-icon {
  font-size: 16px;
  color: var(--el-text-color-placeholder);
}
.profile-footer__item--danger .el-icon {
  color: var(--el-color-danger);
}

@media (min-width: 960px) {
  .profile-page {
    display: grid;
    grid-template-columns: minmax(280px, 0.42fr) minmax(0, 1fr);
    gap: 22px;
    min-height: auto;
    padding: 0;
    background: transparent;
  }

  .profile-header,
  .profile-header--skeleton {
    align-self: start;
    min-height: 260px;
    flex-direction: column;
    align-items: flex-start;
    justify-content: center;
    padding: 30px;
    border-radius: 24px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.1);
  }

  .profile-header__avatar {
    --el-avatar-size: 84px;
  }

  .profile-header__name {
    font-size: 26px;
  }

  .records-section {
    grid-column: 2;
    grid-row: 1 / span 2;
    margin: 0;
    padding: 26px;
    border-radius: 24px;
    border: 1px solid rgba(148, 163, 184, 0.18);
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .records-section__head {
    align-items: center;
  }

  .records-section__title {
    font-size: 22px;
  }

  .records-list-group {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .record-item {
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 16px;
    padding: 14px;
    background: #fff;
  }

  .record-item--last {
    border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  }

  .record-item__desc {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .profile-footer {
    align-self: start;
    margin: 0;
    border-radius: 20px;
    border: 1px solid rgba(148, 163, 184, 0.18);
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .pull-refresh__tip {
    display: none;
  }
}
</style>
