<template>
  <div class="admin-tickets">
    <div class="ticket-filter-bar">
      <div class="filter-item filter-item--keyword">
        <el-input
          v-model="filters.keyword"
          placeholder="工单号/报修人/故障描述"
          clearable
          @keyup.enter="fetchList"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-item">
        <el-select
          v-model="filters.statusGroup"
          placeholder="工单状态"
          style="min-width: 200px"
        >
          <el-option
            v-for="o in adminStatusFilterOptions"
            :key="o.key"
            :label="o.label"
            :value="o.key"
          />
        </el-select>
      </div>
      <div class="filter-item">
        <el-select
          v-model="filters.urgency"
          placeholder="紧急程度"
          clearable
        >
          <el-option label="全部" value="all" />
          <el-option label="普通" value="normal" />
          <el-option label="紧急" value="urgent" />
          <el-option label="特急" value="critical" />
        </el-select>
      </div>
      <div class="filter-item filter-item--date">
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD"
        />
      </div>
      <div class="filter-item filter-item--actions">
        <el-button type="primary" @click="fetchList">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <span class="filter-hint">支持按工单号、报修人、描述搜索</span>
      </div>
    </div>

    <el-table :data="list" stripe v-loading="loading">
      <template #empty>
        <el-empty description="暂无工单数据" />
      </template>
      <el-table-column prop="ticketNo" label="工单号" width="130">
        <template #default="{ row }">
          <div class="ticket-no-cell">
            <span class="ticket-no-link" @click="showDetail(row)">{{ formatTicketNo(row.ticketNo) }}</span>
            <el-tooltip content="复制工单号" placement="top">
              <el-icon class="copy-icon" @click.stop="copyTicketNo(row.ticketNo)"><DocumentCopy /></el-icon>
            </el-tooltip>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="故障标题" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="ticket-title-cell">
            <span class="ticket-title-text">{{ row.title || row.description || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="报修人/地点" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="ticket-meta-cell">
            <div class="ticket-meta-user">
              {{ row.reporterName || '-' }} {{ row.reporterNo || '' }}
            </div>
            <div class="ticket-meta-location">
              {{ row.location || '-' }}
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="故障类型" width="90">
        <template #default="{ row }">
          <el-tag size="small">{{ categoryLabelCn(row.category) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="urgency" label="紧急程度" width="80">
        <template #default="{ row }">
          <el-tag
            size="small"
            class="urgency-tag"
            :class="`urgency-tag--${row.urgency}`"
          >
            {{ urgencyLabel(row.urgency) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="当前状态" width="100">
        <template #default="{ row }">
          <el-tag
            size="small"
            class="status-tag"
            :class="`status-tag--c${row.statusCode}`"
            :type="getStatusTagType(row.statusCode)"
          >
            {{ getRepairStatusLabel(row.statusCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建日期" width="100">
        <template #default="{ row }">{{ shortDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right" align="left" class-name="ticket-actions-col">
        <template #default="{ row }">
          <div class="ticket-actions">
            <el-button type="primary" link size="small" @click="showDetail(row)">详情</el-button>
            <el-button type="success" link size="small" @click="goToProcessPage(row)">去处理</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        :current-page="pagination.page"
        :page-size="pagination.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </div>

    <!-- 工单详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="工单详情"
      width="680px"
      destroy-on-close
      class="ticket-detail-dialog"
      @close="onDetailClose"
    >
      <template v-if="currentTicket.id">
        <div class="detail-section">
          <h4 class="detail-section__title">基础信息</h4>
          <div class="detail-ticket-no">
            工单号：{{ currentTicket.ticketNo }}
            <el-button type="primary" link size="small" @click="copyTicketNo(currentTicket.ticketNo)">复制</el-button>
          </div>
          <p class="detail-desc">{{ currentTicket.description || currentTicket.title || '-' }}</p>
          <div v-if="imageList.length" class="detail-images">
            <el-image
              v-for="(url, idx) in imageList"
              :key="idx"
              :src="url"
              :preview-src-list="imageList"
              :initial-index="idx"
              fit="cover"
              class="detail-image"
            />
          </div>
          <div class="detail-meta">
            <span>报修时间：{{ currentTicket.createTime }}</span>
            <span>联系人电话：{{ currentTicket.phone || '-' }}</span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="detail-section__title">处理记录</h4>
          <el-timeline>
            <el-timeline-item
              v-for="(item, idx) in (currentTicket.timeline || [])"
              :key="idx"
              :timestamp="item.time"
              placement="top"
            >
              <div><strong>{{ item.operator }}</strong>：{{ item.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <div class="detail-section detail-actions">
          <h4 class="detail-section__title">操作（与业务状态机一致）</h4>
          <p class="detail-actions__hint">
            派单版流程：在工单详情页完成审核、智能推荐派单、维修处理与评价；管理端请跳转至该页处理，勿使用旧版「改数字状态」。
          </p>
          <el-button
            v-if="currentTicket.id || currentTicket.orderId"
            type="primary"
            @click="goToProcessPage(currentTicket)"
          >
            进入工单处理页
          </el-button>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, DocumentCopy } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { normalizeImageUrl } from '@/utils/image'
import { getRepairListPage, getRepairDetail } from '@/api/repair'
import { ADMIN_STATUS_FILTERS, getRepairStatusLabel, getStatusTagType } from '@/constants/repairStatus'

defineOptions({ name: 'AdminTickets' })

const route = useRoute()
const router = useRouter()
const adminStatusFilterOptions = ADMIN_STATUS_FILTERS
const loading = ref(false)
const list = ref([])
const total = ref(0)

const filters = reactive({
  keyword: '',
  statusGroup: 'all',
  urgency: 'all',
  dateRange: null
})

const pagination = reactive({ page: 1, size: 10 })

const detailVisible = ref(false)
const currentTicket = ref({})

function urgencyLabel(u) {
  const map = { low: '普通', medium: '紧急', high: '特急', normal: '普通', urgent: '紧急', critical: '特急' }
  return map[u] ?? u ?? '-'
}

function urgencyTagType(u) {
  const map = { low: 'primary', medium: 'warning', high: 'danger', normal: 'primary', urgent: 'warning', critical: 'danger' }
  return map[u] ?? 'info'
}

function categoryLabelCn(cat) {
  if (!cat) return '其他'
  const c = String(cat).toUpperCase()
  const map = {
    LIFE_DORM: '宿舍用电',
    LIFE_AIRCON: '空调',
    LIFE_LIGHTING: '照明',
    LIFE_WATER: '给排水',
    LIFE_WASHER: '洗衣设备',
    LIFE_CANTEEN: '食堂餐厅',
    LIFE_CANTER: '食堂餐厅',
    INFRA_BASKETBALL: '球场设施',
    INFRA_TRACK: '跑道/操场',
    INFRA_FOOTBALL: '足球场',
    TEACHING_LAB: '教学实验室',
    TEACHING_LABORATORY: '教学实验室',
    TEACHING_LAB_EQUIP: '实验室设备',
    TEACHING_LAB_EQUIPMENT: '实验室设备',
    TEACHING_MULTIMEDIA: '多媒体设备',
    TEACHING_MULTI_MEDIA: '多媒体设备'
  }
  if (map[c]) return map[c]
  if (c.includes('TEACHING')) return '教学相关'
  if (c.startsWith('INFRA')) return '基础设施'
  if (c.startsWith('LIFE_')) return '生活类'
  return '其他'
}

const imageList = computed(() => {
  const imgs = currentTicket.value.images || []
  return imgs.map((u) => normalizeImageUrl(typeof u === 'string' ? u : u?.url)).filter(Boolean)
})

function formatTicketNo(no) {
  if (!no) return '-'
  const s = String(no)
  if (s.length <= 12) return s
  return `${s.slice(0, 6)}...${s.slice(-4)}`
}

function shortDate(v) {
  if (!v) return '-'
  if (typeof v === 'string') return v.slice(0, 10)
  if (Array.isArray(v) && v.length >= 3) {
    const [y, m, d] = v
    const pad = (n) => String(n).padStart(2, '0')
    return `${y}-${pad(m)}-${pad(d)}`
  }
  return '-'
}

function copyTicketNo(no) {
  if (!no) return
  navigator.clipboard.writeText(no).then(
    () => ElMessage.success('已复制工单号'),
    () => ElMessage.error('复制失败')
  )
}

function resetFilters() {
  filters.keyword = ''
  filters.statusGroup = 'all'
  filters.urgency = 'all'
  filters.dateRange = null
  pagination.page = 1
  fetchList()
}

function fetchList() {
  loading.value = true
  const opt = ADMIN_STATUS_FILTERS.find((x) => x.key === (filters.statusGroup || 'all'))
  const statusIn = opt && opt.statusIn != null ? opt.statusIn : undefined
  const params = {
    page: pagination.page,
    size: pagination.size,
    keyword: filters.keyword?.trim() || undefined,
    statusIn,
    urgency: filters.urgency && filters.urgency !== 'all' ? filters.urgency : undefined,
    dateRange: filters.dateRange && filters.dateRange.length === 2 ? filters.dateRange : undefined
  }
  getRepairListPage(params)
    .then(({ records, total: t }) => {
      list.value = records || []
      total.value = t ?? list.value.length
    })
    .catch(() => {
      list.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

function onPageChange(p) {
  pagination.page = p
  fetchList()
}

function onSizeChange(s) {
  pagination.size = s
  pagination.page = 1
  fetchList()
}

function showDetail(row) {
  const id = row.id ?? row.orderId
  if (id) {
    getRepairDetail(id)
      .then((data) => {
        currentTicket.value = { ...data, timeline: data.timeline || [] }
        detailVisible.value = true
      })
      .catch(() => {
        currentTicket.value = { ...row }
        detailVisible.value = true
      })
  } else {
    currentTicket.value = { ...row }
    detailVisible.value = true
  }
}

function onDetailClose() {
  currentTicket.value = {}
}

function goToProcessPage(row) {
  const id = row.id ?? row.orderId
  if (id) {
    const r = router.resolve({ path: `/repair/detail/${id}` })
    window.open(r.href, '_blank')
  }
}

onMounted(() => {
  fetchList()
  const id = route.query?.id
  if (id) {
    getRepairDetail(id)
      .then((data) => {
        currentTicket.value = { ...data, timeline: data.timeline || [] }
        detailVisible.value = true
      })
      .catch(() => {})
  }
})
</script>

<style lang="scss" scoped>
.admin-tickets {
  min-height: 400px;
}

.ticket-filter-bar {
  margin-bottom: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px 16px;
  align-items: center;
}

.filter-item :deep(.el-input),
.filter-item :deep(.el-select),
.filter-item :deep(.el-date-editor) {
  width: 100%;
}

.filter-item--keyword {
  min-width: 260px;
}

.filter-item--actions {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-start;
}

.filter-hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.ticket-no-link {
  color: var(--el-color-primary);
  cursor: pointer;
  margin-right: 6px;
  &:hover {
    text-decoration: underline;
  }
}

.ticket-no-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.copy-icon {
  cursor: pointer;
  color: var(--el-text-color-secondary);
  vertical-align: middle;
  &:hover {
    color: var(--el-color-primary);
  }
}

.repair-info {
  font-size: 13px;
  .repair-info__title {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .repair-info__reporter {
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
  .repair-info__location {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    margin-top: 2px;
  }
}

.ticket-title-cell {
  display: flex;
  align-items: center;
}

.ticket-title-text {
  font-weight: 600;
  font-size: 14px;
  color: var(--el-text-color-primary);
  line-height: 1.6;
}

.ticket-meta-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 12px;
  line-height: 1.6;
}

.ticket-meta-user {
  color: var(--el-text-color-primary);
}

.ticket-meta-location {
  color: var(--el-text-color-secondary);
}

.urgency-tag,
.status-tag {
  border-radius: 999px;
  border: none;
  padding: 0 10px;
  font-size: 12px;
}

.urgency-tag--low,
.urgency-tag--normal {
  background: #e5e7eb;
  color: #4b5563;
}

.urgency-tag--medium,
.urgency-tag--urgent {
  background: #fef3c7;
  color: #d97706;
}

.urgency-tag--high,
.urgency-tag--critical {
  background: #fee2e2;
  color: #b91c1c;
}

.status-tag--pending {
  background: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.status-tag--processing {
  background: rgba(99, 102, 241, 0.12);
  color: #4338ca;
}

.status-tag--done,
.status-tag--evaluated {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.status-tag--closed {
  background: rgba(148, 163, 184, 0.18);
  color: #4b5563;
}

.ticket-actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-section {
  margin-bottom: 24px;
  .detail-section__title {
    margin: 0 0 12px 0;
    font-size: 14px;
    color: var(--el-text-color-primary);
  }
}

.detail-ticket-no {
  margin-bottom: 12px;
  font-size: 14px;
}

.detail-desc {
  white-space: pre-wrap;
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.detail-images {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  .detail-image {
    width: 80px;
    height: 80px;
    border-radius: 4px;
    cursor: pointer;
  }
}

.detail-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.detail-actions__hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin: 0 0 12px 0;
}

.detail-actions .action-block {
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
}
</style>
