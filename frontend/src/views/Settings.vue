<template>
  <div class="settings-page">
    <header class="settings-header">
      <el-button type="primary" link class="settings-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="settings-header__title">设置</span>
    </header>
    <div class="settings-body">
      <div class="settings-item" @click="showAbout = true">
        <span>关于系统</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="settings-item" @click="showPrivacy = true">
        <span>隐私说明</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>

    <div class="settings-version">
      <p>校园报修管理系统 v1.0</p>
      <p class="settings-version__tech">SpringBoot + Vue3 + Element Plus + MySQL</p>
    </div>

    <el-dialog v-model="showAbout" title="关于系统" width="400px">
      <div class="about-content">
        <p><strong>校园报修管理系统</strong></p>
        <p>版本：v1.0.0</p>
        <p>技术栈：SpringBoot 2.x + Vue 3 + Element Plus + MySQL 8 + Redis</p>
        <p style="margin-top: 12px; color: #666">本系统为毕业设计项目，提供校园报修工单的全流程管理，包括报修提交、工单派发、维修跟踪、服务评价等功能。</p>
      </div>
    </el-dialog>

    <el-dialog v-model="showPrivacy" title="隐私说明" width="400px">
      <div class="about-content">
        <p>本系统仅收集与报修服务相关的必要信息（姓名、学号/工号、联系方式、报修位置），所有数据仅用于校内报修服务管理，不会向第三方披露。</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
const showAbout = ref(false)
const showPrivacy = ref(false)
</script>

<style scoped>
.settings-page {
  min-height: 400px;
  background: var(--el-fill-color-lighter);
}
.settings-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.settings-header__back {
  margin-right: 8px;
}
.settings-header__title {
  font-size: 16px;
  font-weight: 500;
}
.settings-body {
  margin-top: 16px;
  background: var(--el-bg-color);
  padding: 0 16px;
}
.settings-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 44px;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
}
.settings-item:last-child {
  border-bottom: none;
}
.settings-version {
  margin-top: 32px;
  text-align: center;
  padding: 20px;
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
.settings-version p {
  margin: 4px 0;
}
.settings-version__tech {
  font-size: 11px;
  color: var(--el-text-color-disabled);
}
.about-content p {
  margin: 6px 0;
  line-height: 1.6;
}
</style>
