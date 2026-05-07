<template>
  <div class="image-upload">
    <el-upload
      ref="uploadRef"
      class="upload-drag"
      drag
      :action="uploadUrl"
      :headers="uploadHeaders"
      name="file"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :disabled="loading"
    >
      <el-icon v-if="!loading" class="el-icon--upload"><UploadFilled /></el-icon>
      <el-icon v-else class="el-icon--upload"><Loading /></el-icon>
      <div class="el-upload__text">
        <template v-if="loading">上传中...</template>
        <template v-else>将文件拖到此处，或 <em>点击上传</em></template>
      </div>
    </el-upload>

    <div class="preview-wrap">
      <div class="preview-label">预览</div>
      <template v-if="imageUrl">
        <div class="preview-img-wrap">
          <span class="preview-badge">已上传</span>
          <el-image
            :src="imageUrl"
            fit="contain"
            class="preview-img"
            :preview-src-list="[imageUrl]"
          />
        </div>
        <div class="preview-actions">
          <el-button type="primary" size="small" @click="handleDownload">
            下载
          </el-button>
          <el-button size="small" @click="handlePreview">预览</el-button>
          <el-button type="danger" size="small" plain @click="handleRemovePreview">
            删除
          </el-button>
        </div>
      </template>
      <div v-else class="preview-empty">
        <el-icon class="preview-empty-icon"><Picture /></el-icon>
        <p class="preview-empty-text">暂无图片，请点击上方上传</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled, Loading, Picture } from '@element-plus/icons-vue'

const props = defineProps({
  /** Bearer Token，用于请求头 Authorization */
  token: { type: String, default: '' },
  /** 上传接口地址 */
  uploadUrl: { type: String, default: '/api/file/upload' }
})

const emit = defineEmits(['success'])

const uploadRef = ref(null)
const loading = ref(false)
const imageUrl = ref('')

const uploadHeaders = computed(() => {
  const t = (props.token || '').trim()
  const auth = t.startsWith('Bearer ') ? t : (t ? `Bearer ${t}` : '')
  return auth ? { Authorization: auth } : {}
})

function normalizePreviewUrl(url) {
  if (typeof url !== 'string' || !url) return ''
  if (url.startsWith('/')) {
    const base = import.meta.env.VITE_API_BASE_URL || '/api'
    const origin = typeof window !== 'undefined' ? window.location.origin : ''
    return origin ? `${origin}${base}${url}` : url
  }
  if (typeof window !== 'undefined' && url.includes('/api/files/')) {
    const m = url.match(/\/api\/files\/(.+)$/)
    if (m) return `${window.location.origin}/api/files/${m[1]}`
  }
  return url
}

const beforeUpload = () => {
  imageUrl.value = ''
  loading.value = true
}

const onSuccess = (response) => {
  loading.value = false
  uploadRef.value?.clearFiles()
  if (response && response.code === 200 && response.data) {
    const url = normalizePreviewUrl(response.data)
    imageUrl.value = url
    ElMessage.success('上传成功')
    emit('success', url)
  } else {
    ElMessage.error(response?.message || '上传失败')
  }
}

const onError = (err) => {
  loading.value = false
  uploadRef.value?.clearFiles()
  const msg = (err?.message || String(err)) || '上传失败'
  ElMessage.error(msg)
}

function getFilenameFromUrl(url) {
  try {
    const pathname = new URL(url, window.location.origin).pathname
    const name = pathname.split('/').filter(Boolean).pop() || '下载'
    return decodeURIComponent(name)
  } catch {
    return '下载.jpg'
  }
}

async function handleDownload() {
  const url = imageUrl.value
  if (!url) return
  const filename = getFilenameFromUrl(url)
  try {
    const res = await fetch(url, { credentials: 'include' })
    if (!res.ok) throw new Error(res.statusText)
    const blob = await res.blob()
    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = filename
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(blobUrl)
    ElMessage.success(`${filename} 已保存到本地`)
  } catch (e) {
    ElMessage.error('下载失败：' + (e?.message || '未知错误'))
  }
}

function handlePreview() {
  if (imageUrl.value) window.open(imageUrl.value, '_blank')
}

function handleRemovePreview() {
  ElMessageBox.confirm('确定要删除这张图片吗？此操作不可恢复。', '提示', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      imageUrl.value = ''
    })
    .catch(() => {})
}

defineExpose({
  imageUrl,
  clearPreview: () => { imageUrl.value = '' }
})
</script>

<style scoped>
.image-upload {
  max-width: 100%;
  overflow: hidden;
}
.upload-drag :deep(.el-upload-dragger) {
  width: 100%;
}
.el-upload__text :deep(em) {
  font-style: normal;
  font-weight: 600;
  color: var(--el-color-primary);
}
.preview-wrap {
  margin-top: 16px;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}
.preview-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  padding: 24px;
}
.preview-empty-icon {
  font-size: 48px;
  color: #909399;
  margin-bottom: 8px;
}
.preview-empty-text {
  margin: 0;
  font-size: 14px;
  color: #909399;
}
.preview-img-wrap {
  position: relative;
  display: inline-block;
  max-width: 100%;
}
.preview-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 1;
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 500;
  color: #fff;
  background: var(--el-color-success);
  border-radius: 4px;
}
.preview-img {
  display: block;
  max-width: 100%;
  max-height: 400px;
}
.preview-img :deep(img) {
  width: 100%;
  height: auto;
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
}
.preview-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
