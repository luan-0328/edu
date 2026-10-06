<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const busy = ref(false)
const form = reactive({ username: '', password: '' })
async function submit() {
  if (busy.value) return
  if (!form.username.trim() || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  busy.value = true
  try { await auth.login(form); ElMessage.success(`欢迎回来，${auth.user?.realName || auth.user?.username}`); await router.push('/') }
  finally { busy.value = false }
}
</script>

<template>
  <main class="auth-screen">
    <section class="auth-intro">
      <div class="brand-mark">E</div>
      <div class="eyebrow">EDUCORE · LEARNING OPERATIONS</div>
      <h1>把教学流程<br /><span>放在一个清晰的地方。</span></h1>
      <p>课程、班级、报名、教学与考试，一套系统完成日常协作。</p>
      <div class="auth-orbit orbit-one"></div><div class="auth-orbit orbit-two"></div>
    </section>
    <section class="auth-panel">
      <el-card class="auth-card" shadow="never">
        <div class="eyebrow muted">WELCOME BACK</div>
        <h2>登录 EduCore</h2>
        <p class="subtle">使用收到的账号和密码登录，体验不同角色的日常工作。</p>
        <div class="login-role-guide"><span><strong>管理员</strong>课程、开班与排课</span><span><strong>教师</strong>考勤、作业与考试</span><span><strong>学生</strong>报名、学习与成绩</span></div>
        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名"><el-input v-model="form.username" size="large" autocomplete="username" placeholder="输入用户名" @keyup.enter="submit" /></el-form-item>
          <el-form-item label="密码"><el-input v-model="form.password" size="large" type="password" show-password autocomplete="current-password" placeholder="输入密码" @keyup.enter="submit" /></el-form-item>
          <el-button class="wide-button" type="primary" size="large" :loading="busy" @click="submit">登录</el-button>
        </el-form>
        <div class="auth-foot">还没有学生账号？ <router-link to="/register">立即注册</router-link></div>
      </el-card>
    </section>
  </main>
</template>
