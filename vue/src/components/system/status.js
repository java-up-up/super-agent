export const STATUS_TONES = Object.freeze({
  DEFAULT: 'default',
  WAITING: 'waiting',
  RUNNING: 'running',
  SUCCESS: 'success',
  DANGER: 'danger'
})

export function resolveStatusTone(type = 'default', rawCode = '') {
  const code = String(rawCode ?? '')

  // vector 走和 parse/index 相同的码表：DocumentVectorStatusEnum 也是
  // 1 待处理 / 2 进行中 / 3 成功 / 4 失败，没有额外分支，所以并到同一条。
  if (type === 'parse' || type === 'index' || type === 'vector') {
    if (code === '3') return STATUS_TONES.SUCCESS
    if (code === '2') return STATUS_TONES.RUNNING
    if (code === '4') return STATUS_TONES.DANGER
    return STATUS_TONES.WAITING
  }

  if (type === 'strategy') {
    if (code === '3') return STATUS_TONES.SUCCESS
    if (code === '2') return STATUS_TONES.RUNNING
    return STATUS_TONES.WAITING
  }

  if (type === 'task') {
    if (code === '3') return STATUS_TONES.SUCCESS
    if (code === '1' || code === '2') return STATUS_TONES.RUNNING
    if (code === '4') return STATUS_TONES.DANGER
    return STATUS_TONES.DEFAULT
  }

  return STATUS_TONES.DEFAULT
}
