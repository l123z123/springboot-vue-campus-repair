<template>
  <div class="staff-page">
    <header class="staff-hero">
      <div class="staff-hero__glow" aria-hidden="true" />
      <div class="staff-hero__content">
        <div class="staff-hero__intro">
          <div class="staff-hero__icon" aria-hidden="true">
            <el-icon :size="30"><Tools /></el-icon>
          </div>
          <div>
            <h1 class="staff-hero__title">维修工作台</h1>
            <p class="staff-hero__sub">待抢、跟单、完工，一站式处理</p>
          </div>
        </div>

        <div v-if="!loading && list.length" class="staff-stats" role="status">
          <div
            v-for="s in statRow"
            :key="s.key"
            class="staff-stats__item"
            :class="{ 'is-pulse': s.key === 'assigned' && s.count > 0 }"
          >
            <span class="staff-stats__num">{{ s.count }}</span>
            <span class="staff-stats__label">{{ s.label }}</span>
          </div>
        </div>

        <p class="staff-hero__tip">
          下方筛选仅作用于工单列表，保持当前「全部/待接收/跟单中/已结单」视图一致。
        </p>
      </div>
    </header>

    <section class="staff-list-wrap">
      <div class="staff-list-wrap__head">
        <h2 class="staff-list-wrap__h">工单列表</h2>
        <el-button
          class="staff-list-wrap__refresh"
          round
          :loading="loading"
          @click="loadList"
        >
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
      </div>
      <div class="staff-filters" role="tablist">
        <button
          v-for="opt in filterTabs"
          :key="opt.value"
          type="button"
          class="staff-filters__btn"
          :class="{ 'is-active': filter === opt.value }"
          role="tab"
          :aria-selected="filter === opt.value"
          @click="filter = opt.value"
        >
          {{ opt.label }}
        </button>
      </div>
      <p class="staff-list-wrap__desc">
        {{ listHint }}
      </p>

      <div v-if="loading" class="staff-skeleton">
        <el-skeleton v-for="i in 4" :key="i" animated>
          <template #template>
            <div class="skeleton-line">
              <el-skeleton-item variant="h3" style="width: 50%" />
              <el-skeleton-item variant="text" style="width: 30%" class="m-t" />
              <el-skeleton-item variant="p" style="width: 90%" class="m-t" />
            </div>
          </template>
        </el-skeleton>
      </div>
      <el-empty
        v-else-if="!filteredList.length"
        :description="emptyDesc"
        :image-size="96"
      />
      <TransitionGroup v-else name="staff-list" tag="ul" class="order-list">
        <li
          v-for="item in filteredList"
          :key="String(item.id)"
          class="order-card"
          :class="{
            'is-urgent': item.isUrgent,
            'is-pool': item.statusCode === 4
          }"
          @click="goDetail(item.id)"
        >
          <div class="order-card__left">
            <el-icon v-if="item.isUrgent" class="order-card__bolt"><Lightning /></el-icon>
            <el-icon v-else-if="item.statusCode === 4" class="order-card__bolt is-warn"
              ><AlarmClock
            /></el-icon>
            <el-icon v-else class="order-card__doc"><Document /></el-icon>
          </div>
          <div class="order-card__main">
            <div class="order-card__top">
              <span class="order-card__place">{{ item.location || '报修' }}</span>
              <el-tag
                v-if="item.statusCode != null"
                :type="getStatusTagType(item.statusCode)"
                size="small"
                effect="light"
                class="order-card__tag"
              >
                {{ getRepairStatusLabel(item.statusCode) }}
              </el-tag>
              <el-tag v-else type="info" size="small" effect="light">{{ item.status || '-' }}</el-tag>
            </div>
            <p class="order-card__desc line-clamp-2">{{ item.description || item.title || '—' }}</p>
            <div class="order-card__foot">
              <span v-if="item.ticketNo" class="order-card__no">{{ item.ticketNo }}</span>
              <span v-else-if="item.id" class="order-card__no">#{{ String(item.id).slice(-6) }}</span>
              <span v-if="item.createTime" class="order-card__time">{{ item.createTime }}</span>
            </div>
          </div>
          <el-icon class="order-card__chev"><ArrowRight /></el-icon>
        </li>
      </TransitionGroup>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Tools,
  RefreshRight,
  Lightning,
  Document,
  AlarmClock,
  ArrowRight
} from '@element-plus/icons-vue'
import { getRepairListPage } from '@/api/repair'
import { getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'StaffWorkbench' })

const router = useRouter()
const loading = ref(true)
const list = ref([])
const filter = ref('all')

const filterTabs = [
  { value: 'all', label: '全部' },
  { value: 'assigned', label: '待接收' },
  { value: 'going', label: '跟单中' },
  { value: 'closed', label: '已结单' }
]

