<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
});
</script>

<template>
  <div
    data-slot="card-title"
    :class="
      cn(
        'text-base leading-snug font-medium group-data-[size=sm]/card:text-sm cn-font-heading',
        props.class,
      )
    "
  >
    <slot />
  </div>
</template>
