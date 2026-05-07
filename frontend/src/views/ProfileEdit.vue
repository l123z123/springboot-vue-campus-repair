<template>
  <div class="profile-edit-page">
    <header class="edit-header">
      <el-button type="primary" link class="edit-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="edit-header__title">编辑资料</span>
    </header>
    <div class="edit-body">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="头像">
          <div class="edit-avatar-row">
            <el-upload
              class="avatar-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              name="file"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
              :before-upload="beforeAvatarUpload"
              accept="image/*"
            >
              <img v-if="form.avatarUrl" :src="avatarDisplayUrl" class="avatar-img" />
              <div v-else class="avatar-placeholder">
                <el-icon><UserFilled /></el-icon>
              </div>
            </el-upload>
            <div class="avatar-tip">建议使用清晰的人像照片，支持 jpg、png，单张不超过 2MB</div>
          </div>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" show-word-limit />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">女</el-radio>
            <el-radio :value="0">保密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="所属院系 / 部门" prop="department">
          <el-select v-model="form.department" placeholder="请选择院系/部门" clearable>
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号码" />
        </el-form-item>

        <el-form-item label="个性签名" prop="signature">
          <el-input
            v-model="form.signature"
            type="textarea"
            :rows="3"
            maxlength="50"
            show-word-limit
            placeholder="写点什么，让别人更了解你（最多 50 字）"
          />
        </el-form-item>

        <el-form-item>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserProfile } from '@/api/user'
import { normalizeImageUrl } from '@/utils/image'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const saving = ref(false)
const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${baseURL.replace(/\/$/, '')}/file/upload` : `${baseURL}/file/upload`
})
const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})
const avatarDisplayUrl = computed(() => normalizeImageUrl(form.value.avatarUrl))

const form = ref({
  avatarUrl: '',
  nickname: '',
  gender: 0,
  department: '',
  phone: '',
  signature: ''
})

const departmentOptions = [
  { value: '计算机学院', label: '计算机学院' },
  { value: '机械工程学院', label: '机械工程学院' },
  { value: '经济管理学院', label: '经济管理学院' },
  { value: '文学院', label: '文学院' },
  { value: '后勤保障部', label: '后勤保障部' }
]

const PHONE_REG = /^1[3-9]\d{9}$/

const rules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (!PHONE_REG.test(v)) return cb(new Error('请输入有效的手机号'))
        cb()
      },
      trigger: ['blur', 'change']
    }
  ],
  signature: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (v.length > 50) return cb(new Error('个性签名最多 50 字'))
        cb()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const loadData = async () => {
  try {
    const data = await getUserInfo()
    userStore.setUserInfo(data)
    form.value = {
      avatarUrl: data.avatarUrl || data.avatar || '',
      nickname: data.nickname || data.realName || data.username || '',
      gender: data.gender ?? 0,
      department: data.department || '',
      phone: data.phone || '',
      signature: data.signature && data.signature !== '暂无签名' ? data.signature : ''
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取用户信息失败')
  }
}

onMounted(() => {
  loadData()
})

const onReset = () => {
  loadData()
  formRef.value?.clearValidate()
}

function beforeAvatarUpload(file) {
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('头像大小不能超过 2MB')
    return false
  }
  return true
}
function handleAvatarSuccess(res) {
  const data = res?.data ?? res
  const url = typeof data === 'string' ? data : data?.url
  if (url) form.value.avatarUrl = url
}
function handleAvatarError() {
  ElMessage.error('头像上传失败')
}

const onSave = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        avatarUrl: form.value.avatarUrl || null,
        nickname: form.value.nickname,
        gender: form.value.gender,
        department: form.value.department || null,
        phone: form.value.phone || null,
        signature: form.value.signature || null
      }
      const updated = await updateUserProfile(payload)
      userStore.setUserInfo(updated)
      form.value.avatarUrl = updated?.avatarUrl ?? updated?.avatar ?? form.value.avatarUrl
      ElMessage.success('资料已更新')
      router.back()
    } catch (e) {
      ElMessage.error(e?.message || '更新失败，请稍后重试')
    } finally {
      saving.value = false
    }
  })
}
</script>

<style scoped>
.profile-edit-page {
  min-height: 100vh;
  background: var(--el-fill-color-lighter);
}
.edit-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.edit-header__back {
  margin-right: 8px;
}
.edit-header__title {
  font-size: 16px;
  font-weight: 500;
}
.edit-body {
  padding: 16px;
}
.edit-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.avatar-uploader {
  cursor: pointer;
}
.avatar-img,
.avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-img {
  object-fit: cover;
}
.avatar-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
