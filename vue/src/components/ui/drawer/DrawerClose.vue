<script setup>
import { DrawerClose } from "vaul-vue";

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
});
</script>

<template>
  <DrawerClose data-slot="drawer-close" v-bind="props">
    <slot />
  </DrawerClose>
</template>
