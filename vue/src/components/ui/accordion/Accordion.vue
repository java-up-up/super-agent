<script setup>
import { reactiveOmit } from "@vueuse/core";
import { AccordionRoot, useForwardPropsEmits } from "reka-ui";
import { cn } from "@/lib/utils";

const props = defineProps({
  collapsible: { type: Boolean, required: false },
  disabled: { type: Boolean, required: false },
  dir: { type: String, required: false },
  orientation: { type: String, required: false },
  unmountOnHide: { type: Boolean, required: false },
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  type: { type: String, required: false },
  modelValue: { type: null, required: false },
  defaultValue: { type: null, required: false },
  class: { type: null, required: false },
});
const emits = defineEmits(["update:modelValue"]);

const delegatedProps = reactiveOmit(props, "class");
const forwarded = useForwardPropsEmits(delegatedProps, emits);
</script>

<template>
  <AccordionRoot
    v-slot="slotProps"
    data-slot="accordion"
    v-bind="forwarded"
    :class="cn('flex w-full flex-col', props.class)"
  >
    <slot v-bind="slotProps" />
  </AccordionRoot>
</template>
