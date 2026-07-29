<script setup>
import { DialogClose } from 'reka-ui'
</script>

<template>
  <DialogClose data-slot="dialog-close-root" v-bind="$attrs">
    <slot />
  </DialogClose>
</template>
