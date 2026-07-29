<script setup>
import { reactiveOmit } from "@vueuse/core";
import { DrawerTitle } from "vaul-vue";
import { cn } from "@/lib/utils";

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
});

const delegatedProps = reactiveOmit(props, "class");
</script>

<template>
  <DrawerTitle
    data-slot="drawer-title"
    v-bind="delegatedProps"
    :class="
      cn('text-base font-semibold text-foreground', props.class)
    "
  >
    <slot />
  </DrawerTitle>
</template>
