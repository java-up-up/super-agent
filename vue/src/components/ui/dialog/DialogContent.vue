<script setup>
import { reactiveOmit } from '@vueuse/core'
import { DialogContent, useForwardPropsEmits } from 'reka-ui'
import { XMarkIcon } from '@heroicons/vue/24/outline'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import DialogClose from './DialogClose.vue'
import DialogOverlay from './DialogOverlay.vue'
import DialogPortal from './DialogPortal.vue'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  forceMount: { type: Boolean, default: false },
  trapFocus: { type: Boolean, default: true },
  disableOutsidePointerEvents: { type: Boolean, default: true },
  asChild: { type: Boolean, default: false },
  as: { type: null, default: undefined },
  class: { type: null, default: '' },
  size: { type: String, default: 'default' },
  showClose: { type: Boolean, default: true },
  closeLabel: { type: String, default: '关闭弹窗' }
})

const emit = defineEmits([
  'escapeKeyDown',
  'pointerDownOutside',
  'focusOutside',
  'interactOutside',
  'openAutoFocus',
  'closeAutoFocus'
])

const delegatedProps = reactiveOmit(props, 'class', 'size', 'showClose', 'closeLabel')
const forwarded = useForwardPropsEmits(delegatedProps, emit)

function forwardCloseAutoFocus(event) {
  emit('closeAutoFocus', event)
}
</script>

<template>
  <DialogPortal>
    <DialogOverlay />
    <div data-slot="dialog-positioner" class="pointer-events-none fixed inset-0 z-[var(--z-modal)] flex items-center justify-center overflow-hidden p-4 sm:p-6">
      <DialogContent
        v-bind="{ ...$attrs, ...forwarded }"
        data-slot="dialog-content"
        role="dialog"
        aria-modal="true"
        @close-auto-focus="forwardCloseAutoFocus"
        :data-size="size"
        :class="cn(
          'pointer-events-auto relative flex max-h-[calc(100dvh-3rem)] w-[760px] max-w-[94vw] flex-col overflow-hidden rounded-xl border border-border bg-card text-card-foreground shadow-[var(--shadow-overlay)] outline-none data-open:animate-in data-closed:animate-out data-open:fade-in-0 data-closed:fade-out-0 data-open:zoom-in-95 data-closed:zoom-out-95 duration-200 motion-reduce:animate-none',
          'data-[size=sm]:w-[520px] data-[size=lg]:w-[960px] data-[size=wide]:w-[1120px]',
          props.class
        )"
      >
        <slot />
        <DialogClose v-if="showClose" as-child>
          <Button
            data-dialog-close
            variant="ghost"
            size="icon"
            type="button"
            :aria-label="closeLabel"
            class="absolute right-3 top-3 z-10"
          >
            <XMarkIcon />
          </Button>
        </DialogClose>
      </DialogContent>
    </div>
  </DialogPortal>
</template>
