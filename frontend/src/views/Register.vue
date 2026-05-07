<template>
  <div class="register-container">
    <!-- PC 端：左右分栏 -->
    <div v-if="!isMobile" class="register-pc-wrapper">
      <div class="pc-left-panel">
        <div class="brand-content">
          <div class="logo-wrap" aria-hidden="true">
            <el-icon class="logo-icon" :size="56"><Reading /></el-icon>
          </div>
          <h1 class="system-title">校园报修管理系统</h1>
          <p class="system-slogan">智慧运维 · 高效服务 · 安全可靠</p>
          <div class="feature-list">
            <div class="feature-item">
              <el-icon class="feature-ico"><Promotion /></el-icon>
              <span class="feature-text">快速报修，响应及时</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-ico"><Iphone /></el-icon>
              <span class="feature-text">手机端 + PC 端，随时可用</span>
            </div>
            <div class="feature-item">
              <el-icon class="feature-ico"><CircleCheck /></el-icon>
              <span class="feature-text">流程透明，进度可查</span>
            </div>
          </div>
        </div>
      </div>
      <div class="pc-right-panel">
        <div class="register-form-card">
          <div class="form-header">
            <h2 class="form-title">创建您的账号</h2>
            <p class="form-subtitle form-subtitle--emphasis">请填写信息完成学生账号注册（维修工账号由管理员创建）</p>
          </div>
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            class="register-form"
            label-position="top"
            @submit.prevent="onSubmit"
          >
            <div class="form-section">
              <h3 class="form-section__title">账号与邮箱</h3>
              <el-form-item prop="username" label="用户名" class="form-item-full">
                <div class="input-wrapper">
                  <el-icon class="input-ico" :size="20"><User /></el-icon>
                  <el-input v-model="form.username" placeholder="3～20 位，学号 / 工号形式" size="large" clearable />
                </div>
              </el-form-item>
              <el-form-item prop="email" label="邮箱" class="form-item-full">
                <div class="input-wrapper">
                  <el-icon class="input-ico" :size="20"><Message /></el-icon>
                  <el-input v-model="form.email" placeholder="用于登录与收取验证码" size="large" clearable />
                </div>
              </el-form-item>
              <el-form-item prop="emailCode" label="邮箱验证码" class="form-item-full form-item--code">
                <div class="code-row">
                  <div class="input-wrapper input-wrapper--grow">
                    <el-icon class="input-ico" :size="20"><Key /></el-icon>
                    <el-input
                      v-model="form.emailCode"
                      placeholder="6 位数字"
                      maxlength="8"
                      size="large"
                      name="email-verification-code"
                      autocomplete="one-time-code"
                      @keyup.enter="sendRegisterCode"
                    />
                  </div>
                  <el-button
                    type="primary"
                    plain
                    :disabled="regSendSec > 0"
                    class="code-btn"
                    size="large"
                    @click="sendRegisterCode"
                  >
                    {{ regSendSec > 0 ? `${regSendSec} s` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </div>

            <div class="form-section">
              <h3 class="form-section__title">设置密码</h3>
              <div class="form-row form-row--split">
                <el-form-item prop="password" label="密码" class="form-item-full">
                  <div class="input-wrapper">
                    <el-icon class="input-ico" :size="20"><Lock /></el-icon>
                    <el-input
                      v-model="form.password"
                      type="password"
                      placeholder="至少 6 位"
                      show-password
                      size="large"
                    />
                  </div>
                </el-form-item>
                <el-form-item prop="confirmPassword" label="确认密码" class="form-item-full">
                  <div class="input-wrapper">
                    <el-icon class="input-ico" :size="20"><Lock /></el-icon>
                    <el-input
                      v-model="form.confirmPassword"
                      type="password"
                      placeholder="再次输入密码"
                      show-password
                      size="large"
                    />
                  </div>
                </el-form-item>
              </div>
            </div>

            <div class="form-section">
              <h3 class="form-section__title">个人信息</h3>
              <div class="form-row form-row--split">
                <el-form-item prop="realName" label="真实姓名" class="form-item-full">
                  <div class="input-wrapper">
                    <el-icon class="input-ico" :size="20"><UserFilled /></el-icon>
                    <el-input v-model="form.realName" placeholder="与校园证件一致" size="large" clearable />
                  </div>
                </el-form-item>
                <el-form-item prop="phone" label="手机号" class="form-item-full">
                  <div class="input-wrapper">
                    <el-icon class="input-ico" :size="20"><Iphone /></el-icon>
                    <el-input v-model="form.phone" placeholder="11 位中国大陆号码" size="large" clearable />
                  </div>
                </el-form-item>
              </div>
              <el-form-item prop="department" label="院系 / 部门" class="form-item-full">
                <div class="input-wrapper">
                  <el-icon class="input-ico" :size="20"><OfficeBuilding /></el-icon>
                  <el-input v-model="form.department" placeholder="例如：计算机学院" size="large" clearable />
                </div>
              </el-form-item>
            </div>

            <el-form-item class="form-actions">
              <el-button type="primary" :loading="loading" class="register-btn" @click="onSubmit" size="large">
                <span v-if="!loading">完成注册</span>
                <span v-else>注册中…</span>
              </el-button>
            </el-form-item>
          </el-form>
          <div class="footer-links">
            <span class="footer-text">已有账号？</span>
            <span class="footer-link" @click="goToLogin">立即登录 →</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 移动端：全屏流式 App 风格 -->
    <div v-else class="register-mobile-wrapper">
      <div class="mobile-header">
        <el-icon class="mobile-logo" :size="40"><Reading /></el-icon>
        <h1 class="register-mobile__title">校园报修</h1>
        <p class="register-mobile__subtitle">创建学生账号</p>
      </div>
      <div class="register-mobile__inner">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="register-mobile__form"
          label-position="top"
          @submit.prevent="onSubmit"
        >
          <p class="register-mobile__sec">账号与邮箱</p>
          <el-form-item prop="username" label="用户名">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><User /></el-icon>
              <el-input v-model="form.username" placeholder="学号/工号形式" />
            </div>
          </el-form-item>
          <el-form-item prop="email" label="邮箱">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><Message /></el-icon>
              <el-input v-model="form.email" placeholder="收验证码" />
            </div>
          </el-form-item>
          <el-form-item prop="emailCode" label="验证码" class="mobile-code-row">
            <div class="mobile-code-line">
              <div class="input-wrapper-mobile input-wrapper-mobile--code">
                <el-icon class="input-ico" :size="18"><Key /></el-icon>
                <el-input
                  v-model="form.emailCode"
                  placeholder="6 位数字"
                  maxlength="8"
                  name="email-verification-code"
                  autocomplete="one-time-code"
                />
              </div>
              <el-button
                type="primary"
                plain
                :disabled="regSendSec > 0"
                class="code-btn-mobile"
                @click="sendRegisterCode"
              >
                {{ regSendSec > 0 ? regSendSec + 's' : '获取' }}
              </el-button>
            </div>
          </el-form-item>
          <p class="register-mobile__sec">密码</p>
          <el-form-item prop="password" label="密码">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><Lock /></el-icon>
              <el-input
                v-model="form.password"
                type="password"
                placeholder="至少 6 位"
                show-password
              />
            </div>
          </el-form-item>
          <el-form-item prop="confirmPassword" label="确认">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><Lock /></el-icon>
              <el-input
                v-model="form.confirmPassword"
                type="password"
                placeholder="再次输入"
                show-password
              />
            </div>
          </el-form-item>
          <p class="register-mobile__sec">个人资料</p>
          <el-form-item prop="realName" label="姓名">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><UserFilled /></el-icon>
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </div>
          </el-form-item>
          <el-form-item prop="phone" label="手机">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><Iphone /></el-icon>
              <el-input v-model="form.phone" placeholder="11 位号码" />
            </div>
          </el-form-item>
          <el-form-item prop="department" label="院系">
            <div class="input-wrapper-mobile">
              <el-icon class="input-ico" :size="18"><OfficeBuilding /></el-icon>
              <el-input v-model="form.department" placeholder="院系/部门" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="register-btn-mobile" @click="onSubmit">
              <span v-if="!loading">完成注册</span>
              <span v-else>注册中…</span>
            </el-button>
          </el-form-item>
          <div class="footer-links footer-links-mobile">
            <span class="footer-text">已有账号？</span>
            <span class="footer-link" @click="goToLogin">立即登录 →</span>
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
import {
  User,
  Message,
  Key,
  Lock,
  UserFilled,
  Iphone,
  OfficeBuilding,
  Reading,
  Promotion,
  CircleCheck
} from '@element-plus/icons-vue'
import request from '@/utils/request'

defineOptions({ name: 'Register' })

const router = useRouter()

// 设备检测：< 768px 为移动端
const isMobile = ref(window.innerWidth < 768)

const formRef = ref(null)
const loading = ref(false)
const form = reactive({
  username: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  department: ''
})

const regSendSec = ref(0)
let regSendTimer = null

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: '请填写邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{4,8}$/, message: '验证码为 4-8 位数字', trigger: 'blur' }
  ],
  department: [
    { required: true, message: '请输入院系或部门', trigger: 'blur' }
  ]
}

