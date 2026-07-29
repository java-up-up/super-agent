<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({ class: { type: null, default: '' } })
</script>

<template>
  <div data-slot="dialog-body" :class="cn('min-h-0 flex-1 overflow-y-auto px-5 py-4', props.class)">
    <slot />
  </div>
</template>
