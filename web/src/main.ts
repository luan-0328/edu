import { createApp } from 'vue'
import { pinia } from './stores/pinia'
import App from './App.vue'
import router from './router'
import './styles.css'

createApp(App).use(pinia).use(router).mount('#app')
