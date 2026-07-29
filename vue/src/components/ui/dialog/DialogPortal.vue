<script setup>
import { DialogPortal } from 'reka-ui'

defineProps({
  to: { type: [String, Object], default: 'body' },
  disabled: { type: Boolean, default: false },
  forceMount: { type: Boolean, default: false }
})
</script>

<template>
  <DialogPortal data-slot="dialog-portal" v-bind="$props">
    <slot />
  </DialogPortal>
</template>
