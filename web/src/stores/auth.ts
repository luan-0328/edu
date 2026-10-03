import { defineStore } from 'pinia'
import { api } from '../api'

export interface UserInfo { id: number; username: string; realName: string; role: 'ADMIN' | 'TEACHER' | 'STUDENT'; status: string }

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('educore.token') || '',
    user: JSON.parse(localStorage.getItem('educore.user') || 'null') as UserInfo | null,
  }),
  getters: { isLoggedIn: (state) => Boolean(state.token && state.user) },
  actions: {
    async login(credentials: { username: string; password: string }) {
      const result = await api.post<{ accessToken: string; user: UserInfo }>('/auth/login', credentials)
      this.token = result.accessToken
      this.user = result.user
      localStorage.setItem('educore.token', this.token)
      localStorage.setItem('educore.user', JSON.stringify(this.user))
    },
    async register(data: { username: string; password: string; realName: string }) {
      await api.post('/auth/register', data)
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('educore.token')
      localStorage.removeItem('educore.user')
    },
  },
})
