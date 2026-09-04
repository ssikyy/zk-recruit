import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import 'element-plus/dist/index.css'
import '@/styles/global.css'

import App from './App.vue'
import router from './router'
import { onUnauthorized } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn })

for (const [name, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(name, component)
}

// 会话失效时统一弹出登录框，并保留当前页面（§18）
const auth = useAuthStore(pinia)
onUnauthorized(() => {
  auth.user = null
  auth.openModal('login')
})

app.mount('#app')
