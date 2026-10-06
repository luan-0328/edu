import assert from 'node:assert/strict'
import { test } from 'node:test'
import { formatTime, parseUtc } from '../src/utils/dateTime.ts'
import { emptyQuestion, questionPayload, decodeQuestion } from '../src/utils/questionForm.ts'

test('UTC timestamps render in Shanghai time, preserving calendar dates', () => {
  process.env.TZ = 'Asia/Shanghai'
  assert.equal(formatTime('2026-10-05T04:07:50.528'), '2026-10-05 12:07:50')
  assert.equal(formatTime('2026-10-05T04:07:50Z'), '2026-10-05 12:07:50')
  assert.equal(formatTime('2026-10-05T12:07:50+08:00'), '2026-10-05 12:07:50')
  assert.equal(formatTime('2026-10-05'), '2026-10-05')
  assert.equal(parseUtc('2026-10-05T04:00:00'), Date.parse('2026-10-05T04:00:00Z'))
  assert.equal(formatTime(null), '—')
})
test('question forms serialize and restore all four question types', () => {
  const form=emptyQuestion()
  form.courseId=1;form.content='题目';form.options=[{id:'A',text:'甲'},{id:'B',text:'乙'}];form.correctOptions=['A']
  assert.equal(questionPayload(form).answerJson,'"A"')
  assert.deepEqual(decodeQuestion({...questionPayload(form),id:1}).correctOptions,['A'])
  form.type='MULTIPLE_CHOICE';form.correctOptions=['A','B']
  assert.deepEqual(JSON.parse(questionPayload(form).answerJson),['A','B'])
  form.type='TRUE_FALSE';form.trueFalse=false
  assert.equal(questionPayload(form).answerJson,'false')
  form.type='SHORT_ANSWER';form.referenceAnswer='参考答案'
  assert.equal(questionPayload(form).optionsJson,null)
  assert.equal(decodeQuestion({...questionPayload(form),id:1}).referenceAnswer,'参考答案')
})
test('incomplete questions show understandable validation errors', () => {
  const form=emptyQuestion()
  assert.throws(()=>questionPayload(form),/请选择课程/)
  form.courseId=1;form.content='题目'
  assert.throws(()=>questionPayload(form),/完整的选项/)
  form.options=[{id:'A',text:'甲'},{id:'B',text:'乙'}]
  assert.throws(()=>questionPayload(form),/请选择正确答案/)
  form.correctOptions=['A','B']
  assert.throws(()=>questionPayload(form),/单选题只能选择一个/)
})
