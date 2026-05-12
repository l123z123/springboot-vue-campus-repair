<template>
  <div class="publish-page">
    <!-- 紧急报修提示 -->
    <el-alert
      v-if="isUrgentMode"
      title="紧急报修 - 该工单将优先处理"
      type="warning"
      :closable="false"
      show-icon
      class="urgent-alert"
    />

    <el-card shadow="never" class="publish-card">
      <template #header>
        <span class="publish-card__title">{{ isUrgentMode ? '一键急修' : '提交报修' }}</span>
      </template>

      <div class="publish-form">
        <!-- 左列：图片上传 -->
        <div class="publish-form__left">
          <label
            class="upload-area"
            :class="{ 'is-loading': uploadLoading }"
            @dragover.prevent
            @drop.prevent="onDrop"
          >
            <el-icon v-if="!uploadLoading" class="upload-area__icon"><Plus /></el-icon>
            <el-icon v-else class="upload-area__icon is-spin"><Loading /></el-icon>
            <span class="upload-area__text">{{ uploadLoading ? '上传中...' : '点击或拖拽上传（可多张，单张≤5MB）' }}</span>
            <input
              ref="fileInputRef"
              type="file"
              accept="image/jpeg,image/png,image/webp,image/jpg"
              multiple
              class="upload-area__input"
              @change="onFileChange"
            >
          </label>
          <div v-if="imageList.length" class="preview-list">
            <div v-for="(item, index) in imageList" :key="index" class="preview-item">
              <el-image :src="item.url" fit="cover" class="preview-item__img" />
              <span class="preview-item__del" @click.stop="removeImage(index)">&times;</span>
            </div>
          </div>
        </div>

        <!-- 右列：表单字段 -->
        <div class="publish-form__right">
          <el-form label-width="80px" label-position="right">
            <el-form-item label="校区">
              <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
                <el-option v-for="item in campusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="具体位置">
              <el-select v-model="form.area" :disabled="!form.campus" placeholder="请选择楼栋/区域" style="width: 100%">
                <el-option v-for="item in areaOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="详细位置">
              <el-input v-model="form.locationDetail" placeholder="如：3号楼 502 宿舍 / A302 教室" />
            </el-form-item>
            <el-form-item label="报修种类">
              <el-cascader
                v-model="form.category"
                :options="categoryOptions"
                :props="{ expandTrigger: 'click', value: 'value', label: 'label' }"
                placeholder="请选择报修种类"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="故障描述">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="4"
                placeholder="请详细描述故障现象..."
                maxlength="500"
                show-word-limit
              />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.phoneNumber" maxlength="11" placeholder="请输入11位手机号码" />
              <div v-if="form.phoneNumber && phoneInvalid" class="field-error">请输入有效的手机号码</div>
            </el-form-item>
            <el-form-item label="紧急程度">
              <el-radio-group v-model="form.urgency">
                <el-radio value="low">普通 — 不影响正常使用</el-radio>
                <el-radio value="medium">紧急 — 影响正常使用</el-radio>
                <el-radio value="high">非常紧急 — 存在安全隐患</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="submitLoading"
                :disabled="phoneInvalid"
                @click="handleSubmit"
              >
                立即提交
              </el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-card>
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

const isUrgentMode = computed(() => route.query.urgent === 'true')

const fileInputRef = ref(null)
const imageList = ref([])
const submitLoading = ref(false)
const uploadLoading = ref(false)

const form = ref({
  campus: '', area: '', locationDetail: '', description: '',
  urgency: 'medium', phoneNumber: '', category: []
})

onMounted(() => {
  if (isUrgentMode.value) form.value.urgency = 'high'
})

const areaOptions = computed(() => {
  const found = campusOptions.find((c) => c.value === form.value.campus)
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
  const valid = files.filter((f) => {
    if (!f.type.startsWith('image/')) { ElMessage.warning(`${f.name} 不是图片，已跳过`); return false }
    if (f.size > MAX_IMAGE_SIZE) { ElMessage.error(`${f.name} 超过 5MB，已跳过`); return false }
    return true
  })
  if (valid.length) await doUpload(valid)
}

async function onDrop(e) {
  if (uploadLoading.value) return
  const files = e.dataTransfer?.files ? [...e.dataTransfer.files] : []
  const valid = files.filter((f) => {
    if (!f.type.startsWith('image/')) { ElMessage.warning(`${f.name} 不是图片，已跳过`); return false }
    if (f.size > MAX_IMAGE_SIZE) { ElMessage.error(`${f.name} 超过 5MB，已跳过`); return false }
    return true
  })
  if (valid.length) await doUpload(valid)
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

function removeImage(index) { imageList.value.splice(index, 1) }

async function handleSubmit() {
  const hasDesc = form.value.description && form.value.description.trim()
  const hasImages = imageList.value.length > 0
  if (!hasImages && !hasDesc) { ElMessage.warning('请至少上传一张图片或填写故障描述'); return }
  if (hasImages && !hasDesc) {
    ElMessage.warning('建议补充故障描述，方便维修人员快速定位问题')
  }
  const phoneVal = (form.value.phoneNumber || '').trim()
  if (!PHONE_REG.test(phoneVal)) { ElMessage.error('请输入有效的手机号码'); return }
  if (!form.value.campus || !form.value.area) { ElMessage.error('请选择校区与具体位置'); return }
  if (!Array.isArray(form.value.category) || !form.value.category.length) { ElMessage.error('请选择报修种类'); return }

  submitLoading.value = true
  try {
    const campusLabel = campusOptions.find((c) => c.value === form.value.campus)?.label || ''
    const areaLabel = areaOptions.value.find((a) => a.value === form.value.area)?.label || ''
    const detail = form.value.locationDetail || ''
    const parts = [campusLabel, areaLabel, detail].filter(Boolean)
    const locationStr = parts.join(' - ') || (form.value.description || '').slice(0, 50)
    const title = (form.value.description && form.value.description.trim().slice(0, 50)) || locationStr || '报修'

    await createRepair({
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
    })
    ElMessage.success(isUrgentMode.value ? '一键急诊提交成功，已进入快速处理通道' : '报修成功，管理员已收到通知')
    form.value = { campus: '', area: '', locationDetail: '', description: '', urgency: 'medium', phoneNumber: '', category: [] }
    imageList.value = []
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.urgent-alert {
  margin-bottom: 16px;
}

.publish-card {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid var(--el-border-color-lighter);
}

.publish-card__title {
  font-size: 16px;
  font-weight: 600;
}

.publish-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.publish-form__left {
  min-width: 0;
}

/* upload area */
.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 160px;
  border: 2px dashed var(--el-color-primary-light-5);
  border-radius: 12px;
  background: var(--el-color-primary-light-9);
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  position: relative;
}
.upload-area:hover {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-8);
}
.upload-area.is-loading {
  pointer-events: none;
  opacity: 0.6;
}
.upload-area__input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}
.upload-area__icon {
  font-size: 36px;
  color: var(--el-color-primary);
}
.is-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.upload-area__text {
  margin-top: 10px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}
.preview-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}
.preview-item__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-item__del {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-radius: 50%;
  font-size: 14px;
  cursor: pointer;
  line-height: 1;
}
.preview-item__del:hover { background: var(--el-color-danger); }

/* form */
.publish-form__right {
  min-width: 0;
}

.field-error {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-color-danger);
}
</style>
