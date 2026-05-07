<template>
  <div class="admin-profile">
    <el-card class="profile-card" shadow="never">
      <template #header>
        <span>基本信息</span>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="头像">
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
          <div class="avatar-tip">点击上传头像，支持 jpg、png，单张不超过 2MB</div>
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="form.nickname" maxlength="20" placeholder="请输入姓名" show-word-limit />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号码" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-select v-model="form.department" placeholder="请选择部门" clearable style="width: 100%;">
            <el-option
              v-for="item in departmentOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="onSaveBasic">保存</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="profile-card" shadow="never">
      <template #header>
        <span>修改密码</span>
      </template>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" placeholder="请输入新密码，6-20位" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="pwdSaving" @click="onSavePassword">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateUserProfile, updatePassword } from '@/api/user'
import { normalizeImageUrl } from '@/utils/image'

defineOptions({ name: 'AdminProfile' })

const userStore = useUserStore()
const formRef = ref(null)
const pwdFormRef = ref(null)
const saving = ref(false)
const pwdSaving = ref(false)

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api'
const uploadUrl = computed(() => {
  const origin = typeof window !== 'undefined' ? window.location.origin : ''
  return origin ? `${origin}${baseURL.replace(/\/$/, '')}/file/upload` : `${baseURL}/file/upload`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const form = ref({
  avatarUrl: '',
  nickname: '',
  department: '',
  phone: ''
})

const pwdForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const departmentOptions = [
  { value: '计算机学院', label: '计算机学院' },
  { value: '机械工程学院', label: '机械工程学院' },
  { value: '经济管理学院', label: '经济管理学院' },
  { value: '后勤保障部', label: '后勤保障部' },
  { value: '校务处', label: '校务处' }
]

const PHONE_REG = /^1[3-9]\d{9}$/

const rules = {
  nickname: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [
    {
      validator: (_r, v, cb) => {
        if (!v) return cb()
        if (!PHONE_REG.test(v)) return cb(new Error('请输入有效的手机号'))
        cb()
      },
      trigger: ['blur', 'change']
    }
  ]
}

const validateConfirmPwd = (_r, v, cb) => {
  if (v !== pwdForm.value.newPassword) cb(new Error('两次输入的密码不一致'))
  else cb()
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

const avatarDisplayUrl = computed(() => {
  const url = form.value.avatarUrl || userStore.userInfo?.avatarUrl || userStore.userInfo?.avatar
  const normalized = normalizeImageUrl(url)
  return normalized ? `${normalized}?t=${avatarTimestamp.value}` : ''
})
const avatarTimestamp = ref(0)

async function loadData() {
  try {
    const data = await getUserInfo()
    userStore.setUserInfo(data)
    form.value = {
      avatarUrl: data.avatarUrl || data.avatar || '',
      nickname: data.nickname || data.realName || data.username || '',
      department: data.department || '',
      phone: data.phone || ''
    }
  } catch (e) {
    ElMessage.error(e?.message || '获取用户信息失败')
  }
}

function onReset() {
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
  if (url) {
    form.value.avatarUrl = url
  }
}

function handleAvatarError() {
  ElMessage.error('头像上传失败')
}

async function onSaveBasic() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = {
      avatarUrl: form.value.avatarUrl || null,
      nickname: form.value.nickname,
      department: form.value.department || null,
      phone: form.value.phone || null
    }
    const updated = await updateUserProfile(payload)
    userStore.setUserInfo(updated)
    form.value.avatarUrl = updated?.avatarUrl ?? updated?.avatar ?? form.value.avatarUrl
    avatarTimestamp.value = Date.now()
    ElMessage.success('资料已更新')
  } catch (e) {
    ElMessage.error(e?.message || '更新失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function onSavePassword() {
  try {
    await pwdFormRef.value?.validate()
  } catch {
    return
  }
  pwdSaving.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功')
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    pwdFormRef.value?.resetFields()
  } catch (e) {
    ElMessage.error(e?.message || '修改失败')
  } finally {
    pwdSaving.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-profile {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-card {
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.avatar-uploader {
  cursor: pointer;
}

.avatar-img,
.avatar-placeholder {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  object-fit: cover;
}

.avatar-placeholder .el-icon {
  font-size: 40px;
  color: var(--el-text-color-placeholder);
}

.avatar-tip {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 8px;
}
</style>
