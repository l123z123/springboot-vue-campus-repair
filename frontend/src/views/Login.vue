<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 左侧品牌区域 -->
      <div class="brand-section">
        <div class="brand-content">
          <div class="logo-wrapper">
            <div class="logo-icon">
              <el-icon :size="40"><Tools /></el-icon>
            </div>
          </div>
          <h1 class="brand-title">校园报修管理系统</h1>
          <p class="brand-subtitle">专业维修 · 高效服务 · 安全可靠</p>
          <div class="feature-points">
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>快速响应</span>
            </div>
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>专业服务</span>
            </div>
            <div class="feature-item">
              <el-icon><CircleCheck /></el-icon>
              <span>品质保证</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧登录表单 -->
      <div class="form-section">
        <div class="login-form-wrapper">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-desc">{{ loginMode === 'pwd' ? '账号密码登录' : '使用已绑定邮箱收取验证码登录' }}</p>
          </div>

          <el-radio-group v-model="loginMode" size="large" class="login-mode-tabs">
            <el-radio-button label="pwd">密码登录</el-radio-button>
            <el-radio-button label="email">邮箱验证码</el-radio-button>
          </el-radio-group>

          <el-form
            v-if="loginMode === 'pwd'"
            ref="formRef"
            :model="form"
            :rules="rules"
            class="login-form"
            @submit.prevent="onSubmit"
          >
            <el-form-item prop="username">
              <div class="input-field">
                <div class="input-icon">
                  <el-icon><User /></el-icon>
                </div>
                <el-input
                  v-model="form.username"
                  placeholder="请输入用户名/工号"
                  size="large"
                  class="form-input"
                />
              </div>
            </el-form-item>

            <el-form-item prop="password">
              <div class="input-field">
                <div class="input-icon">
                  <el-icon><Lock /></el-icon>
                </div>
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="请输入密码"
                  size="large"
                  show-password
                  class="form-input"
                  @keyup.enter="onSubmit"
                />
              </div>
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-button"
                @click="onSubmit"
              >
                <span v-if="!loading">立即登录</span>
                <span v-else>登录中...</span>
              </el-button>
            </el-form-item>
          </el-form>

          <el-form
            v-else
            ref="emailFormRef"
            :model="emailForm"
            :rules="emailRules"
            class="login-form"
            @submit.prevent="onEmailSubmit"
          >
            <el-form-item prop="email">
              <div class="input-field">
                <div class="input-icon">
                  <el-icon><User /></el-icon>
                </div>
                <el-input v-model="emailForm.email" placeholder="已注册的 QQ 邮箱" size="large" class="form-input" />
              </div>
            </el-form-item>
            <el-form-item prop="code">
              <div class="input-field email-code-row">
                <div class="input-icon">
                  <el-icon><Lock /></el-icon>
                </div>
                <el-input
                  v-model="emailForm.code"
                  placeholder="邮箱验证码"
                  size="large"
                  maxlength="8"
                  class="form-input"
                  @keyup.enter="onEmailSubmit"
                />
                <el-button
                  :disabled="loginSendSec > 0"
                  class="send-code-btn"
                  @click="sendLoginCode"
                >
                  {{ loginSendSec > 0 ? loginSendSec + 's' : '发送' }}
                </el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="loading"
                class="login-button"
                @click="onEmailSubmit"
              >
                <span v-if="!loading">验证并登录</span>
                <span v-else>登录中...</span>
              </el-button>
            </el-form-item>
          </el-form>

          <div class="form-footer">
            <span class="footer-link" @click="goToRegister">没有账号？点击注册</span>
            <span class="footer-link" @click="onForgotPassword">忘记密码？</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Tools, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

defineOptions({ name: 'Login' })

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const emailFormRef = ref(null)
const loading = ref(false)
const loginMode = ref('pwd')
const form = reactive({
  username: '',
  password: ''
})
const emailForm = reactive({
  email: '',
  code: ''
})
const loginSendSec = ref(0)
let loginSendTimer = null

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const emailRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '4-8 位数字', trigger: 'blur' }
  ]
}

function getHomeByRole(role) {
  const roleMap = { 2: '/admin/dashboard', 1: '/staff/workbench', 0: '/home' }
  return roleMap[role] || '/home'
}

function onForgotPassword() {
  router.push('/forgot-password')
}

