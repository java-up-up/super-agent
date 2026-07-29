<script setup>
import { computed } from 'vue'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import { resolveStatusBadgeClass } from './toneClasses'

const props = defineProps({
  label: { type: String, default: '' },
  tone: { type: String, default: 'default' },
  class: { type: String, default: '' }
})

const toneClass = computed(() => resolveStatusBadgeClass(props.tone))
</script>

<template>
  <Badge
    data-slot="status-badge"
    :data-tone="tone"
    variant="outline"
    :class="cn('status-badge', toneClass, props.class)"
  >
    <span class="status-badge__dot" aria-hidden="true"></span>
    <span>{{ label || '未设置' }}</span>
  </Badge>
</template>
