<script setup>
import { ChevronDownIcon } from "@heroicons/vue/24/outline";

import { reactiveOmit } from "@vueuse/core";
import { AccordionHeader, AccordionTrigger } from "reka-ui";
import { cn } from "@/lib/utils";

const props = defineProps({
  asChild: { type: Boolean, required: false },
  as: { type: null, required: false },
  class: { type: null, required: false },
});

const delegatedProps = reactiveOmit(props, "class");
</script>

<template>
  <AccordionHeader class="flex">
    <AccordionTrigger
      data-slot="accordion-trigger"
      v-bind="delegatedProps"
      :class="
        cn(
          'focus-visible:ring-ring/50 focus-visible:border-ring focus-visible:after:border-ring rounded-lg py-2.5 text-left text-sm font-medium hover:underline focus-visible:ring-3 group/accordion-trigger relative flex flex-1 items-start justify-between gap-4 border border-transparent transition-all outline-none disabled:pointer-events-none disabled:opacity-50',
          props.class,
        )
      "
    >
      <slot />
      <slot name="icon">
        <ChevronDownIcon
          data-slot="accordion-trigger-icon"
          class="pointer-events-none ml-auto size-4 shrink-0 text-muted-foreground transition-transform duration-200 group-aria-expanded/accordion-trigger:rotate-180"
        />
      </slot>
    </AccordionTrigger>
  </AccordionHeader>
</template>