function goToLogin() {
  router.push('/login')
}

async function sendRegisterCode() {
  if (!form.email?.trim()) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  try {
    await request.post('/auth/send-email-code', {
      email: form.email.trim(),
      scene: 'REGISTER'
    })
    ElMessage.success('已发送，请查收 QQ 邮箱（未配置 SMTP 时可看后端控制台日志中的验证码）')
  } catch (e) {
    ElMessage.error(e?.message || '发送失败')
    return
  }
  regSendSec.value = 60
  if (regSendTimer) clearInterval(regSendTimer)
  regSendTimer = setInterval(() => {
    regSendSec.value -= 1
    if (regSendSec.value <= 0 && regSendTimer) {
      clearInterval(regSendTimer)
      regSendTimer = null
    }
  }, 1000)
}

onUnmounted(() => {
  if (regSendTimer) clearInterval(regSendTimer)
})

async function onSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await request.post('/auth/register', {
      username: form.username,
      email: form.email.trim(),
      emailCode: form.emailCode.trim(),
      password: form.password,
      realName: form.realName,
      phone: form.phone,
      role: 0,
      department: form.department
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
// 样式变量
$bg-pc: #f5f7fa;
$primary-color: #409eff;
$primary-gradient: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
$secondary-gradient: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
$mobile-input-height: 52px;
$mobile-btn-height: 52px;
$mobile-font-input: 16px;
$mobile-font-btn: 17px;
$mobile-radius-btn: 26px;
$mobile-spacing: 20px;

/* PC 端：左右分栏 */
.register-container {
  min-height: 100vh;
}

.register-pc-wrapper {
  min-height: 100vh;
  display: flex;
  background: $bg-pc;
}

.pc-left-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: $secondary-gradient;
  padding: 48px;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 70%);
    top: -100px;
    right: -100px;
  }
  
  &::after {
    content: '';
    position: absolute;
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(255,255,255,0.08) 0%, transparent 70%);
    bottom: -50px;
    left: -50px;
  }
}

