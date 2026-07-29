<template>
  <ChildPageDialog
    :open="open"
    title="任务执行详情"
    :description="`任务 ${documentDetail?.latestTaskId || '-'} · ${documentDetail?.latestTaskTypeName || '暂无任务类型'}`"
    size="sm"
    close-label="关闭任务执行详情"
    @update:open="$emit('update:open', $event)"
  >
    <div class="mb-4 flex flex-wrap gap-3 border-b border-border pb-4">
      <div class="flex items-center gap-2 text-xs text-muted-foreground">
        <span>当前状态</span>
        <AdminStatusBadge :label="documentDetail?.latestTaskStatusName || '暂无状态'" :code="documentDetail?.latestTaskStatus" type="task" />
      </div>
      <div class="flex items-center gap-2 text-xs text-muted-foreground">
        <span>索引状态</span>
        <AdminStatusBadge :label="documentDetail?.indexStatusName || '暂无状态'" :code="documentDetail?.indexStatus" type="index" />
      </div>
    </div>
    <div v-if="loading" class="py-8 text-center text-sm text-muted-foreground">正在加载任务日志...</div>
    <div v-else-if="!logs.length" class="py-8 text-center text-sm text-muted-foreground">当前任务还没有日志记录。</div>
    <div v-else class="grid gap-0">
      <article v-for="log in logs" :key="log.id" class="flex gap-3 border-b border-border pb-4 pt-3 last:border-0">
        <div class="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-primary/60"></div>
        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between gap-2 text-xs">
            <strong class="text-foreground">{{ log.stageTypeName }} · {{ log.eventTypeName }}</strong>
            <span class="shrink-0 text-muted-foreground">{{ formatDateTime(log.createTime) }}</span>
          </div>
          <p class="mt-1 text-compact text-muted-foreground">{{ log.content }}</p>
          <pre v-if="log.detailJson" class="mt-2 overflow-x-auto whitespace-pre-wrap break-all rounded-md bg-code p-2.5 text-xs text-code-foreground">{{ prettyJson(log.detailJson) }}</pre>
        </div>
      </article>
    </div>
  </ChildPageDialog>
</template>

<script setup>
import { formatDateTime } from '../../utils/manageFormat'
import AdminStatusBadge from './AdminStatusBadge.vue'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'

defineProps({
  open: { type: Boolean, default: false },
  documentDetail: { type: Object, default: null },
  logs: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:open'])

function prettyJson(str) {
  try { return JSON.stringify(JSON.parse(str), null, 2) }
  catch { return str }
}
</script>

