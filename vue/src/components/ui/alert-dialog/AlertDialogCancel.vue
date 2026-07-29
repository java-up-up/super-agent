<script setup>
import { reactiveOmit } from "@vueuse/core";
import { AlertDialogCancel } from "reka-ui";
import { cn } from "@/lib/utils";
import { buttonVariants } from '@/components/ui/button';

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
  variant: { type: null, required: false, default: "outline" },
  size: { type: null, required: false, default: "default" },
});

const delegatedProps = reactiveOmit(props, "class", "variant", "size");
</script>

<template>
  <AlertDialogCancel
    data-slot="alert-dialog-cancel"
    v-bind="delegatedProps"
    :class="cn('', buttonVariants({ variant, size }), props.class)"
  >
    <slot />
  </AlertDialogCancel>
</template>
