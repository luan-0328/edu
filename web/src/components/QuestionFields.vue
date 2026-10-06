<script setup lang="ts">
import { watch } from 'vue'
import { difficulties, questionTypes, type QuestionForm } from '../utils/questionForm'
const props = defineProps<{ form: QuestionForm; courseIds: number[]; courseNames?: Record<number, string> }>()
watch(() => props.form.type, () => { props.form.correctOptions = [] }, { flush: 'sync' })
function addOption() {
  const id = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('').find(id => !props.form.options.some(o => o.id === id))
  if (id) props.form.options.push({ id, text: '' })
}
function removeOption(index: number) {
  const removed = props.form.options.splice(index, 1)[0]
  props.form.correctOptions = props.form.correctOptions.filter(id => id !== removed?.id)
}
</script>
<template>
  <el-form-item label="课程"><el-select v-model="form.courseId" placeholder="请选择课程"><el-option v-for="id in courseIds" :key="id" :label="courseNames?.[id] || `课程 #${id}`" :value="id" /></el-select></el-form-item>
  <el-form-item label="题型"><el-select v-model="form.type"><el-option v-for="(label,value) in questionTypes" :key="value" :label="label" :value="value" /></el-select></el-form-item>
  <el-form-item label="难度"><el-select v-model="form.difficulty"><el-option v-for="(label,value) in difficulties" :key="value" :label="label" :value="value" /></el-select></el-form-item>
  <el-form-item label="题目内容" class="span-2"><el-input v-model="form.content" type="textarea" :rows="3" maxlength="10000" show-word-limit placeholder="请输入题干" /></el-form-item>
  <template v-if="['SINGLE_CHOICE','MULTIPLE_CHOICE'].includes(form.type)">
    <el-form-item label="选项" class="span-2"><div class="question-options"><div v-for="(option,index) in form.options" :key="option.id" class="option-row"><span>{{ option.id }}</span><el-input v-model="option.text" placeholder="请输入选项内容" maxlength="500"/><el-button :disabled="form.options.length<=2" @click="removeOption(index)">删除</el-button></div><el-button :disabled="form.options.length>=26" @click="addOption">添加选项</el-button></div></el-form-item>
    <el-form-item label="正确答案" class="span-2"><el-select v-model="form.correctOptions" multiple :multiple-limit="form.type==='SINGLE_CHOICE'?1:0" placeholder="请选择正确选项"><el-option v-for="o in form.options" :key="o.id" :label="`${o.id} · ${o.text || '未填写'}`" :value="o.id" /></el-select></el-form-item>
  </template>
  <el-form-item v-else-if="form.type==='TRUE_FALSE'" label="正确答案"><el-radio-group v-model="form.trueFalse"><el-radio :value="true">正确</el-radio><el-radio :value="false">错误</el-radio></el-radio-group></el-form-item>
  <el-form-item v-else label="参考答案" class="span-2"><el-input v-model="form.referenceAnswer" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="供教师批改时参考，学生考试时不可见" /></el-form-item>
</template>
<style scoped>
.question-options { width:100%; display:grid; gap:12px }
.option-row { display:flex; align-items:center; gap:12px }
</style>
