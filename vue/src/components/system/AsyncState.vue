<script setup>
import { computed } from 'vue'
import { ArrowPathIcon, ExclamationCircleIcon, InboxIcon } from '@heroicons/vue/24/outline'

const props = defineProps({
  state: { type: String, default: 'empty' },
  title: { type: String, default: '' },
  description: { type: String, default: '' }
})

const stateCopy = computed(() => {
  const copy = {
    loading: { title: '正在加载', description: '数据即将显示。' },
    empty: { title: '暂无数据', description: '当前范围还没有可展示的记录。' },
    filtered: { title: '没有匹配项', description: '调整筛选条件后再试。' },
    error: { title: '加载失败', description: '请稍后重试，或检查请求状态。' },
    partial: { title: '部分数据不可用', description: '已保留可用内容，其余内容可以稍后重试。' }
  }
  return copy[props.state] || copy.empty
})

const icon = computed(() => props.state === 'loading'
  ? ArrowPathIcon
  : props.state === 'error' || props.state === 'partial'
    ? ExclamationCircleIcon
    : InboxIcon)
</script>

<template>
  <div class="async-state" :data-state="state" role="status">
    <component :is="icon" class="async-state__icon" :class="{ 'async-state__icon--loading': state === 'loading' }" aria-hidden="true" />
    <div>
      <p class="async-state__title">{{ title || stateCopy.title }}</p>
      <p class="async-state__description">{{ description || stateCopy.description }}</p>
    </div>
    <div v-if="$slots.action" class="async-state__action">
      <slot name="action" />
    </div>
  </div>
</template>
