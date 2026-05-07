<template>
  <div class="booking-page">
    <header class="booking-header">
      <el-button type="primary" link class="booking-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="booking-header__title">预约维修</span>
    </header>
    <div class="booking-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="预约时间" prop="time">
          <el-date-picker
            v-model="form.time"
            type="datetime"
            placeholder="选择预约时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="联系电话" prop="phoneNumber">
          <el-input v-model="form.phoneNumber" maxlength="11" placeholder="请输入11位手机号码" />
        </el-form-item>
        <el-form-item label="校区" prop="campus">
          <el-select v-model="form.campus" placeholder="请选择校区" style="width: 100%">
            <el-option
              v-for="item in campusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="具体位置" prop="area">
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
        </el-form-item>
        <el-form-item label="详细位置" prop="locationDetail">
          <el-input v-model="form.locationDetail" placeholder="如：教学楼 A-302" />
        </el-form-item>
        <el-form-item label="问题描述" prop="desc">
          <el-input v-model="form.desc" type="textarea" :rows="4" placeholder="请描述需要维修的内容..." />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" style="width: 100%" @click="submit">提交预约</el-button>
        </el-form-item>
      </el-form>

      <div v-if="list.length" class="booking-list">
        <h4 class="booking-list__title">我的预约</h4>
        <div v-for="(item, i) in list" :key="i" class="booking-item">
          <div class="booking-item__row">
            <span class="booking-item__label">时间</span>
            <span>{{ item.time }}</span>
          </div>
          <div class="booking-item__row">
            <span class="booking-item__label">地点</span>
            <span>{{ item.location || '-' }}</span>
          </div>
          <div class="booking-item__row">
            <span class="booking-item__label">描述</span>
            <span>{{ item.desc || '-' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createRepair } from '@/api/repair'
import { campusOptions } from '@/constants/campus'

const STORAGE_KEY = 'bookingList'

function loadList() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : []
  } catch {
    return []
  }
}

function saveList(arr) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(arr))
  } catch {}
}

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const list = ref([])

const PHONE_REG = /^1[3-9]\d{9}$/

const form = ref({ time: '', campus: '', area: '', locationDetail: '', desc: '', phoneNumber: '' })
const rules = {
  time: [{ required: true, message: '请选择预约时间', trigger: 'change' }],
  phoneNumber: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: PHONE_REG, message: '请输入有效的手机号码', trigger: ['blur', 'change'] }
  ],
  campus: [{ required: true, message: '请选择校区', trigger: 'change' }],
  area: [{ required: true, message: '请选择位置', trigger: 'change' }]
}

const areaOptions = computed(() => {
  const campus = form.value.campus
  const found = campusOptions.find((c) => c.value === campus)
  return found?.children || []
})

onMounted(() => {
  list.value = loadList()
})

async function submit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    const { time, campus, area, locationDetail, desc } = form.value
    const campusLabel = campusOptions.find((c) => c.value === campus)?.label || ''
    const areaLabel = areaOptions.value.find((a) => a.value === area)?.label || ''
    const locText = [campusLabel, areaLabel, locationDetail].filter(Boolean).join(' - ')
    const title = `预约 ${time || ''} - ${locText || '待确认'}`
    await createRepair({
      title: title.slice(0, 100),
      description: desc || '',
      location: locText || '',
      urgency: 2,
      images: '',
      phoneNumber: (form.value.phoneNumber || '').trim(),
      campus,
      area,
      category: 'LIFE_DORM',
      locationDetail
    })
    list.value.unshift({ time, location: locText, desc })
    saveList(list.value)
    form.value = { time: '', campus: '', area: '', locationDetail: '', desc: '', phoneNumber: '' }
    formRef.value?.resetFields()
    ElMessage.success('预约已提交，管理员已收到通知')
  } catch (e) {
    ElMessage.error(e?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.booking-page {
  min-height: 400px;
  max-width: 640px;
}
.booking-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.booking-header__back {
  margin-right: 8px;
}
.booking-header__title {
  font-size: 16px;
  font-weight: 500;
}
.booking-body {
  padding: 16px;
}
.booking-list {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.booking-list__title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
}
.booking-item {
  padding: 12px;
  margin-bottom: 12px;
  background: var(--el-bg-color);
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.booking-item__row {
  margin-bottom: 6px;
  font-size: 14px;
}
.booking-item__row:last-child {
  margin-bottom: 0;
}
.booking-item__label {
  color: var(--el-text-color-secondary);
  margin-right: 8px;
}
</style>
