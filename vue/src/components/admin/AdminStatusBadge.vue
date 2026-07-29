<template>
  <StatusBadge :label="label" :tone="tone" />
</template>

<script setup>
import { computed } from 'vue'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { resolveStatusTone } from '@/components/system/status'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'default'
  },
  code: {
    type: [String, Number],
    default: ''
  }
})

const tone = computed(() => resolveStatusTone(props.type, props.code))
</script>
