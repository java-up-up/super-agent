import { reactive } from 'vue'

const state = reactive({
  open: false,
  title: '',
  message: '',
  resolve: null
})

export function useConfirm() {
  function confirm(message, title = '确认操作') {
    state.title = title
    state.message = message
    state.open = true
    return new Promise((resolve) => {
      state.resolve = resolve
    })
  }

  function onConfirm() {
    state.open = false
    state.resolve?.(true)
  }

  function onCancel() {
    state.open = false
    state.resolve?.(false)
  }

  return { state, confirm, onConfirm, onCancel }
}
