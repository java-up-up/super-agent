<script setup>
import { cn } from "@/lib/utils";

const props = defineProps({
  class: { type: null, required: false },
});
</script>

<template>
  <div
    data-slot="skeleton"
    :class="cn('bg-muted rounded-md animate-pulse', props.class)"
  />
</template>
