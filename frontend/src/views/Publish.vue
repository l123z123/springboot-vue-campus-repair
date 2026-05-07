<template>
  <div class="publish-page">
    <div class="publish-card">
      <!-- 一键急修特殊标识 -->
      <div v-if="isUrgentMode" class="urgent-banner">
        <el-icon class="urgent-icon"><Lightning /></el-icon>
        <span class="urgent-text">紧急报修 - 优先处理</span>
      </div>
      <h3 class="publish-card__title">{{ isUrgentMode ? '一键急修' : '提交报修' }}</h3>

      <!-- 图片上传：多图（用 label 关联 input，确保点击能稳定打开文件选择） -->
      <div class="form-block">
        <div class="form-block__label">报修图片</div>
        <label
          class="upload-area"
          :class="{ 'is-loading': uploadLoading, 'is-disabled': uploadLoading }"
          :for="uploadLoading ? undefined : 'publish-file-input'"
          @dragover.prevent
          @drop.prevent="onDrop"
        >
          <el-icon v-if="!uploadLoading" class="upload-area__icon"><Plus /></el-icon>
          <el-icon v-else class="upload-area__icon is-loading"><Loading /></el-icon>
          <span class="upload-area__text">{{ uploadLoading ? '上传中...' : '点击或拖拽上传（可多张，单张≤5MB）' }}</span>
          <input
            id="publish-file-input"
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/webp,image/jpg"
            multiple
            class="upload-area__input"
            @change="onFileChange"
          >
        </label>
        <div v-if="imageList.length" class="preview-list">
          <div
            v-for="(item, index) in imageList"
            :key="index"
            class="preview-item"
          >
            <el-image :src="item.url" fit="cover" class="preview-item__img" />
            <span class="preview-item__del" @click.stop="removeImage(index)">×</span>
          </div>
        </div>
      </div>

      <!-- 校区与位置 -->
      <div class="form-block">
        <div class="form-block__label">校区</div>
        <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
          <el-option
            v-for="item in campusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
      <div class="form-block">
        <div class="form-block__label">具体位置</div>
        <el-select
          v-model="form.area"
          :disabled="!form.campus"
          placeholder="请选择楼栋/区域"
          style="width: 100%"
        >
          <el-option
            v-for="item in areaOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </div>
      <div class="form-block">
        <div class="form-block__label">详细位置</div>
        <el-input v-model="form.locationDetail" placeholder="如：3号楼 502 宿舍 / A302 教室" />
      </div>

      <!-- 报修种类 -->
      <div class="form-block">
        <div class="form-block__label">报修种类</div>
        <el-cascader
          v-model="form.category"
          :options="categoryOptions"
          :props="{ expandTrigger: 'click', value: 'value', label: 'label' }"
          placeholder="请选择报修种类"
          style="width: 100%"
        />
      </div>

      <!-- 故障描述 -->
      <div class="form-block">
        <div class="form-block__label">故障描述</div>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="请详细描述故障现象..."
          maxlength="500"
          show-word-limit
        />
      </div>

      <!-- 联系手机号 -->
      <div class="form-block">
        <div class="form-block__label">联系电话</div>
        <el-input
          v-model="form.phoneNumber"
          maxlength="11"
          placeholder="请输入11位手机号码"
        />
        <div v-if="form.phoneNumber && phoneInvalid" class="error-text">
          请输入有效的手机号码
        </div>
      </div>

      <!-- 紧急程度 -->
      <div class="form-block">
        <div class="form-block__label">紧急程度</div>
        <el-radio-group v-model="form.urgency" class="urgency-group">
          <el-radio value="high">高</el-radio>
          <el-radio value="medium">中</el-radio>
          <el-radio value="low">低</el-radio>
        </el-radio-group>
      </div>

      <el-button
        type="primary"
        size="large"
        class="submit-btn"
        :loading="submitLoading"
        :disabled="phoneInvalid"
        @click="handleSubmit"
      >
        立即提交
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Loading, Lightning } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'
import { createRepair } from '@/api/repair'
import { normalizeImageUrl, extractImageUrl, MAX_IMAGE_SIZE } from '@/utils/image'
import { campusOptions, categoryOptions } from '@/constants/campus'

defineOptions({ name: 'Publish' })

const route = useRoute()
const userStore = useUserStore()
const token = computed(() => userStore.token || localStorage.getItem('token') || '')

// 判断是否是一键急修
const isUrgentMode = computed(() => route.query.urgent === 'true')

