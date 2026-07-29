<script setup>
import { reactiveOmit } from "@vueuse/core";
import { DrawerOverlay } from "vaul-vue";
import { cn } from "@/lib/utils";

const props = defineProps({
  forceMount: { type: Boolean, required: false },
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
});

const delegatedProps = reactiveOmit(props, "class");
</script>

<template>
  <DrawerOverlay
    data-slot="drawer-overlay"
    v-bind="delegatedProps"
    :class="
      cn(
        'fixed inset-0 z-[var(--z-backdrop)] bg-overlay backdrop-blur-[2px] data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 duration-200 motion-reduce:animate-none',
        props.class,
      )
    "
  />
</template>
