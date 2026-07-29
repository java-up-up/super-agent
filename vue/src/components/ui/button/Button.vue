<script setup>
import { computed } from 'vue'
import { Primitive } from 'reka-ui'
import { cn } from '@/lib/utils'
import { buttonVariants } from '.'
import { Spinner } from '@/components/ui/spinner'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  variant: { type: null, required: false },
  size: { type: null, required: false },
  class: { type: null, required: false },
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false, default: 'button' },
  disabled: { type: Boolean, required: false },
  loading: { type: Boolean, required: false, default: false },
  loadingText: { type: String, required: false, default: '' }
})

const isDisabled = computed(() => Boolean(props.disabled || props.loading))
</script>

<template>
  <Primitive
    v-bind="$attrs"
    data-slot="button"
    :data-variant="variant"
    :data-size="size"
    :data-loading="loading || undefined"
    :as="as"
    :as-child="asChild"
    :disabled="isDisabled || undefined"
    :aria-busy="loading ? 'true' : undefined"
    :class="cn(buttonVariants({ variant, size }), props.class)"
  >
    <Spinner v-if="loading" data-icon="inline-start" />
    <span v-if="loading && loadingText">{{ loadingText }}</span>
    <slot v-else />
  </Primitive>
</template>
