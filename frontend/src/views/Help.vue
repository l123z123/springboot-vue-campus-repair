<template>
  <div class="help-page">
    <el-card shadow="never" class="help-card">
      <template #header><span class="help-card__title">帮助与反馈</span></template>
      <p class="help-desc">如有问题或建议，请先查看常见问题，或通过下方表单提交反馈。</p>

      <div class="help-faq">
        <h4 class="help-section__title">常见问题</h4>
        <el-collapse>
          <el-collapse-item title="如何查看我的工单处理进度？" name="1">
            <p>在"工单管理"页面可以查看所有工单，点击任意工单进入详情页即可查看完整的处理进度（提交→审核→派单→维修→确认→评价）。</p>
          </el-collapse-item>
          <el-collapse-item title="报修后多久会有响应？" name="2">
            <p>工作日内通常30分钟内会有响应，紧急工单（如漏水、漏电等安全隐患）将优先处理。非工作日提交的工单将顺延至下一个工作日处理。</p>
          </el-collapse-item>
          <el-collapse-item title="如何联系维修师傅？" name="3">
            <p>工单分配维修师傅后，在工单详情页会出现"联系沟通"按钮，点击即可与师傅在线沟通。也可以通过消息中心查看所有沟通记录。</p>
          </el-collapse-item>
          <el-collapse-item title="对维修结果不满意怎么办？" name="4">
            <p>确认维修完成时，您可以在评价中给出真实评分和意见。如有严重问题，也可以通过本页面的反馈表单提交申诉，管理员会跟进处理。</p>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="help-feedback">
        <h4 class="help-section__title">意见反馈</h4>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
          <el-form-item label="反馈内容" prop="content">
            <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请简要描述您的问题或建议..." maxlength="500" show-word-limit />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.contact" placeholder="手机号/邮箱，方便我们联系您（选填）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitFeedback">提交反馈</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card v-if="recentList.length" shadow="never" class="help-card help-recent">
      <template #header><span>我的反馈记录</span></template>
      <div v-for="(item, i) in recentList" :key="i" class="recent-item">
        <p class="recent-item__content">{{ item.content }}</p>
        <p class="recent-item__meta">{{ item.time }}{{ item.contact ? ' · ' + item.contact : '' }}</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { submitFeedback as submitFeedbackApi, getMyFeedback } from '@/api/feedback'

defineOptions({ name: 'Help' })

const formRef = ref(null)
const submitting = ref(false)
const feedbackList = ref([])

const form = ref({ content: '', contact: '' })

const validateContact = (_rule, value, callback) => {
  if (!value) { callback(); return }
  const phoneReg = /^1[3-9]\d{9}$/
  const emailReg = /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/
  if (phoneReg.test(value) || emailReg.test(value)) { callback() }
  else { callback(new Error('请输入有效的手机号或邮箱地址')) }
}

const rules = {
  content: [
    { required: true, message: '请填写反馈内容', trigger: 'blur' },
    { min: 5, max: 500, message: '反馈内容需在 5-500 字之间', trigger: 'blur' }
  ],
  contact: [{ validator: validateContact, trigger: 'blur' }]
}

const recentList = computed(() => feedbackList.value.slice(0, 5))

const loadMyFeedback = async () => {
  try {
    const res = await getMyFeedback()
    const arr = Array.isArray(res) ? res : res?.data ?? []
    feedbackList.value = arr.map((item) => ({
      content: item.content, contact: item.contactInfo, time: item.createTime
    }))
  } catch { feedbackList.value = [] }
}

onMounted(() => { loadMyFeedback() })

async function submitFeedback() {
  try { await formRef.value?.validate() } catch { return }
  submitting.value = true
  try {
    await submitFeedbackApi({ content: form.value.content.trim(), contactInfo: form.value.contact?.trim() || undefined })
    ElMessage.success('已收到您的反馈，感谢支持！')
    form.value = { content: '', contact: '' }
    formRef.value?.resetFields()
    await loadMyFeedback()
  } catch (e) {
    ElMessage.error(e?.message || '提交失败，请稍后重试')
  } finally { submitting.value = false }
}
</script>

<style scoped>
.help-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  max-width: 800px;
}

.help-card__title {
  font-size: 16px;
  font-weight: 600;
}

.help-desc {
  margin: 0 0 20px 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.help-section__title {
  margin: 0 0 12px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.help-faq {
  margin-bottom: 24px;
}

.help-faq p {
  margin: 0;
  font-size: 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.help-feedback {
  margin-bottom: 8px;
}

.help-recent {
  margin-top: 20px;
}

.recent-item {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.recent-item:last-child { border-bottom: none; }

.recent-item__content {
  margin: 0 0 4px 0;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

.recent-item__meta {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
