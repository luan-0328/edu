import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { pinia } from './stores/pinia'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('./views/LoginView.vue'), meta: { guest: true } },
    { path: '/register', name: 'register', component: () => import('./views/RegisterView.vue'), meta: { guest: true } },
    { path: '/', name: 'home', component: () => import('./views/DashboardView.vue'), meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) return { name: 'login' }
  if (to.meta.guest && auth.isLoggedIn) return { name: 'home' }
})

window.addEventListener('educore:unauthorized', () => {
  useAuthStore(pinia).logout()
  router.push({ name: 'login' })
})
export default router
