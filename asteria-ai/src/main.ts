import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
/* 本地字体（不依赖外网 CDN）：思源宋体标题、Inter 正文、Outfit 英文眉标 */
import '@fontsource/noto-serif-sc/600.css'
import '@fontsource/noto-serif-sc/700.css'
import '@fontsource-variable/inter'
import '@fontsource/outfit/500.css'
import '@fontsource/outfit/600.css'

import '@/styles/tokens.css'
import '@/styles/element-overrides.css'
import '@/styles/global.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
