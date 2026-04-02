import { createApp } from 'vue'
import App from './App.vue'
import pinia from './stores'
import router from './router'
import i18n from './locales'
import elementPlus from './plugins/element-plus'
import { setupPermissionDirective } from './directives/permission'
import './assets/styles/global.scss'

const app = createApp(App)

app.use(pinia)
app.use(i18n)
app.use(router)
app.use(elementPlus)
setupPermissionDirective(app)

app.mount('#app')
