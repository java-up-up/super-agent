<script setup>
import { reactiveOmit } from "@vueuse/core";
import { SelectGroup } from "reka-ui";
import { cn } from "@/lib/utils";

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
});

const delegatedProps = reactiveOmit(props, "class");
</script>

<template>
  <SelectGroup
    data-slot="select-group"
    v-bind="delegatedProps"
    :class="cn('scroll-my-1 p-1', props.class)"
  >
    <slot />
  </SelectGroup>
</template>
