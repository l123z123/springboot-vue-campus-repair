<template>
  <div class="settings-page">
    <el-card shadow="never" class="settings-card">
      <template #header><span class="card-title">修改密码</span></template>
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px" style="max-width: 480px">
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
          <el-button type="primary" :loading="pwdSaving" @click="onSavePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="settings-card">
      <template #header><span class="card-title">系统设置</span></template>
      <div class="setting-item" @click="showAbout = true">
        <div class="setting-item__icon is-about">
          <el-icon :size="18"><InfoFilled /></el-icon>
        </div>
        <div class="setting-item__body">
          <span class="setting-item__label">关于系统</span>
          <span class="setting-item__desc">版本信息与技术栈</span>
        </div>
        <el-icon class="setting-item__arrow"><ArrowRight /></el-icon>
      </div>
      <div class="setting-item" @click="showPrivacy = true">
        <div class="setting-item__icon is-privacy">
          <el-icon :size="18"><Lock /></el-icon>
        </div>
        <div class="setting-item__body">
          <span class="setting-item__label">隐私说明</span>
          <span class="setting-item__desc">数据收集与使用政策</span>
        </div>
        <el-icon class="setting-item__arrow"><ArrowRight /></el-icon>
      </div>
    </el-card>

    <el-dialog v-model="showAbout" title="关于系统" width="500px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="系统名称">校园报修管理系统</el-descriptions-item>
        <el-descriptions-item label="版本号">v1.0.0</el-descriptions-item>
        <el-descriptions-item label="前端技术">Vue 3 + Element Plus + Vite</el-descriptions-item>
        <el-descriptions-item label="后端技术">SpringBoot 2.x + MySQL 8 + Redis</el-descriptions-item>
      </el-descriptions>
      <p style="margin-top: 16px; color: var(--el-text-color-regular); line-height: 1.6;">本系统为毕业设计项目，提供校园报修工单的全流程管理。</p>
    </el-dialog>

    <el-dialog v-model="showPrivacy" title="隐私说明" width="500px">
      <p style="line-height: 1.8; color: var(--el-text-color-regular);">
        本系统仅收集与报修服务相关的必要信息（姓名、学号/工号、联系方式、报修位置）。
        所有数据仅用于校内报修服务管理，不会向第三方披露。
      </p>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, InfoFilled, Lock } from '@element-plus/icons-vue'
import { updatePassword } from '@/api/user'

defineOptions({ name: 'Settings' })

const showAbout = ref(false)
const showPrivacy = ref(false)

const pwdFormRef = ref(null)
const pwdSaving = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

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

async function onSavePassword() {
  try { await pwdFormRef.value?.validate() } catch { return }
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
    ElMessage.error(e?.message || '修改失败，请检查原密码是否正确')
  } finally { pwdSaving.value = false }
}
</script>

<style scoped>
.settings-page { width: 100%; display: flex; flex-direction: column; gap: 20px; }

.settings-card {
  border-radius: 12px;
  border: 1px solid var(--el-border-color-lighter);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.card-title { font-size: 15px; font-weight: 600; }

.setting-item {
  display: flex; align-items: center; gap: 14px;
  padding: 16px 4px; border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer; transition: background 0.15s;
}
.setting-item:last-child { border-bottom: none; }
.setting-item:hover { background: var(--el-fill-color-lighter); margin: 0 -12px; padding: 16px 16px; border-radius: 8px; }

.setting-item__icon {
  width: 38px; height: 38px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.setting-item__icon.is-about { background: var(--el-color-primary-light-9); color: var(--el-color-primary); }
.setting-item__icon.is-privacy { background: var(--el-color-success-light-9); color: var(--el-color-success); }

.setting-item__body { flex: 1; min-width: 0; }
.setting-item__label { display: block; font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); }
.setting-item__desc { display: block; font-size: 12px; color: var(--el-text-color-placeholder); margin-top: 2px; }
.setting-item__arrow { color: var(--el-text-color-placeholder); flex-shrink: 0; }
</style>
