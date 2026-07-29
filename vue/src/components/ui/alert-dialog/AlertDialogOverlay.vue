<script setup>
import { AlertDialogOverlay } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps({
  class: { type: null, default: '' },
  asChild: { type: Boolean, required: false },
  forceMount: { type: Boolean, required: false }
})
</script>

<template>
  <AlertDialogOverlay
    v-bind="props"
    :class="cn(
      'fixed inset-0 z-[var(--z-backdrop)] bg-overlay backdrop-blur-[2px] data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 motion-reduce:animate-none',
      props.class
    )"
  >
    <slot />
  </AlertDialogOverlay>
</template>