const uploadUrl = computed(() => {
  const base = import.meta.env.VITE_API_BASE_URL || '/api'
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${base}/file/upload` : `${base}/file/upload`
})

const fileInputRef = ref(null)
const imageList = ref([])
const submitLoading = ref(false)
const uploadLoading = ref(false)

const form = ref({
  campus: '',
  area: '',
  locationDetail: '',
  description: '',
  urgency: 'medium',
  phoneNumber: '',
  category: []
})

// 页面加载时处理一键急修逻辑
onMounted(() => {
  if (isUrgentMode.value) {
    form.value.urgency = 'high' // 一键急修默认高优先级
  }
})

const areaOptions = computed(() => {
  const campus = form.value.campus
  const found = campusOptions.find((c) => c.value === campus)
  return found?.children || []
})

const URGENCY_NUM = { high: 3, medium: 2, low: 1 }
const PHONE_REG = /^1[3-9]\d{9}$/

const phoneInvalid = computed(() => {
  const val = (form.value.phoneNumber || '').trim()
  if (!val) return true
  return !PHONE_REG.test(val)
})

function getCategoryValue(arr) {
  if (!Array.isArray(arr) || arr.length === 0) return ''
  return arr[arr.length - 1]
}

async function onFileChange(e) {
  const files = e.target.files ? [...e.target.files] : []
  e.target.value = ''
  const validFiles = files.filter((f) => {
    if (!f.type.startsWith('image/')) {
      ElMessage.warning(`${f.name} 不是图片，已跳过`)
      return false
    }
    if (f.size > MAX_IMAGE_SIZE) {
      ElMessage.error(`${f.name} 超过 5MB，已跳过`)
      return false
    }
    return true
  })
  if (!validFiles.length) return
  await doUpload(validFiles)
}
async function onDrop(e) {
  if (uploadLoading.value) return
  const files = e.dataTransfer?.files ? [...e.dataTransfer.files] : []
  const validFiles = files.filter((f) => {
    if (!f.type.startsWith('image/')) {
      ElMessage.warning(`${f.name} 不是图片，已跳过`)
      return false
    }
    if (f.size > MAX_IMAGE_SIZE) {
      ElMessage.error(`${f.name} 超过 5MB，已跳过`)
      return false
    }
    return true
  })
  if (!validFiles.length) return
  await doUpload(validFiles)
}
async function doUpload(validFiles) {
  uploadLoading.value = true
  try {
    for (const file of validFiles) {
      const fd = new FormData()
      fd.append('file', file)
      const res = await request.post('/file/upload', fd)
      const url = extractImageUrl(res)
      if (url) imageList.value.push({ url: normalizeImageUrl(url) })
    }
  } catch (err) {
    ElMessage.error('上传失败：' + (err?.message || '未知错误'))
  } finally {
    uploadLoading.value = false
  }
}


function removeImage(index) {
  imageList.value.splice(index, 1)
}

async function handleSubmit() {
  const hasDesc = form.value.description && form.value.description.trim()
  const hasImages = imageList.value.length > 0
  if (!hasImages && !hasDesc) {
    ElMessage.warning('请至少上传一张图片或填写故障描述')
    return
  }
  const phoneVal = (form.value.phoneNumber || '').trim()
  if (!PHONE_REG.test(phoneVal)) {
    ElMessage.error('请输入有效的手机号码')
    return
  }
  if (!form.value.campus || !form.value.area) {
    ElMessage.error('请选择校区与具体位置')
    return
  }
  if (!Array.isArray(form.value.category) || !form.value.category.length) {
    ElMessage.error('请选择报修种类')
    return
  }
  submitLoading.value = true
  try {
    const campusLabel = campusOptions.find((c) => c.value === form.value.campus)?.label || ''
    const areaLabel = areaOptions.value.find((a) => a.value === form.value.area)?.label || ''
    const detail = form.value.locationDetail || ''
    const parts = [campusLabel, areaLabel, detail].filter(Boolean)
    const locationStr = parts.join(' - ') || (form.value.description || '').slice(0, 50)
    const title = (form.value.description && form.value.description.trim().slice(0, 50)) || locationStr || '报修'
    const body = {
      title: title.slice(0, 100),
      description: form.value.description || '',
      location: locationStr.slice(0, 100),
      urgency: URGENCY_NUM[form.value.urgency] ?? 2,
      images: hasImages ? JSON.stringify(imageList.value.map((i) => i.url)) : '',
      phoneNumber: phoneVal,
      campus: form.value.campus,
      area: form.value.area,
      category: getCategoryValue(form.value.category),
      locationDetail: form.value.locationDetail,
      isUrgent: isUrgentMode.value
    }
    await createRepair(body)
    ElMessage.success(isUrgentMode.value ? '一键急诊提交成功，已进入快速处理通道' : '报修成功，管理员已收到通知')
    form.value = {
      campus: '',
      area: '',
      locationDetail: '',
      description: '',
      urgency: 'medium',
      phoneNumber: '',
      category: []
    }
    imageList.value = []
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.publish-page {
  padding: 24px 16px 80px 16px;
  min-height: 400px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.publish-card {
  background: white;
  border-radius: 20px;
  padding: 28px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

/* 一键急修特殊样式 */
.urgent-banner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  margin-bottom: 24px;
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
  border-radius: 16px;
  color: white;
  box-shadow: 0 4px 16px rgba(238, 90, 36, 0.35);
  position: relative;
  overflow: hidden;
}

.urgent-banner::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.15) 0%, transparent 70%);
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(20%, 20%); }
}

.urgent-icon {
  font-size: 24px;
  position: relative;
  z-index: 1;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

.urgent-text {
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.5px;
  position: relative;
  z-index: 1;
}

.publish-card__title {
  margin: 0 0 28px 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  letter-spacing: 0.5px;
  position: relative;
  display: inline-block;
}

.publish-card__title::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 40px;
  height: 4px;
  background: linear-gradient(90deg, var(--el-color-primary), var(--el-color-primary-light-3));
  border-radius: 2px;
}

.form-block {
  margin-bottom: 24px;
}

.form-block__label {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  display: flex;
  align-items: center;
  gap: 6px;
}

.form-block__label::before {
  content: '';
  width: 4px;
  height: 14px;
  background: var(--el-color-primary);
  border-radius: 2px;
}

.error-text {
  margin-top: 6px;
  font-size: 13px;
  color: var(--el-color-danger);
  padding: 6px 10px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 上传区域美化 */
.upload-area {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  border: 2px dashed var(--el-color-primary-light-5);
  border-radius: 16px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9), #fff);
  cursor: pointer;
  margin: 0;
  transition: all 0.3s ease;
}

.upload-area:hover {
  border-color: var(--el-color-primary);
  background: linear-gradient(135deg, var(--el-color-primary-light-8), #fff);
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.15);
}

.upload-area.is-disabled {
  pointer-events: none;
  cursor: not-allowed;
  opacity: 0.6;
}

.upload-area__input {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  opacity: 0;
  font-size: 0;
  cursor: pointer;
}

.upload-area__icon {
  font-size: 36px;
  color: var(--el-color-primary);
  transition: transform 0.3s ease;
}

.upload-area:hover .upload-area__icon {
  transform: scale(1.15);
}

.upload-area__text {
  margin-top: 10px;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

/* 预览列表美化 */
.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.preview-item {
  position: relative;
  width: 90px;
  height: 90px;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s ease;
}

.preview-item:hover {
  transform: scale(1.05);
}

.preview-item__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-item__del {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  backdrop-filter: blur(4px);
}

.preview-item__del:hover {
  background: #f56c6c;
  transform: scale(1.15);
}

/* 紧急程度单选框美化 */
.urgency-group {
  width: 100%;
  display: flex;
  gap: 12px;
}

.urgency-group :deep(.el-radio) {
  flex: 1;
  margin-right: 0;
}

.urgency-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 12px;
  font-weight: 500;
  padding: 12px 20px;
  transition: all 0.3s ease;
}

/* 提交按钮美化 */
.submit-btn {
  width: 100%;
  margin-top: 32px;
  height: 54px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--el-color-primary), #7c66f0);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s ease;
  border: none;
}

.submit-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(102, 126, 234, 0.5);
}

.submit-btn:active {
  transform: translateY(-1px);
}

/* Element Plus 组件样式覆盖 */
:deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 10px 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.25);
}

:deep(.el-select .el-input__wrapper) {
  padding-right: 8px;
}

:deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.25);
}

:deep(.el-cascader) {
  width: 100%;
}

:deep(.el-input__inner) {
  border-radius: 12px;
}

/* 响应式适配 */
@media (max-width: 480px) {
  .publish-page {
    padding: 20px 14px 80px 14px;
  }
  
  .publish-card {
    padding: 24px 20px;
  }
  
  .preview-item {
    width: 78px;
    height: 78px;
  }
  
  .urgency-group {
    flex-direction: column;
    gap: 8px;
  }
  
  .urgency-group :deep(.el-radio-button__inner) {
    padding: 10px 16px;
  }
}

@media (min-width: 960px) {
  .publish-page {
    min-height: auto;
    padding: 0;
    background: transparent;
  }

  .publish-card {
    display: grid;
    grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
    gap: 4px 28px;
    padding: 32px;
    border: 1px solid rgba(148, 163, 184, 0.18);
    border-radius: 24px;
    box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
  }

  .urgent-banner,
  .publish-card__title {
    grid-column: 1 / -1;
  }

  .publish-card__title {
    margin-bottom: 20px;
  }

  .form-block:first-of-type {
    grid-row: span 5;
  }

  .form-block {
    margin-bottom: 18px;
  }

  .upload-area {
    min-height: 330px;
  }

  .preview-list {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .preview-item {
    width: 100%;
    height: 104px;
  }

  .submit-btn {
    grid-column: 1 / -1;
    max-width: 320px;
    justify-self: end;
    margin-top: 10px;
  }
}
</style>
