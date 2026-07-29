<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
});
</script>

<template>
  <div
    data-slot="card-content"
    :class="cn('px-4 group-data-[size=sm]/card:px-3', props.class)"
  >
    <slot />
  </div>
</template>
