<template>
  <div class="feedback-manage-container">
    <!-- 1. 顶部导航栏 (复用公告通知样式) -->
    <header class="page-header">
      <el-button type="primary" link class="page-header__back" @click="router.push('/admin/dashboard')">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="page-header__title">反馈管理</span>
    </header>

    <el-card shadow="never" class="feedback-card">
      <!-- 2. 搜索与筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索反馈内容或联系方式..."
          style="width: 300px; margin-right: 10px;"
          clearable
          @clear="fetchFeedbacks"
          @keyup.enter="fetchFeedbacks"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>

        <el-select
          v-model="statusFilter"
          placeholder="状态筛选"
          style="width: 120px; margin-right: 10px;"
          clearable
          @change="fetchFeedbacks"
        >
          <el-option label="全部" value="" />
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>

        <el-button type="primary" @click="fetchFeedbacks">查询</el-button>
      </div>

      <!-- 3. 表格区域（可滚动） -->
      <div class="table-wrap">
        <el-table
          :data="feedbackList"
          stripe
          highlight-current-row
          v-loading="loading"
          @row-click="showDetail"
          style="cursor: pointer;"
        >
        <el-table-column prop="id" label="ID" width="100" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="100" />

        <el-table-column prop="content" label="反馈内容" min-width="250" show-overflow-tooltip />

        <el-table-column prop="contactInfo" label="联系方式" width="150">
          <template #default="{ row }">
            <span v-if="row.contactInfo" class="contact-text">{{ row.contactInfo }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="提交时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
              {{ row.status === 0 ? '待处理' : '已处理' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div @click.stop>
              <el-button
                v-if="row.status === 0"
                type="primary"
                size="small"
                @click="handleMarkAsProcessed(row.id)"
              >
                标记已处理
              </el-button>
              <el-tag v-else type="success" size="small">已完成</el-tag>
            </div>
          </template>
        </el-table-column>
        </el-table>
      </div>

      <!-- 4. 分页（固定在底部：总数、每页条数、上一页、页码、下一页、跳转输入框） -->
      <div class="pagination-container">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          :pager-count="5"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>

    <!-- 4. 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="反馈详情" width="500px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="提交人ID">{{ currentFeedback.userId }}</el-descriptions-item>
        <el-descriptions-item label="联系方式">
          {{ currentFeedback.contactInfo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDate(currentFeedback.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="反馈内容">
          <div class="detail-content">{{ currentFeedback.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="currentFeedback.status === 0 ? 'danger' : 'success'">
            {{ currentFeedback.status === 0 ? '待处理' : '已处理' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="currentFeedback.status === 0"
          type="primary"
          @click="handleMarkAsProcessed(currentFeedback.id); detailVisible = false"
        >
          标记已处理
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Search } from '@element-plus/icons-vue'
import { getAllFeedback, updateFeedbackStatus } from '@/api/feedback'

defineOptions({ name: 'FeedbackManage' })

const router = useRouter()
const loading = ref(false)
const feedbackList = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const statusFilter = ref('')
const detailVisible = ref(false)
const currentFeedback = ref({})

function formatDate(val) {
  if (!val) return '-'
  if (Array.isArray(val)) {
    const [y, m, d, h = 0, i = 0, s = 0] = val
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(i).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  if (typeof val === 'string') return val
  return String(val)
}

async function fetchFeedbacks() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (statusFilter.value !== '' && statusFilter.value != null) params.status = statusFilter.value
    const data = await getAllFeedback(params)
    const records = data?.records ?? []
    feedbackList.value = records
    // 优先使用后端返回的 total，缺失时根据当前页数据估算，避免 Total 显示 0
    const rawTotal = data?.total != null ? Number(data.total) : -1
    total.value = rawTotal >= 0 ? rawTotal : (records.length > 0 ? Math.max(records.length, (currentPage.value - 1) * pageSize.value + records.length) : 0)
  } catch (e) {
    feedbackList.value = []
    total.value = 0
    ElMessage.error(e?.message || '加载失败，请检查后端服务是否启动')
  } finally {
    loading.value = false
  }
}

function onPageChange(page) {
  currentPage.value = page
  fetchFeedbacks()
}

function onSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  fetchFeedbacks()
}

function showDetail(row) {
  currentFeedback.value = { ...row }
  detailVisible.value = true
}

async function handleMarkAsProcessed(id) {
  try {
    await updateFeedbackStatus(id, 1)
    ElMessage.success('已标记为已处理')
    await fetchFeedbacks()
  } catch (e) {
    ElMessage.error(e?.message || '操作失败')
  }
}

onMounted(() => {
  fetchFeedbacks()
})
</script>

<style scoped>
.feedback-manage-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--el-fill-color-lighter);
}

/* 顶部导航 (完全复用公告通知样式) */
.page-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.page-header__back {
  margin-right: 8px;
}
.page-header__title {
  font-size: 16px;
  font-weight: 500;
}

.feedback-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin: 16px;
  border-radius: 12px;
  min-height: 0;
}

.feedback-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.filter-bar {
  flex-shrink: 0;
  margin-bottom: 16px;
}

.table-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.contact-text {
  color: var(--el-color-primary);
}

.pagination-container {
  flex-shrink: 0;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: flex-end;
}

.pagination-container :deep(.el-pagination) {
  flex-wrap: wrap;
}

.pagination-container :deep(.el-pagination__jump) {
  margin-left: 16px;
}

.detail-content {
  max-height: 300px;
  overflow-y: auto;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
