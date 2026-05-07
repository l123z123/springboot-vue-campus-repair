<template>
  <div class="workorders-page">
    <!-- 高级筛选区 -->
    <el-form :model="filters" label-width="80px" class="filter-form">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="关键词">
            <el-input
              v-model="filters.keyword"
              placeholder="工单号/标题/描述"
              clearable
              @keyup.enter="fetchList"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="状态">
            <el-select
              v-model="filters.status"
              placeholder="全部"
              clearable
              @change="fetchList"
            >
              <el-option label="全部" value="" />
              <el-option label="待审核" :value="0" />
              <el-option label="已分配" :value="1" />
              <el-option label="处理中" :value="2" />
              <el-option label="待验收" :value="3" />
              <el-option label="已完成" :value="4" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="紧急度">
            <el-select
              v-model="filters.urgency"
              placeholder="全部"
              clearable
              @change="fetchList"
            >
              <el-option label="全部" value="" />
              <el-option label="普通" value="low" />
              <el-option label="一般" value="medium" />
              <el-option label="紧急" value="high" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="4">
          <el-form-item label="维修员">
            <el-select
              v-model="filters.staffId"
              placeholder="全部"
              clearable
              filterable
              remote
              :remote-method="searchStaff"
              @change="fetchList"
            >
              <el-option
                v-for="item in staffOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="filters.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              @change="fetchList"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <!-- 操作栏 -->
    <div class="action-bar">
      <div class="left">
        <el-button type="primary" :icon="User" @click="handleBatchAssign" :disabled="selectedIds.length === 0">批量分配</el-button>
        <el-button type="success" :icon="CircleCheck" @click="handleBatchComplete" :disabled="selectedIds.length === 0">批量完成</el-button>
        <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="selectedIds.length === 0">批量删除</el-button>
      </div>
      <div class="right">
        <el-button type="default" :icon="Download" @click="handleExport">导出报表</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      ref="tableRef"
      :data="list"
      stripe
      v-loading="loading"
      border
      size="small"
      highlight-current-row
      @selection-change="onSelectionChange"
      style="width: 100%"
    >
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column prop="id" label="工单号" width="130" show-overflow-tooltip>
        <template #default="{ row }">
          <span style="font-family: monospace; font-size: 12px;">{{ row.id || row.orderId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.title || row.description || '-' }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" width="140" show-overflow-tooltip />
      <el-table-column prop="campus" label="校区" width="100" />
      <el-table-column prop="staffName" label="维修员" width="100">
        <template #default="{ row }">
          {{ row.staffName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="urgency" label="紧急度" width="90">
        <template #default="{ row }">
          <el-tag :type="urgencyTagType(row.urgency)" size="small">
            {{ urgencyLabel(row.urgency) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="date" label="提交时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showDetail(row)">查看</el-button>
          <el-button type="warning" link size="small" @click="handleAssign(row)">分配</el-button>
          <el-button type="success" link size="small" @click="handleComplete(row)">完成</el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 工单详情弹窗（PC 端专业样式） -->
    <el-dialog
      v-model="detailVisible"
      title="工单详情"
      width="640px"
      destroy-on-close
      class="order-detail-dialog"
    >
      <div v-if="detailLoading" class="detail-loading">
        <el-skeleton :rows="6" animated />
      </div>
      <template v-else>
        <div v-if="currentOrder.image" class="detail-dialog__image">
          <el-image :src="currentOrder.image" fit="contain" style="max-height: 200px; width: 100%;" />
        </div>
        <el-descriptions :column="2" border class="detail-dialog__desc">
          <el-descriptions-item label="工单号">{{ currentOrder.id || currentOrder.orderId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentOrder.status)" size="small">
              {{ statusLabel(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ currentOrder.title || currentOrder.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ currentOrder.location || '-' }}</el-descriptions-item>
          <el-descriptions-item label="校区">{{ currentOrder.campus || '-' }}</el-descriptions-item>
          <el-descriptions-item label="紧急度">
            <el-tag :type="urgencyTagType(currentOrder.urgency)" size="small">
              {{ urgencyLabel(currentOrder.urgency) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ currentOrder.date || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentOrder.phoneNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">
            <div class="detail-dialog__desc-text">{{ currentOrder.description || currentOrder.title || '-' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleProcessFromDetail">前往处理</el-button>
      </template>
    </el-dialog>

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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, User, CircleCheck, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { getRepairListPage, getRepairDetail, assignStaff, completeRepair } from '@/api/repair'
import { searchUsers } from '@/api/user' // 新增：查询用户接口

const router = useRouter()

// 补充筛选条件
const filters = reactive({
  keyword: '',
  status: '',
  location: '',
  urgency: '', // 紧急度筛选
  staffId: '', // 维修员筛选
  dateRange: [] // 时间范围筛选
})

// 维修员搜索选项
const staffOptions = ref([])

// 搜索维修员
async function searchStaff(query) {
  if (query && query.length > 1) {
    try {
      const result = await searchUsers({ keyword: query, role: 1 }) // 角色1为维修员
      staffOptions.value = result.records || []
    } catch (error) {
      console.error('搜索维修员失败:', error)
      staffOptions.value = []
    }
  } else {
    staffOptions.value = []
  }
}

// 批量分配选中的工单
async function handleBatchAssign() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要分配的工单')
    return
  }
  
  try {
    await ElMessageBox.prompt('请输入维修员ID或姓名', '批量分配工单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /.+/, // 输入不能为空
      inputErrorMessage: '请输入有效的维修员信息'
    })
    
    // 这里需要根据输入查找维修员并获取其ID
    // 简化处理：假设输入的是维修员ID
    const staffId = parseInt(promptInput)
    if (isNaN(staffId)) {
      ElMessage.error('维修员ID必须是数字')
      return
    }
    
    // 调用批量分配API（需要后端支持）
    await Promise.all(selectedIds.value.map(id => assignStaff(id, staffId)))
    
    ElMessage.success(`成功分配${selectedIds.value.length}个工单给维修员`)
    fetchList() // 刷新列表
    selectedIds.value = [] // 清空选择
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批量分配失败')
      console.error(err)
    }
  }
}

// 批量完成选中的工单
async function handleBatchComplete() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要完成的工单')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要将选中的${selectedIds.value.length}个工单标记为已完成吗？`,
      '批量完成',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    
    // 调用批量完成API（需要后端支持）
    await Promise.all(selectedIds.value.map(id => completeRepair(id)))
    
    ElMessage.success(`成功完成${selectedIds.value.length}个工单`)
    fetchList() // 刷新列表
    selectedIds.value = [] // 清空选择
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批量完成失败')
      console.error(err)
    }
  }
}

// 分配单个工单
async function handleAssign(row) {
  try {
    const { value: staffId } = await ElMessageBox.prompt('请输入维修员ID', '分配工单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\d+/, // 必须是数字
      inputErrorMessage: '维修员ID必须是数字'
    })
    
    await assignStaff(row.id || row.orderId, parseInt(staffId))
    ElMessage.success('分配成功')
    fetchList() // 刷新列表
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('分配失败')
      console.error(err)
    }
  }
}

// 完成单个工单
async function handleComplete(row) {
  try {
    await ElMessageBox.confirm('确定要将此工单标记为已完成吗？', '完成工单', {
      type: 'warning'
    })
    
    await completeRepair(row.id || row.orderId)
    ElMessage.success('工单已完成')
    fetchList() // 刷新列表
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('完成失败')
      console.error(err)
    }
  }
}

// 删除单个工单
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除此工单吗？', '删除工单', {
      type: 'warning'
    })
    
    await deleteRepair(row.id || row.orderId)
    ElMessage.success('工单已删除')
    fetchList() // 刷新列表
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(err)
    }
  }
}

