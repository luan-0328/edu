<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { passwordValidationMessage, realNameValidationMessage, usernameValidationMessage } from '../utils/formValidation'

const auth = useAuthStore()
const router = useRouter()
const busy = ref(false)
const registerFormRef = ref<FormInstance>()
const form = reactive({ username: '', realName: '', password: '', confirm: '' })

const rules: FormRules = {
  username: [{
    validator: (_rule, value, callback) => {
      const message = usernameValidationMessage(String(value ?? ''))
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  realName: [{
    validator: (_rule, value, callback) => {
      const message = realNameValidationMessage(String(value ?? ''))
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  password: [{
    validator: (_rule, value, callback) => {
      const message = passwordValidationMessage(String(value ?? ''))
      callback(message ? new Error(message) : undefined)
    },
    trigger: 'blur',
  }],
  confirm: [{
    validator: (_rule, value, callback) => {
      if (!value) callback(new Error('请再次输入密码'))
      else if (value !== form.password) callback(new Error('两次输入的密码不一致'))
      else callback()
    },
    trigger: 'blur',
  }],
}

async function submit() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  busy.value = true
  try {
    await auth.register({ username: form.username, realName: form.realName, password: form.password })
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <main class="auth-screen register-screen">
    <section class="auth-intro">
      <div class="brand-mark">E</div>
      <div class="eyebrow">JOIN EDUCORE</div>
      <h1>从报名开始，<br /><span>开启你的学习旅程。</span></h1>
      <p>注册后即可浏览课程、报名班级并跟踪学习进度。</p>
      <div class="auth-orbit orbit-one"></div>
      <div class="auth-orbit orbit-two"></div>
    </section>

    <section class="auth-panel">
      <el-card class="auth-card" shadow="never">
        <div class="eyebrow muted">STUDENT ACCOUNT</div>
        <h2>创建学生账号</h2>
        <p class="subtle">用户名需为 3–64 位，只能使用英文字母、数字、点（.）、下划线（_）或短横线（-）。密码需为 8–72 位。</p>
        <el-form ref="registerFormRef" :model="form" :rules="rules" label-position="top" @submit.prevent="submit">
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" size="large" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" size="large" autocomplete="username" placeholder="用于登录" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password autocomplete="new-password" placeholder="请输入 8–72 位密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirm">
            <el-input v-model="form.confirm" size="large" type="password" show-password autocomplete="new-password" placeholder="请再次输入密码" />
          </el-form-item>
          <el-button class="wide-button" type="primary" size="large" :loading="busy" @click="submit">注册并继续</el-button>
        </el-form>
        <div class="auth-foot">已有账号？ <router-link to="/login">返回登录</router-link></div>
      </el-card>
    </section>
  </main>
</template>
