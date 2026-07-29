<script setup>
import { ArrowPathIcon } from '@heroicons/vue/24/outline'
import { cn } from '@/lib/utils'

const props = defineProps({
  class: { type: null, default: '' }
})
</script>

<template>
  <ArrowPathIcon
    data-slot="spinner"
    aria-hidden="true"
    :class="cn('size-4 animate-spin motion-reduce:animate-none', props.class)"
  />
</template>