// 批量删除选中的工单
async function handleBatchDelete() {
  if (selectedIds.value.length === 0) {
    ElMessage.warning('请先选择要删除的工单')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的${selectedIds.value.length}个工单吗？`,
      '批量删除',
      {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }
    )
    
    // 调用批量删除API（需要后端支持）
    await Promise.all(selectedIds.value.map(id => deleteRepair(id)))
    
    ElMessage.success(`成功删除${selectedIds.value.length}个工单`)
    fetchList() // 刷新列表
    selectedIds.value = [] // 清空选择
    
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批量删除失败')
      console.error(err)
    }
  }
}

// 导出工单列表
async function handleExport() {
  try {
    const result = await getRepairListPage({ ...filters, export: true })
    const blob = new Blob([result], { type: 'application/vnd.ms-excel' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '工单列表.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  }
}

// 查看工单详情
async function showDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const result = await getRepairDetail(row.id || row.orderId)
    currentOrder.value = result
  } catch (error) {
    console.error('获取工单详情失败:', error)
    ElMessage.error('获取工单详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 从详情页前往处理
function handleProcessFromDetail() {
  detailVisible.value = false
  router.push({ name: 'RepairProcess', params: { id: currentOrder.value.id || currentOrder.value.orderId } })
}

// 获取工单列表
async function fetchList() {
  loading.value = true
  try {
    const result = await getRepairListPage({
      ...filters,
      page: pagination.page,
      size: pagination.size
    })
    list.value = result.records || []
    total.value = result.total || 0
  } catch (error) {
    console.error('获取工单列表失败:', error)
    ElMessage.error('获取工单列表失败')
  } finally {
    loading.value = false
  }
}

// 选择项变化
function onSelectionChange(selection) {
  selectedIds.value = selection.map(item => item.id || item.orderId)
}

// 分页变化
function onPageChange(page) {
  pagination.page = page
  fetchList()
}

// 每页数量变化
function onSizeChange(size) {
  pagination.size = size
  fetchList()
}

// 紧急度标签类型
function urgencyTagType(urgency) {
  switch (urgency) {
    case 1:
    case 'low':
      return 'info'
    case 2:
    case 'medium':
      return 'warning'
    case 3:
    case 'high':
      return 'danger'
    default:
      return 'info'
  }
}

// 紧急度标签文本
function urgencyLabel(urgency) {
  switch (urgency) {
    case 1:
    case 'low':
      return '普通'
    case 2:
    case 'medium':
      return '一般'
    case 3:
    case 'high':
      return '紧急'
    default:
      return '未知'
  }
}

// 状态标签类型
function statusTagType(status) {
  switch (status) {
    case 0:
      return 'info'
    case 1:
      return 'warning'
    case 2:
      return 'primary'
    case 3:
      return 'success'
    case 4:
      return 'success'
    default:
      return ''
  }
}

// 状态标签文本
function statusLabel(status) {
  switch (status) {
    case 0:
      return '待审核'
    case 1:
      return '已分配'
    case 2:
      return '处理中'
    case 3:
      return '待验收'
    case 4:
      return '已完成'
    default:
      return ''
  }
}

// 初始化
onMounted(() => {
  fetchList()
})

</script>

<style scoped>
.workorders-page {
  min-height: 400px;
  padding: 20px;
}

/* 高级筛选表单样式 */
.filter-form {
  margin-bottom: 16px;
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.action-bar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.action-bar .left {
  display: flex;
  gap: 10px;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-loading {
  padding: 20px 0;
}

.detail-dialog__image {
  margin-bottom: 16px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color);
}

.detail-dialog__desc-text {
  max-height: 120px;
  overflow-y: auto;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
