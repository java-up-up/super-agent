<script setup>
import { DialogTrigger } from 'reka-ui'
</script>

<template>
  <DialogTrigger data-slot="dialog-trigger" v-bind="$attrs">
    <slot />
  </DialogTrigger>
</template>
