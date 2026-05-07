import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

// 全局错误捕获，避免白屏无提示
window.addEventListener('unhandledrejection', (e) => {
  console.error('[App] unhandledrejection', e.reason)
})
window.addEventListener('error', (e) => {
  console.error('[App] error', e.message, e.filename, e.lineno)
})

const app = createApp(App)
const pinia = createPinia()

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 捕获路由组件加载失败
router.onError((err) => {
  console.error('[Router] 路由加载失败', err)
})

router.isReady().then(() => {
  try {
    app.mount('#app')
  } catch (e) {
    console.error('[App] mount failed', e)
    document.getElementById('app').innerHTML = '<div style="padding:20px;color:#f56c6c">应用初始化失败，请刷新页面或检查控制台</div>'
  }
}).catch((e) => {
  console.error('[App] router ready failed', e)
  document.getElementById('app').innerHTML = '<div style="padding:20px;color:#f56c6c">应用初始化失败，请刷新页面或检查控制台</div>'
})
