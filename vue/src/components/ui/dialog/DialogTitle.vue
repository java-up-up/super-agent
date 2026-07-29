<script setup>
import { reactiveOmit } from '@vueuse/core'
import { DialogTitle, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps({
  asChild: { type: Boolean, default: false },
  as: { type: null, default: undefined },
  class: { type: null, default: '' }
})

const forwardedProps = useForwardProps(reactiveOmit(props, 'class'))
</script>

<template>
  <DialogTitle data-slot="dialog-title" v-bind="forwardedProps" :class="cn('text-base font-semibold leading-6 text-foreground', props.class)">
    <slot />
  </DialogTitle>
</template>
