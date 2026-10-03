<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
const auth=useAuthStore(),router=useRouter(),busy=ref(false)
const form=reactive({username:'',realName:'',password:'',confirm:''})
async function submit(){
  if(form.password!==form.confirm){ElMessage.warning('两次输入的密码不一致');return}
  busy.value=true
  try{await auth.register({username:form.username,realName:form.realName,password:form.password});ElMessage.success('注册成功，请登录');await router.push('/login')}
  finally{busy.value=false}
}
</script>
<template>
  <main class="auth-screen register-screen">
    <section class="auth-intro"><div class="brand-mark">E</div><div class="eyebrow">JOIN EDUCORE</div><h1>从报名开始，<br /><span>开启你的学习旅程。</span></h1><p>注册后即可浏览课程、报名班级并跟踪学习进度。</p><div class="auth-orbit orbit-one"></div><div class="auth-orbit orbit-two"></div></section>
    <section class="auth-panel"><el-card class="auth-card" shadow="never"><div class="eyebrow muted">STUDENT ACCOUNT</div><h2>创建学生账号</h2><p class="subtle">用户名使用 3–64 位字母、数字或 ._-</p>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="姓名"><el-input v-model="form.realName" size="large" placeholder="你的姓名" /></el-form-item>
        <el-form-item label="用户名"><el-input v-model="form.username" size="large" autocomplete="username" placeholder="用于登录" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" size="large" type="password" show-password autocomplete="new-password" placeholder="至少 8 位" /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="form.confirm" size="large" type="password" show-password autocomplete="new-password" /></el-form-item>
        <el-button class="wide-button" type="primary" size="large" :loading="busy" @click="submit">注册并继续</el-button>
      </el-form><div class="auth-foot">已有账号？ <router-link to="/login">返回登录</router-link></div>
    </el-card></section>
  </main>
</template>
