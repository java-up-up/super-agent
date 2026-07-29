<script setup>
defineProps({
  label: { type: String, default: '筛选条件' }
})
</script>

<template>
  <section class="filter-toolbar" :aria-label="label">
    <div class="filter-toolbar__fields">
      <slot />
    </div>
    <div v-if="$slots.actions" class="filter-toolbar__actions">
      <slot name="actions" />
    </div>
  </section>
</template>
