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

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: #d0d5dd;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #b0b5bd;
}

::selection {
  background: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  html { font-size: 15px; }
}

@media (max-width: 480px) {
  html { font-size: 14px; }
}
</style>
