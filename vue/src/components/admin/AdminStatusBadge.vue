<template>
  <span class="status-badge" :class="badgeClass">
    <span class="status-dot"></span>
    {{ label || '未设置' }}
  </span>
</template>

<script setup>
import { computed } from 'vue'
import { normalizeCode } from '../../utils/manageFormat'

const props = defineProps({
  label: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'default'
  },
  code: {
    type: [String, Number],
    default: ''
  }
})

const badgeClass = computed(() => {
  if (props.type === 'parse') {
    return mapParseClass(normalizeCode(props.code))
  }
  if (props.type === 'strategy') {
    return mapStrategyClass(normalizeCode(props.code))
  }
  if (props.type === 'index') {
    return mapIndexClass(normalizeCode(props.code))
  }
  if (props.type === 'task') {
    return mapTaskClass(normalizeCode(props.code))
  }
  return 'status-default'
})

function mapParseClass(code) {
  if (code === '3') return 'status-success'
  if (code === '2') return 'status-processing'
  if (code === '4') return 'status-danger'
  return 'status-waiting'
}

function mapStrategyClass(code) {
  if (code === '3') return 'status-success'
  if (code === '2') return 'status-processing'
  return 'status-waiting'
}

function mapIndexClass(code) {
  if (code === '3') return 'status-success'
  if (code === '2') return 'status-processing'
  if (code === '4') return 'status-danger'
  return 'status-waiting'
}

function mapTaskClass(code) {
  if (code === '3') return 'status-success'
  if (code === '2' || code === '1') return 'status-processing'
  if (code === '4') return 'status-danger'
  return 'status-default'
}
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
  white-space: nowrap;
}

.status-dot {
  width: 6px;
  height: 6px;
  flex: none;
  border-radius: 50%;
  background: currentColor;
}

.status-default {
  background: rgba(92, 108, 131, 0.1);
  color: #516072;
  border-color: rgba(92, 108, 131, 0.2);
}

.status-waiting {
  background: rgba(168, 101, 32, 0.1);
  color: #9b5d1c;
  border-color: rgba(168, 101, 32, 0.2);
}

.status-processing {
  background: rgba(178, 31, 196, 0.1);
  color: #8c168d;
  border-color: rgba(178, 31, 196, 0.2);
}

.status-success {
  background: rgba(18, 125, 74, 0.1);
  color: #127d4a;
  border-color: rgba(18, 125, 74, 0.2);
}

.status-danger {
  background: rgba(177, 47, 38, 0.1);
  color: #b12f26;
  border-color: rgba(177, 47, 38, 0.2);
}
</style>
