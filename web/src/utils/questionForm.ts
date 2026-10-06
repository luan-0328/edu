export const questionTypes: Record<string, string> = { SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', TRUE_FALSE: '判断题', SHORT_ANSWER: '简答题' }
export const difficulties: Record<string, string> = { EASY: '简单', MEDIUM: '中等', HARD: '困难' }
export type QuestionForm = {
  id?: number; courseId?: number; type: string; difficulty: string; content: string
  options: { id: string; text: string }[]; correctOptions: string[]; trueFalse: boolean; referenceAnswer: string
}
export function emptyQuestion(): QuestionForm {
  return { courseId: undefined, type: 'SINGLE_CHOICE', difficulty: 'EASY', content: '', options: [{ id: 'A', text: '' }, { id: 'B', text: '' }], correctOptions: [], trueFalse: true, referenceAnswer: '' }
}
export function decodeQuestion(row: Record<string, any>): QuestionForm {
  const form = { ...emptyQuestion(), id: row.id, courseId: row.courseId, type: row.type, difficulty: row.difficulty, content: row.content }
  const answer = JSON.parse(row.answerJson)
  if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(row.type)) {
    form.options = JSON.parse(row.optionsJson || '[]').map((o: any) => typeof o === 'string' ? { id: o, text: o } : { id: String(o.id), text: String(o.text) })
    form.correctOptions = Array.isArray(answer) ? answer : [answer]
  } else if (row.type === 'TRUE_FALSE') form.trueFalse = answer
  else form.referenceAnswer = typeof answer === 'string' ? answer : JSON.stringify(answer)
  return form
}
export function questionPayload(form: QuestionForm) {
  if (!form.courseId) throw new Error('请选择课程')
  if (!form.content.trim()) throw new Error('请填写题目内容')
  let optionsJson: string | null = null
  let answer: unknown
  if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(form.type)) {
    if (form.options.length < 2 || form.options.some(o => !o.text.trim())) throw new Error('请至少填写两个完整的选项')
    if (new Set(form.options.map(o => o.id)).size !== form.options.length) throw new Error('选项标识不能重复')
    const valid = form.correctOptions.filter(id => form.options.some(o => o.id === id))
    if (!valid.length || (form.type === 'SINGLE_CHOICE' && valid.length !== 1)) throw new Error('请选择正确答案，单选题只能选择一个')
    answer = form.type === 'SINGLE_CHOICE' ? valid[0] : valid
    optionsJson = JSON.stringify(form.options.map(o => ({ id: o.id, text: o.text.trim() })))
  } else if (form.type === 'TRUE_FALSE') {
    optionsJson = JSON.stringify([{ id: true, text: '正确' }, { id: false, text: '错误' }])
    answer = form.trueFalse
  } else {
    if (!form.referenceAnswer.trim()) throw new Error('请填写简答题参考答案')
    answer = form.referenceAnswer.trim()
  }
  return { courseId: form.courseId, type: form.type, difficulty: form.difficulty, content: form.content.trim(), optionsJson, answerJson: JSON.stringify(answer) }
}
