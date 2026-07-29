<script setup>
import { computed, ref } from 'vue'
import { reactiveOmit } from '@vueuse/core'
import { DialogRoot, useForwardProps } from 'reka-ui'

const props = defineProps({
  open: { type: Boolean, default: undefined },
  defaultOpen: { type: Boolean, default: false },
  modal: { type: Boolean, default: true }
})

const emit = defineEmits(['update:open'])
const delegatedProps = reactiveOmit(props, 'open', 'defaultOpen')
const forwardedProps = useForwardProps(delegatedProps)
const internalOpen = ref(Boolean(props.defaultOpen))
const resolvedOpen = computed(() => props.open === undefined ? internalOpen.value : Boolean(props.open))

function onOpenChange(value) {
  internalOpen.value = Boolean(value)
  emit('update:open', Boolean(value))
}

</script>

<template>
  <DialogRoot v-bind="forwardedProps" :open="resolvedOpen" @update:open="onOpenChange">
    <slot />
  </DialogRoot>
</template>
