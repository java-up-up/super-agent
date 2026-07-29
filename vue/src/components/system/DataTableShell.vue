<script setup>
defineProps({
  busy: { type: Boolean, default: false },
  caption: { type: String, default: '' }
})
</script>

<template>
  <section class="data-table-shell" :aria-busy="busy">
    <div v-if="$slots.toolbar" class="data-table-shell__toolbar">
      <slot name="toolbar" />
    </div>
    <div class="data-table-shell__viewport">
      <table class="data-table-shell__table">
        <caption v-if="caption" class="sr-only">{{ caption }}</caption>
        <slot />
      </table>
    </div>
    <div v-if="$slots.footer" class="data-table-shell__footer">
      <slot name="footer" />
    </div>
  </section>
</template>
