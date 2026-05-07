<template>
  <div class="app">
    <Suspense>
      <router-view v-slot="{ Component }">
        <component v-if="Component" :is="Component" />
        <div v-else class="app-loading">
          <div class="loading-spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>
      </router-view>
      <template #fallback>
        <div class="app-loading">
          <div class="loading-spinner"></div>
          <div class="loading-text">加载中...</div>
        </div>
      </template>
    </Suspense>
  </div>
</template>

<script setup>
</script>

<style>
/* 全局样式重置和统一设置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html {
  font-size: 16px;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  background: var(--el-bg-color-page);
}

.app {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

/* 加载动画 */
.app-loading {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--el-color-primary-light-7);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-text {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  font-weight: 500;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--el-fill-color-lighter);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb {
  background: var(--el-color-primary-light-5);
  border-radius: 4px;
  transition: background 0.3s ease;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--el-color-primary-light-3);
}

/* 选择文字美化 */
::selection {
  background: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
}

/* 按钮通用样式增强 */
.el-button {
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.el-button--primary {
  background: linear-gradient(135deg, var(--el-color-primary), #7c66f0);
  border: none;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

/* 输入框通用样式增强 */
.el-input__wrapper {
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.el-input__wrapper:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.el-input__wrapper.is-focus {
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.25);
}

/* 卡片通用样式增强 */
.el-card {
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border: none;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.el-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

/* 响应式基础设置 */
@media (max-width: 768px) {
  html {
    font-size: 15px;
  }
}

@media (max-width: 480px) {
  html {
    font-size: 14px;
  }
}
</style>
