<script setup>
defineProps({
  label: { type: String, default: '' }
})
</script>

<template>
  <section class="code-surface" :aria-label="label || undefined">
    <div v-if="label" class="code-surface__label">{{ label }}</div>
    <pre class="code-surface__content"><code><slot /></code></pre>
  </section>
</template>
