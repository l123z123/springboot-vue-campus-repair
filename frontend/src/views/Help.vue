<template>
  <div class="help-page">
    <header class="help-header">
      <el-button type="primary" link class="help-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="help-header__title">帮助与反馈</span>
    </header>
    <div class="help-body">
      <p class="help-desc">如有问题或建议，请联系校园报修客服或通过下方表单提交反馈。</p>

      <!-- 意见反馈表单 -->
      <div class="help-feedback">
        <h4 class="help-feedback__title">意见反馈</h4>
        <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="4"
              placeholder="请简要描述您的问题或建议..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="联系方式（选填）" prop="contact">
            <el-input v-model="form.contact" placeholder="手机号/邮箱，方便我们联系您" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 最近反馈记录 -->
      <div v-if="recentList.length" class="help-recent">
        <h4 class="help-recent__title">我的反馈记录</h4>
        <div v-for="(item, i) in recentList" :key="i" class="help-recent__item">
          <p class="help-recent__content">{{ item.content }}</p>
          <p class="help-recent__meta">{{ item.time }}{{ item.contact ? ' · ' + item.contact : '' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { submitFeedback as submitFeedbackApi, getMyFeedback } from '@/api/feedback'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const feedbackList = ref([])

const form = ref({ content: '', contact: '' })

// 自定义联系方式校验：允许为空，非空时必须是手机或邮箱
const validateContact = (_rule, value, callback) => {
  if (!value) {
    callback()
    return
  }
  const phoneReg = /^1[3-9]\d{9}$/
  const emailReg = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
  if (phoneReg.test(value) || emailReg.test(value)) {
    callback()
  } else {
    callback(new Error('请输入有效的手机号或邮箱地址'))
  }
}

const rules = {
  content: [
    { required: true, message: '请填写反馈内容', trigger: 'blur' },
    {
      min: 5,
      max: 500,
      message: '反馈内容需在 5-500 字之间',
      trigger: 'blur'
    }
  ],
  contact: [{ validator: validateContact, trigger: 'blur' }]
}

const recentList = computed(() => feedbackList.value.slice(0, 5))

const loadMyFeedback = async () => {
  try {
    const res = await getMyFeedback()
    // 后端返回 SysFeedback 列表，这里做一个简单映射
    const arr = Array.isArray(res) ? res : res?.data ?? []
    feedbackList.value = arr.map((item) => ({
      content: item.content,
      contact: item.contactInfo,
      time: item.createTime
    }))
  } catch {
    feedbackList.value = []
  }
}

onMounted(() => {
  loadMyFeedback()
})

async function submitFeedback() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitting.value = true
  try {
    await submitFeedbackApi({
      content: form.value.content.trim(),
      contactInfo: form.value.contact?.trim() || undefined
    })
    ElMessage.success('已收到您的反馈，感谢支持！')
    form.value = { content: '', contact: '' }
    formRef.value?.resetFields()
    await loadMyFeedback()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.help-page {
  min-height: 400px;
  background: var(--el-fill-color-lighter);
}
.help-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.help-header__back {
  margin-right: 8px;
}
.help-header__title {
  font-size: 16px;
  font-weight: 500;
}
.help-body {
  margin-top: 16px;
  padding: 16px;
  background: var(--el-bg-color);
}
.help-desc {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}
.help-feedback {
  margin-bottom: 24px;
}
.help-feedback__title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
}
.help-recent {
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.help-recent__title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
}
.help-recent__item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.help-recent__item:last-child {
  border-bottom: none;
}
.help-recent__content {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
}
.help-recent__meta {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