function goDetail(id) {
  if (id == null) {
    return
  }
  router.push({ name: 'RepairDetail', params: { id: String(id) } })
}

const statRow = computed(() => {
  const l = list.value
  const n = (code) => l.filter((i) => Number(i.statusCode) === code).length
  return [
    { key: 'all', label: '全部', count: l.length },
    { key: 'assigned', label: '待接', count: n(4) },
    {
      key: 'going',
      label: '跟单中',
      count: l.filter((i) => [2, 3, 4, 5, 6, 7, 12].includes(Number(i.statusCode))).length
    },
    { key: 'end', label: '已结', count: l.filter((i) => [8, 9, 10].includes(Number(i.statusCode))).length }
  ]
})

const filteredList = computed(() => {
  const f = filter.value
  return list.value.filter((item) => {
    const sc = Number(item.statusCode)
    if (f === 'all') {
      return true
    }
    if (f === 'assigned') {
      return sc === 4
    }
    if (f === 'going') {
      return [2, 3, 4, 5, 6, 7, 12].includes(sc)
    }
    if (f === 'closed') {
      return [8, 9, 10].includes(sc)
    }
    return true
  })
})

const listHint = computed(() => {
  if (loading.value) {
    return '加载中…'
  }
  if (!list.value.length) {
    return '暂无可看工单，稍后在「公告」里留意派工安排'
  }
  return '共 ' + list.value.length + ' 单；当前「' + (filterTabs.find((t) => t.value === filter.value)?.label) + '」' + filteredList.value.length + ' 条。点进卡片处理派单任务'
})

const emptyDesc = computed(() =>
  list.value.length ? '该筛选项下暂无数据，试试「全部」' : '暂无分配给你的工单'
)

async function loadList() {
  loading.value = true
  try {
    const p = await getRepairListPage({ page: 1, size: 30 })
    list.value = p.records || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function onExternalRefresh() {
  loadList()
}

onMounted(() => {
  loadList()
  window.addEventListener('campus-repair-refresh-workbench', onExternalRefresh)
})
onActivated(loadList)
onUnmounted(() => {
  window.removeEventListener('campus-repair-refresh-workbench', onExternalRefresh)
})
</script>

<style scoped>
.staff-page {
  min-height: 400px;
  padding: 0 0 calc(24px + env(safe-area-inset-bottom, 0));
  background: linear-gradient(180deg, #ecfeff 0%, #f1f5f9 32%, #f8fafc 100%);
  --staff-accent: #0d9488;
  --staff-warn: #ea580c;
}

/* hero */
.staff-hero {
  position: relative;
  margin: 0 0 4px;
  padding: 16px 12px 8px;
  overflow: hidden;
}
.staff-hero__glow {
  position: absolute;
  inset: -30% 20% auto 20%;
  height: 180px;
  background: radial-gradient(ellipse, rgba(45, 212, 191, 0.25) 0%, transparent 70%);
  pointer-events: none;
}
.staff-hero__content {
  position: relative;
  max-width: 720px;
  margin: 0 auto;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid rgba(13, 148, 136, 0.12);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(15, 23, 42, 0.08), 0 1px 0 rgba(255, 255, 255, 0.8) inset;
  padding: 20px 18px 16px;
}
.staff-hero__intro {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
}
.staff-hero__icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #0d9488 0%, #14b8a6 45%, #0f766e 100%);
  box-shadow: 0 8px 20px rgba(13, 148, 136, 0.35);
}
.staff-hero__title {
  font-size: 1.25rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  margin: 0 0 4px 0;
  color: #0f172a;
}
.staff-hero__sub {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}
.staff-hero__tip {
  margin: 0;
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 12px;
  color: #64748b;
  background: rgba(13, 148, 136, 0.07);
  border: 1px solid rgba(13, 148, 136, 0.15);
}

/* stats */
.staff-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.staff-stats__item {
  flex: 1 1 65px;
  min-width: 0;
  padding: 8px 6px;
  text-align: center;
  background: #f0fdfa;
  border: 1px solid rgba(13, 148, 136, 0.12);
  border-radius: 12px;
  transition: transform 0.2s ease;
}
.staff-stats__item.is-pulse {
  box-shadow: 0 0 0 0 rgba(234, 88, 12, 0.3);
  animation: staff-pulse 2.4s ease-in-out infinite;
}
@keyframes staff-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(234, 88, 12, 0.15);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(234, 88, 12, 0.08);
  }
}
.staff-stats__num {
  display: block;
  font-size: 1.1rem;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}
.staff-stats__label {
  font-size: 11px;
  color: #64748b;
}