function goToRegister() {
  router.push('/register')
}

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功！')
    const role = userStore.userInfo?.role ?? 0
    const target = getHomeByRole(role)

    try {
      await router.replace(target)
    } catch (navErr) {
      window.location.href = target
    }
  } catch (e) {
    ElMessage.error(e?.message || '登录失败，请检查账号密码')
  } finally {
    loading.value = false
  }
}

async function sendLoginCode() {
  if (!emailForm.email?.trim()) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  try {
    await request.post('/auth/send-email-code', {
      email: emailForm.email.trim(),
      scene: 'LOGIN'
    })
    ElMessage.success('验证码已发至邮箱，未配 SMTP 时可看后端日志')
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
    return
  }
  loginSendSec.value = 60
  if (loginSendTimer) clearInterval(loginSendTimer)
  loginSendTimer = setInterval(() => {
    loginSendSec.value -= 1
    if (loginSendSec.value <= 0 && loginSendTimer) {
      clearInterval(loginSendTimer)
      loginSendTimer = null
    }
  }, 1000)
}

async function onEmailSubmit() {
  try {
    await emailFormRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.loginByEmail(emailForm.email.trim(), emailForm.code.trim())
    ElMessage.success('登录成功！')
    const role = userStore.userInfo?.role ?? 0
    const target = getHomeByRole(role)
    try {
      await router.replace(target)
    } catch (navErr) {
      window.location.href = target
    }
  } catch (e) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (loginSendTimer) clearInterval(loginSendTimer)
})
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-container {
  display: flex;
  width: 100%;
  max-width: 1200px;
  min-height: 600px;
  background: white;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
}

/* 左侧品牌区域 */
.brand-section {
  flex: 1;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-content {
  text-align: center;
  color: white;
}

.logo-wrapper {
  margin-bottom: 32px;
}

.logo-icon {
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px 0;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.brand-subtitle {
  font-size: 15px;
  opacity: 0.9;
  margin: 0 0 48px 0;
}

.feature-points {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: center;
  opacity: 0.95;
  font-size: 16px;
}

.feature-item .el-icon {
  font-size: 22px;
}

/* 右侧表单区域 */
.form-section {
  flex: 1;
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-form-wrapper {
  width: 100%;
  max-width: 400px;
}

.form-header {
  margin-bottom: 40px;
}

.form-title {
  font-size: 28px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 8px 0;
}

.form-desc {
  font-size: 14px;
  color: #7f8c8d;
  margin: 0;
}

.login-mode-tabs {
  width: 100%;
  margin-bottom: 20px;
  display: flex;
}

.login-mode-tabs :deep(.el-radio-button) {
  flex: 1;
}

.login-mode-tabs :deep(.el-radio-button__inner) {
  width: 100%;
}

.login-form {
  margin-bottom: 32px;
}

.input-field.email-code-row {
  flex-wrap: nowrap;
  gap: 4px;
  padding-right: 6px;
}

.input-field.email-code-row .form-input {
  min-width: 0;
}

.send-code-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

.input-field {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8f9fa;
  border-radius: 14px;
  padding: 0 16px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.input-field:focus-within {
  background: white;
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.input-icon {
  color: #95a5a6;
  transition: color 0.3s ease;
  display: flex;
}

.input-field:focus-within .input-icon {
  color: #667eea;
}

.form-input {
  flex: 1;
}

.form-input :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding: 12px 8px;
}

.form-input :deep(.el-input__wrapper:hover) {
  box-shadow: none;
}

.form-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: none;
}

.login-button {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  border: none;
  color: white;
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.35);
  transition: all 0.3s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 32px rgba(102, 126, 234, 0.45);
  background: linear-gradient(135deg, #5a6fd6, #6a4190);
}

.form-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  border-top: 1px solid #ecf0f1;
}

.footer-link {
  color: #667eea;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.footer-link:hover {
  color: #5a6fd6;
  transform: translateY(-1px);
}

/* 响应式设计 */
@media (max-width: 960px) {
  .login-container {
    flex-direction: column;
    max-width: 500px;
  }

  .brand-section {
    padding: 40px 24px;
  }

  .logo-icon {
    width: 64px;
    height: 64px;
  }

  .brand-title {
    font-size: 26px;
  }

  .form-section {
    padding: 40px 24px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 16px;
  }

  .login-container {
    border-radius: 16px;
  }

  .brand-section {
    padding: 32px 20px;
  }

  .form-section {
    padding: 32px 20px;
  }
}
</style>
