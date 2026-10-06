export function usernameValidationMessage(value: string): string {
  if (!value.trim()) return '请输入用户名'
  if (value.length < 3 || value.length > 64) return '用户名长度需为 3 到 64 位'
  if (!/^[A-Za-z0-9_.-]+$/.test(value)) {
    return '用户名只能包含英文字母、数字、点（.）、下划线（_）或短横线（-）'
  }
  return ''
}

export function passwordValidationMessage(value: string): string {
  if (!value) return '请输入密码'
  if (value.length < 8 || value.length > 72) return '密码长度需为 8 到 72 位'
  return ''
}

export function realNameValidationMessage(value: string): string {
  if (!value.trim()) return '请输入姓名'
  if (value.length > 64) return '姓名不能超过 64 个字符'
  return ''
}
