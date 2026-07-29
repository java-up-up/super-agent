<script setup>
import { reactiveOmit } from '@vueuse/core'
import { DialogOverlay, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps({
  forceMount: { type: Boolean, default: false },
  asChild: { type: Boolean, default: false },
  as: { type: null, default: undefined },
  class: { type: null, default: '' }
})

const forwardedProps = useForwardProps(reactiveOmit(props, 'class'))
</script>

<template>
  <DialogOverlay
    data-slot="dialog-overlay"
    v-bind="forwardedProps"
    :class="cn('fixed inset-0 z-[var(--z-backdrop)] bg-overlay backdrop-blur-[2px] data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 duration-200 motion-reduce:animate-none', props.class)"
  />
</template>
