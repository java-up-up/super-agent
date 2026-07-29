<script setup>
import { reactiveOmit } from "@vueuse/core";
import { AccordionContent } from "reka-ui";
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
  <AccordionContent
    data-slot="accordion-content"
    v-bind="delegatedProps"
    class="data-open:animate-accordion-down data-closed:animate-accordion-up text-sm overflow-hidden"
  >
    <div
      :class="
        cn(
          'pt-0 pb-2.5 [&_a]:underline [&_a]:underline-offset-3 [&_a]:hover:text-foreground [&_p:not(:last-child)]:mb-4',
          props.class,
        )
      "
    >
      <slot />
    </div>
  </AccordionContent>
</template>