/* filter chips */
.staff-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.staff-filters__btn {
  border: 1px solid #e2e8f0;
  background: #fff;
  color: #475569;
  font-size: 12px;
  font-weight: 500;
  padding: 6px 14px;
  border-radius: 999px;
  cursor: pointer;
  transition:
    color 0.2s,
    background 0.2s,
    border-color 0.2s,
    box-shadow 0.2s;
}
.staff-filters__btn:hover {
  color: #0d9488;
  border-color: #99f6e4;
  background: #f0fdfa;
}
.staff-filters__btn.is-active {
  color: #fff;
  background: linear-gradient(135deg, #0d9488, #0f766e);
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(13, 148, 136, 0.35);
}

/* list section */
.staff-list-wrap {
  padding: 8px 12px 12px;
  max-width: 720px;
  margin: 0 auto;
}
.staff-list-wrap__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.staff-list-wrap__refresh {
  color: #475569;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
}
.staff-list-wrap__h {
  margin: 0;
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
}
.staff-list-wrap__desc {
  margin: 0 0 12px 0;
  font-size: 12px;
  color: #64748b;
  line-height: 1.55;
}
.staff-skeleton {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.skeleton-line {
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}
.skeleton-line .m-t {
  margin-top: 8px;
}

/* cards */
.order-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-card {
  position: relative;
  display: flex;
  align-items: stretch;
  gap: 12px;
  padding: 14px 12px 14px 0;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
  cursor: pointer;
  transition:
    box-shadow 0.25s,
    border-color 0.25s,
    transform 0.2s;
  overflow: hidden;
}
.order-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: #cbd5e1;
  border-radius: 0 2px 2px 0;
  transition: background 0.2s;
}
.order-card.is-urgent::before {
  background: linear-gradient(180deg, #f97316, #ea580c);
}
.order-card.is-pool::before {
  background: linear-gradient(180deg, #fbbf24, #ea580c);
}
.order-card:hover {
  border-color: #99f6e4;
  box-shadow: 0 8px 28px rgba(13, 148, 136, 0.1);
  transform: translateY(-1px);
}
.order-card__left {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  padding-left: 4px;
  flex-shrink: 0;
  color: #94a3b8;
}
.order-card__main {
  flex: 1;
  min-width: 0;
}
.order-card__bolt {
  font-size: 22px;
  color: #ea580c;
}
.order-card__bolt.is-warn {
  color: #d97706;
}
.order-card__doc {
  font-size: 20px;
  color: #0d9488;
}
.order-card__top {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
}
.order-card__place {
  flex: 1 1 120px;
  min-width: 0;
  font-weight: 700;
  font-size: 15px;
  color: #0f172a;
}
.order-card__tag {
  font-weight: 600;
  border: none;
}
.order-card__desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.45;
}
.order-card__foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  font-size: 11px;
  color: #94a3b8;
}
.order-card__no {
  font-family: ui-monospace, monospace;
  color: #64748b;
}
.order-card__chev {
  align-self: center;
  color: #cbd5e1;
  font-size: 16px;
  margin-right: 4px;
  flex-shrink: 0;
  transition: transform 0.2s, color 0.2s;
}
.order-card:hover .order-card__chev {
  color: #0d9488;
  transform: translateX(2px);
}
.line-clamp-2 {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.staff-list-enter-active,
.staff-list-leave-active {
  transition: all 0.25s ease;
}
.staff-list-enter-from,
.staff-list-leave-to {
  opacity: 0;
  transform: translateY(6px);
}

@media (min-width: 960px) {
  .staff-page {
    min-height: auto;
    padding: 0;
    background: transparent;
    display: grid;
    grid-template-columns: minmax(320px, 0.42fr) minmax(0, 1fr);
    gap: 22px;
  }

  .staff-hero {
    margin: 0;
    padding: 0;
    overflow: visible;
  }

  .staff-hero__content {
    position: sticky;
    top: 24px;
    max-width: none;
    min-height: 360px;
    padding: 28px;
    border-radius: 24px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .staff-hero__intro {
    align-items: flex-start;
  }

  .staff-hero__title {
    font-size: 28px;
  }

  .staff-stats {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-top: 26px;
  }

  .staff-stats__item {
    min-height: 86px;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .staff-list-wrap {
    max-width: none;
    margin: 0;
    padding: 26px;
    background: #fff;
    border-radius: 24px;
    border: 1px solid rgba(148, 163, 184, 0.18);
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .staff-list-wrap__h {
    font-size: 22px;
  }

  .order-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .order-card {
    height: 100%;
    border: 1px solid rgba(148, 163, 184, 0.16);
    border-radius: 18px;
    padding: 16px 12px 16px 0;
  }

  .order-card__desc {
    min-height: 42px;
  }
}
</style>
