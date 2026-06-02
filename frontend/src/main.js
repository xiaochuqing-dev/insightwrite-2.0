import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import '@/assets/styles/global.css'
import { applyFontSizePreference } from '@/utils/preferences.js'

const app = createApp(App)
applyFontSizePreference()
app.use(router)
app.mount('#app')
