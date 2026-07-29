<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({ class: { type: null, default: '' } })
</script>

<template>
  <div data-slot="dialog-header" :class="cn('flex flex-none flex-col gap-1.5 border-b border-border px-5 py-4 pr-14 text-left', props.class)">
    <slot />
  </div>
</template>