.brand-content {
  text-align: center;
  color: #fff;
  position: relative;
  z-index: 1;
}

.logo-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100px;
  height: 100px;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  animation: float 3s ease-in-out infinite;
}

.logo-icon {
  color: #fff;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

.system-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 16px 0;
  letter-spacing: 2px;
  text-shadow: 0 2px 10px rgba(0,0,0,0.2);
}

.system-slogan {
  font-size: 16px;
  opacity: 0.9;
  margin: 0 0 40px 0;
  letter-spacing: 1px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 40px;
}

.feature-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 24px;
  background: rgba(255,255,255,0.1);
  border-radius: 12px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  
  &:hover {
    background: rgba(255,255,255,0.15);
    transform: translateX(5px);
  }
}

.feature-ico {
  font-size: 22px;
  color: #e0e7ff;
  flex-shrink: 0;
}

.feature-text {
  font-size: 15px;
  font-weight: 500;
}

.pc-right-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.register-form-card {
  width: 100%;
  max-width: 560px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 44px 36px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.06), 0 20px 50px -12px rgba(15, 23, 42, 0.12);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.form-header {
  text-align: center;
  margin-bottom: 28px;
}

.form-title {
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px 0;
  color: #0f172a;
  letter-spacing: 0.5px;
}

.form-subtitle {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin: 0 0 16px 0;
  line-height: 1.5;
}

.form-subtitle--emphasis {
  color: #334155;
  font-weight: 500;
  margin-bottom: 20px;
}

.register-form {
  :deep(.el-form-item__label) {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    margin-bottom: 6px;
    line-height: 1.2;
  }

  .form-section {
    margin-bottom: 8px;
    padding-bottom: 4px;
    & + .form-section {
      margin-top: 8px;
      padding-top: 20px;
      border-top: 1px solid var(--el-border-color-lighter);
    }
  }

  .form-section__title {
    font-size: 12px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: #475569;
    margin: 0 0 14px 0;
    padding-left: 10px;
    border-left: 3px solid #6366f1;
  }

  .form-row--split {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 0 16px;
    @media (max-width: 560px) {
      grid-template-columns: 1fr;
    }
  }

  .form-item-full {
    width: 100%;
  }

  .form-item--code {
    :deep(.el-form-item__content) {
      line-height: normal;
    }
  }

  .code-row {
    display: flex;
    align-items: stretch;
    gap: 12px;
    width: 100%;
  }

  .form-actions {
    margin-bottom: 0;
    margin-top: 8px;
  }

  .el-form-item {
    margin-bottom: 18px;
  }

  .code-btn {
    flex-shrink: 0;
    min-width: 120px;
    padding: 0 20px;
    font-weight: 600;
    border-radius: 12px;
  }
}

