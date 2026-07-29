<script setup>
import { reactiveOmit } from '@vueuse/core'
import { DialogDescription, useForwardProps } from 'reka-ui'
import { cn } from '@/lib/utils'

const props = defineProps({
  asChild: { type: Boolean, default: false },
  as: { type: null, default: undefined },
  class: { type: null, default: '' }
})

const forwardedProps = useForwardProps(reactiveOmit(props, 'class'))
</script>

<template>
  <DialogDescription data-slot="dialog-description" v-bind="forwardedProps" :class="cn('max-w-[68ch] text-sm leading-5 text-muted-foreground', props.class)">
    <slot />
  </DialogDescription>
</template>
