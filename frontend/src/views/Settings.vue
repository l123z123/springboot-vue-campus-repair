<template>
  <div class="settings-page">
    <header class="settings-header">
      <el-button type="primary" link class="settings-header__back" @click="router.back()">
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <span class="settings-header__title">设置</span>
    </header>
    <div class="settings-body">
      <div class="settings-item" @click="ElMessage.info('关于我们')">
        <span>关于我们</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <div class="settings-item" @click="ElMessage.info('隐私政策')">
        <span>隐私政策</span>
        <el-icon><ArrowRight /></el-icon>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()
</script>

<style scoped>
.settings-page {
  min-height: 100vh;
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
</style>
