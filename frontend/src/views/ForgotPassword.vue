<template>
  <div class="forgot-password-container">
    <div v-if="!isMobile" class="forgot-password-pc-wrapper">
      <div class="pc-left-panel">
        <div class="brand-content">
          <div class="logo-wrap" aria-hidden="true">
            <el-icon class="logo-icon" :size="56"><Reading /></el-icon>
          </div>
          <h1 class="system-title">校园报修管理系统</h1>
          <p class="system-slogan">智慧运维 · 高效服务 · 安全可靠</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon class="feature-ico"><Message /></el-icon>
              <span class="feature-text">邮箱验证码，安全重置密码</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-ico"><Lock /></el-icon>
              <span class="feature-text">账号与邮箱须与系统中一致</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pc-right-panel">
        <div class="forgot-password-form-card">
          <h2 class="form-title">忘记密码</h2>
          <p class="form-subtitle">通过绑定邮箱接收验证码，设置新密码</p>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="92px"
            class="forgot-form"
            @submit.prevent="onSubmit"
          >
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="登录用户名" clearable autocomplete="username" />
            </el-form-item>
            <el-form-item label="绑定邮箱" prop="email">
              <el-input v-model="form.email" placeholder="该账号在系统中登记的邮箱" clearable autocomplete="email" />
            </el-form-item>
            <el-form-item label="邮箱验证码" prop="code">
              <div class="code-row">
                <el-input
                  v-model="form.code"
                  placeholder="6 位数字"
                  maxlength="8"
                  clearable
                  autocomplete="one-time-code"
                />
                <el-button type="primary" plain :disabled="sendSec > 0" :loading="sendingCode" @click="sendCode">
                  {{ sendSec > 0 ? `${sendSec} s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="form.newPassword"
                type="password"
                placeholder="至少 6 位"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="再次输入新密码"
                show-password
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="loading" class="forgot-password-btn-pc" @click="onSubmit">
                重置密码
              </el-button>
            </el-form-item>
          </el-form>
          <div class="footer-links">
            <span class="footer-link" @click="goToLogin">返回登录</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="forgot-password-mobile-wrapper">
      <div class="mobile-header">
        <el-icon class="mobile-logo" :size="36"><Reading /></el-icon>
        <h1 class="forgot-password-mobile__title">忘记密码</h1>
        <p class="mobile-sub">邮箱验证码重置</p>
      </div>
      <div class="forgot-password-mobile__inner">
        <el-form ref="formRef" :model="form" :rules="rules" class="forgot-password-mobile__form" @submit.prevent="onSubmit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" autocomplete="username" />
          </el-form-item>
          <el-form-item prop="email">
            <el-input v-model="form.email" placeholder="绑定邮箱" autocomplete="email" />
          </el-form-item>
          <el-form-item prop="code">
            <div class="mobile-code-line">
              <el-input v-model="form.code" placeholder="邮箱验证码" maxlength="8" autocomplete="one-time-code" />
              <el-button type="primary" plain :disabled="sendSec > 0" :loading="sendingCode" @click="sendCode">
                {{ sendSec > 0 ? sendSec + 's' : '获取' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item prop="newPassword">
            <el-input v-model="form.newPassword" type="password" placeholder="新密码（≥6位）" show-password />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认新密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="forgot-password-btn-mobile" @click="onSubmit">
              重置密码
            </el-button>
          </el-form-item>
          <div class="footer-links footer-links-mobile">
            <span class="footer-link" @click="goToLogin">返回登录</span>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Reading, Message, Lock } from '@element-plus/icons-vue'
import request from '@/utils/request'

defineOptions({ name: 'ForgotPassword' })

const router = useRouter()
const isMobile = ref(window.innerWidth < 768)
const formRef = ref(null)
const loading = ref(false)
const sendingCode = ref(false)
const sendSec = ref(0)
let sendTimer = null

const form = reactive({
  username: '',
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请填写验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '验证码为 4-8 位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

function goToLogin() {
  router.push('/login')
}

function startSendCooldown() {
  sendSec.value = 60
  if (sendTimer) clearInterval(sendTimer)
  sendTimer = setInterval(() => {
    sendSec.value -= 1
    if (sendSec.value <= 0 && sendTimer) {
      clearInterval(sendTimer)
      sendTimer = null
    }
  }, 1000)
}

async function sendCode() {
  if (!form.username?.trim()) {
    ElMessage.warning('请先填写用户名')
    return
  }
  if (!form.email?.trim()) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  sendingCode.value = true
  try {
    await request.post('/auth/send-email-code', {
      email: form.email.trim(),
      scene: 'RESET_PASSWORD',
      username: form.username.trim()
    })
    ElMessage.success('已发送，请查收邮箱（未配置 SMTP 时可查看后端日志中的验证码）')
    startSendCooldown()
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
  } finally {
    sendingCode.value = false
  }
}

onUnmounted(() => {
  if (sendTimer) clearInterval(sendTimer)
})

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await request.post('/auth/reset-password', {
      username: form.username.trim(),
      email: form.email.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    ElMessage.success('密码重置成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e?.message || '密码重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.forgot-password-container {
  min-height: 100vh;
}

.forgot-password-pc-wrapper {
  min-height: 100vh;
  display: flex;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.pc-left-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
  padding: 48px;
  position: relative;
  overflow: hidden;
  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.1);
  }
  &::before {
    width: 360px;
    height: 360px;
    top: -120px;
    right: -80px;
  }
  &::after {
    width: 220px;
    height: 220px;
    left: -60px;
    bottom: -60px;
  }
}

.brand-content {
  text-align: center;
  color: #fff;
  position: relative;
  z-index: 1;
}
.logo-wrap {
  margin-bottom: 16px;
}
.logo-icon {
  color: rgba(255, 255, 255, 0.95);
}
.system-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 16px;
  letter-spacing: 2px;
}
.system-slogan {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.92);
  margin: 0 0 32px;
}
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
  max-width: 320px;
  margin: 0 auto;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 14px;
}
.feature-ico {
  font-size: 20px;
}

.pc-right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.forgot-password-form-card {
  width: 100%;
  max-width: 480px;
  background: #fff;
  border-radius: 22px;
  padding: 44px;
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 8px;
  color: #0f172a;
}
.form-subtitle {
  font-size: 14px;
  color: #475569;
  margin: 0 0 28px;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
  .el-input {
    flex: 1;
  }
}

.forgot-password-btn-pc {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  box-shadow: 0 10px 28px -6px rgba(102, 126, 234, 0.45);
}

.forgot-form :deep(.el-input__wrapper) {
  min-height: 44px;
  border-radius: 12px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
}
.forgot-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #667eea inset, 0 0 0 3px rgba(102, 126, 234, 0.12);
}

.footer-links {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  color: #334155;
  display: flex;
  justify-content: center;
}
.footer-link {
  cursor: pointer;
  color: #2563eb;
  &:hover {
    text-decoration: underline;
  }
}

.forgot-password-mobile-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px;
}
.mobile-header {
  text-align: center;
  padding: 32px 0 16px;
  color: #fff;
}
.mobile-logo {
  display: block;
  margin: 0 auto 8px;
}
.forgot-password-mobile__title {
  font-size: 24px;
  font-weight: 700;
  margin: 0;
}
.mobile-sub {
  margin: 8px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
}
.forgot-password-mobile__inner {
  flex: 1;
  background: #fff;
  border-radius: 20px;
  padding: 28px 20px;
  max-width: 400px;
  margin: 0 auto;
  width: 100%;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}
.mobile-code-line {
  display: flex;
  gap: 8px;
  width: 100%;
  .el-input {
    flex: 1;
  }
}
.forgot-password-btn-mobile {
  width: 100%;
  height: 48px;
  border-radius: 24px;
  font-weight: 600;
}
.footer-links-mobile {
  border-top: none;
  padding-top: 8px;
}
</style>
