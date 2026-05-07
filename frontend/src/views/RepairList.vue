<template>
  <div class="repair-list-page">
    <h2>维修单 / 文件列表</h2>
    <el-table :data="tableData" stripe style="width: 100%">
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column prop="uploadTime" label="上传时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.uploadTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="fileSize" label="文件大小" width="120">
        <template #default="{ row }">
          {{ formatFileSize(row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDownload(row)">
            下载
          </el-button>
          <el-button type="danger" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @current-change="(p) => { currentPage.value = p; loadData() }"
      @size-change="(s) => { pageSize.value = s; currentPage.value = 1; loadData() }"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '@/utils/format'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const allMockData = ref([])

const tableData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return allMockData.value.slice(start, start + pageSize.value)
})

function getMockList() {
  return [
    { id: 1, fileName: '报修照片1.jpg', uploadTime: '2025-03-01 10:00:00', fileSize: 102400, downloadUrl: '/files/1.jpg' },
    { id: 2, fileName: '报修照片2.png', uploadTime: '2025-03-02 11:30:00', fileSize: 204800, downloadUrl: '/files/2.png' },
    { id: 3, fileName: '维修单说明.pdf', uploadTime: '2025-03-03 09:15:00', fileSize: 51200, downloadUrl: '/files/3.pdf' },
    { id: 4, fileName: '现场图.jpg', uploadTime: '2025-03-05 14:20:00', fileSize: 307200, downloadUrl: '/files/4.jpg' },
    { id: 5, fileName: '验收单.docx', uploadTime: '2025-03-06 16:45:00', fileSize: 15360, downloadUrl: '/files/5.docx' }
  ]
}

function loadData() {
  total.value = allMockData.value.length
}

function formatFileSize(bytes) {
  if (bytes == null) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function handleDownload(row) {
  ElMessage.info('Mock：下载 ' + row.fileName)
  if (row.downloadUrl) window.open(row.downloadUrl, '_blank')
}

function handleDelete(row) {
  ElMessageBox.confirm('确定删除该记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    allMockData.value = allMockData.value.filter((item) => item.id !== row.id)
    total.value = allMockData.value.length
    ElMessage.success('已删除（当前为前端 Mock）')
  }).catch(() => {})
}

onMounted(() => {
  allMockData.value = getMockList()
  total.value = allMockData.value.length
})
</script>

<style scoped>
.repair-list-page h2 {
  margin-bottom: 20px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
