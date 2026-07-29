<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger
} from '@/components/ui/dialog'

const props = defineProps({
  open: { type: Boolean, required: true },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  size: { type: String, default: 'default' },
  closeLabel: { type: String, default: '关闭弹窗' },
  showClose: { type: Boolean, default: true }
})

const emit = defineEmits(['update:open', 'close'])
const triggerRef = ref(null)
let returnFocusElement = null
let restoreFocusTimer = null

function triggerElement() {
  const element = triggerRef.value?.$el || triggerRef.value
  return element instanceof HTMLElement ? element : null
}

function rememberExternalFocus(event) {
  if (
    !props.open
    && event.target instanceof HTMLElement
    && !event.target.closest('[data-slot="dialog-content"]')
  ) {
    returnFocusElement = event.target
  }
}

onMounted(() => document.addEventListener('focusin', rememberExternalFocus, true))

watch(() => props.open, (isOpen, wasOpen) => {
  if (isOpen && !wasOpen) {
    returnFocusElement = document.activeElement instanceof HTMLElement
      ? document.activeElement
      : null
  }

  if (!isOpen && wasOpen && returnFocusElement) {
    const target = returnFocusElement
    // Reka's Presence may keep the modal mounted for the 200ms exit motion.
    // Restore once immediately and once after that terminal state.
    target.focus({ preventScroll: true })
    restoreFocusTimer = window.setTimeout(() => target.focus({ preventScroll: true }), 260)
  }
}, { flush: 'sync' })

onBeforeUnmount(() => {
  document.removeEventListener('focusin', rememberExternalFocus, true)
  returnFocusElement = null
  if (restoreFocusTimer) window.clearTimeout(restoreFocusTimer)
})

function onOpenChange(value) {
  if (!value) returnFocusElement = triggerElement() || returnFocusElement
  emit('update:open', value)
  if (!value) emit('close')
}

function onCloseAutoFocus(event) {
  if (!returnFocusElement) return
  event.preventDefault()
  const target = returnFocusElement
  nextTick(() => target?.focus?.({ preventScroll: true }))
}
</script>

<template>
  <Dialog :open="open" @update:open="onOpenChange">
    <DialogTrigger v-if="$slots.trigger" ref="triggerRef" as-child>
      <slot name="trigger" />
    </DialogTrigger>
    <DialogContent :size="size" :close-label="closeLabel" :show-close="showClose" @close-auto-focus="onCloseAutoFocus">
      <DialogHeader>
        <DialogTitle>{{ title }}</DialogTitle>
        <DialogDescription v-if="description">{{ description }}</DialogDescription>
      </DialogHeader>
      <DialogBody>
        <slot />
      </DialogBody>
      <DialogFooter v-if="$slots.footer">
        <slot name="footer" />
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
