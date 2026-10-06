/** API timestamps without an offset are UTC; calendar dates are not timestamps. */
export function parseUtc(value: unknown): number {
  if (!value) return Number.NaN
  const text = String(value)
  return new Date(/[zZ]|[+-]\d{2}:?\d{2}$/.test(text) ? text : `${text.replace(' ', 'T')}Z`).getTime()
}

export function formatTime(value: unknown): string {
  if (!value) return '—'
  const text = String(value)
  if (/^\d{4}-\d{2}-\d{2}$/.test(text)) return text
  const timestamp = parseUtc(text)
  if (!Number.isFinite(timestamp)) return '—'
  const date = new Date(timestamp)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export const timeZoneLabel = Intl.DateTimeFormat().resolvedOptions().timeZone