.input-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 14px 0 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, background 0.2s ease;

  &--grow {
    flex: 1;
    min-width: 0;
  }
  
  &:hover {
    border-color: #cbd5e1;
    background: #fff;
  }
  
  &:focus-within {
    border-color: var(--el-color-primary);
    background: #fff;
    box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
    .input-ico {
      color: var(--el-color-primary);
    }
  }
  
  .input-ico {
    flex-shrink: 0;
    color: #475569;
    transition: color 0.2s ease;
  }
  
  :deep(.el-input) {
    flex: 1;
    
    .el-input__wrapper {
      box-shadow: none;
      padding: 0;
      background: transparent;
      
      &.is-focus {
        box-shadow: none;
      }
    }
  }
  
  :deep(.el-select) {
    flex: 1;
    
    .el-input__wrapper {
      box-shadow: none;
      padding: 0;
      background: transparent;
      
      &.is-focus {
        box-shadow: none;
      }
    }
  }
}

.register-btn {
  width: 100%;
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
  border-radius: 12px;
  background: $primary-gradient;
  border: none;
  box-shadow: 0 10px 28px -6px rgba(102, 126, 234, 0.55);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  margin-top: 4px;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 32px rgba(102, 126, 234, 0.5);
  }
  
  &:active {
    transform: translateY(0);
  }
}

.footer-links {
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

.footer-text {
  color: var(--el-text-color-secondary);
}

.footer-link {
  cursor: pointer;
  color: var(--el-color-primary);
  font-weight: 600;
  transition: all 0.2s;
  outline: none;
  user-select: none;
  
  &:hover {
    color: #7c66f0;
    transform: translateX(3px);
  }
  
  &:focus {
    outline: none;
  }
}

.footer-links-mobile {
  margin-top: 24px;
  padding-top: 20px;
}

/* 移动端：全屏流式 App 风格 */
.register-mobile-wrapper {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: $primary-gradient;
  padding: 20px 16px;
}

.mobile-header {
  text-align: center;
  padding: 32px 0 20px 0;
}

.register-mobile__sec {
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
  letter-spacing: 0.06em;
  margin: 4px 0 8px;
  padding-left: 8px;
  border-left: 2px solid #6366f1;
}

.mobile-logo {
  color: #fff;
  margin-bottom: 12px;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.15));
  animation: float 3s ease-in-out infinite;
}

.register-mobile__title {
  font-size: 28px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px 0;
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.register-mobile__subtitle {
  font-size: 15px;
  color: rgba(255,255,255,0.85);
  margin: 0;
}

.register-mobile__inner {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  width: 100%;
  max-width: 400px;
  margin: 0 auto;
  background: #fff;
  border-radius: 20px;
  padding: 24px 20px 28px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.input-wrapper-mobile {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 14px;
  background: var(--el-bg-color);
  border: 2px solid var(--el-border-color-lighter);
  border-radius: 12px;
  transition: all 0.3s ease;
  
  &:hover {
    border-color: var(--el-color-primary-light-5);
  }
  
  &:focus-within {
    border-color: var(--el-color-primary);
    box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  }
  
  .input-icon {
    font-size: 18px;
    flex-shrink: 0;
  }
  
  :deep(.el-input),
  :deep(.el-select) {
    flex: 1;
    
    .el-input__wrapper {
      box-shadow: none;
      padding: 0;
      background: transparent;
      
      &.is-focus {
        box-shadow: none;
      }
    }
  }
}

/* 移动端 Element Plus 组件覆盖 */
@media (max-width: 767px) {
  .register-mobile__inner :deep(.el-input__wrapper) {
    height: $mobile-input-height;
    min-height: $mobile-input-height;
    padding: 0 12px;
  }

  .register-mobile__inner :deep(.el-input__inner) {
    font-size: $mobile-font-input;
  }

  .register-mobile__inner :deep(.el-form-item) {
    margin-bottom: $mobile-spacing;
  }

  .register-mobile__inner :deep(.el-form-item__label) {
    display: none;
  }

  .register-btn-mobile {
    width: 100%;
    height: $mobile-btn-height;
    font-size: $mobile-font-btn;
    font-weight: 600;
    border-radius: $mobile-radius-btn;
    background: $primary-gradient;
    border: none;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.4);
    margin-top: 8px;
  }

  .register-mobile__inner :deep(.mobile-code-row) {
    .el-form-item__content {
      display: block;
    }
  }

  .mobile-code-line {
    display: flex;
    align-items: stretch;
    gap: 10px;
  }

  .input-wrapper-mobile--code {
    flex: 1;
    min-width: 0;
  }

  .code-btn-mobile {
    flex-shrink: 0;
    min-width: 88px;
    font-weight: 600;
    border-radius: 12px;
  }
}
</style>