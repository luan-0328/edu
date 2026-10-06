import { defineStore } from 'pinia'
import { api } from '../api'

export interface UserInfo { id: number; username: string; realName: string; role: 'ADMIN' | 'TEACHER' | 'STUDENT'; status: string }

function readStoredUser():UserInfo|null {
  try {
    const user=JSON.parse(localStorage.getItem('educore.user')||'null')
    return user&&Number.isSafeInteger(user.id)&&['ADMIN','TEACHER','STUDENT'].includes(user.role)?user:null
  } catch { localStorage.removeItem('educore.user');return null }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('educore.token') || '',
    user: readStoredUser(),
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
    async refreshCurrentUser() {
      const user = await api.get<UserInfo>('/users/me')
      this.user = user
      localStorage.setItem('educore.user', JSON.stringify(user))
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
