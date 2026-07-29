<script setup>
import { cn } from '@/lib/utils'

const props = defineProps({ class: { type: null, default: '' } })
</script>

<template>
  <div data-slot="dialog-footer" :class="cn('flex flex-none flex-wrap items-center justify-end gap-2 border-t border-border px-5 py-3', props.class)">
    <slot />
  </div>
</template>
