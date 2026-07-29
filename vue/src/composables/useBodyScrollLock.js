import { watch, onBeforeUnmount } from 'vue'

// 模块级引用计数：多个弹窗可能同时打开，只有全部关闭后才恢复 body 滚动。
let lockCount = 0
let savedOverflow = ''

function acquire() {
  if (lockCount === 0) {
    savedOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  }
  lockCount += 1
}

function release() {
  if (lockCount === 0) return
  lockCount -= 1
  if (lockCount === 0) {
    document.body.style.overflow = savedOverflow
  }
}

/**
 * 弹窗/抽屉打开时锁住 body 滚动，防止滚动穿透到父页面。
 * @param {import('vue').Ref<boolean> | (() => boolean)} isOpen 弹窗开关（ref 或 getter）
 */
export function useBodyScrollLock(isOpen) {
  let locked = false

  const setLocked = (next) => {
    if (next && !locked) {
      acquire()
      locked = true
    } else if (!next && locked) {
      release()
      locked = false
    }
  }

  watch(isOpen, (value) => setLocked(Boolean(value)), { immediate: true })

  // 组件卸载时若仍锁着，兜底释放，避免计数泄漏
  onBeforeUnmount(() => setLocked(false))
}
