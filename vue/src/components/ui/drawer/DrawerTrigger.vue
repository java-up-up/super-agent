<script setup>
import { DrawerTrigger } from "vaul-vue";

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
});
</script>

<template>
  <DrawerTrigger data-slot="drawer-trigger" v-bind="props">
    <slot />
  </DrawerTrigger>
</template>
