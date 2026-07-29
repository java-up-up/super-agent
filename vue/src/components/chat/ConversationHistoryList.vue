<script setup>
import { computed } from 'vue'
import { ArrowPathIcon, TrashIcon } from '@heroicons/vue/24/outline'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import StatusBadge from '@/components/system/StatusBadge.vue'

const props = defineProps({
  sessions: { type: Array, default: () => [] },
  currentConversationId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['select', 'delete', 'retry'])

const sortedSessions = computed(() => [...props.sessions].sort((left, right) => {
  const leftTime = left?.updatedAt ? new Date(left.updatedAt).getTime() : 0
  const rightTime = right?.updatedAt ? new Date(right.updatedAt).getTime() : 0
  return rightTime - leftTime
}))

function latestExchangeValue(session, key) {
  const exchanges = Array.isArray(session?.exchanges) ? session.exchanges : []
  for (let index = exchanges.length - 1; index >= 0; index -= 1) {
    if (exchanges[index]?.[key]) return exchanges[index][key]
  }
  return ''
}

function fullTitle(session) {
  return session?.latestUserMessage
    || latestExchangeValue(session, 'question')
    || session?.latestAssistantMessage
    || latestExchangeValue(session, 'answer')
    || '新的对话'
}

function preview(session) {
  return session?.latestAssistantMessage
    || latestExchangeValue(session, 'answer')
    || session?.latestUserMessage
    || latestExchangeValue(session, 'question')
    || '还没有消息内容'
}

function messageCount(session) {
  if (session?.messageCount) return session.messageCount
  return (Array.isArray(session?.exchanges) ? session.exchanges.length : 0) * 2
}

function formatTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}
</script>

<template>
  <div class="min-h-0 flex-1 overflow-y-auto px-2 pb-3">
    <div v-if="loading" class="grid gap-2 px-2 py-3" aria-label="正在加载会话列表">
      <div v-for="index in 5" :key="index" class="grid gap-2 border-b border-border px-2 py-3">
        <Skeleton class="h-4 w-2/3" />
        <Skeleton class="h-3 w-full" />
        <Skeleton class="h-3 w-1/3" />
      </div>
    </div>

    <Alert v-else-if="error" variant="destructive" class="mx-2 mt-3">
      <AlertTitle>会话列表加载失败</AlertTitle>
      <AlertDescription class="mt-1">{{ error }}</AlertDescription>
      <Button variant="outline" size="sm" class="mt-3" type="button" @click="emit('retry')">
        <ArrowPathIcon data-icon="inline-start" />
        重试
      </Button>
    </Alert>

    <div v-else-if="!sortedSessions.length" class="px-4 py-10 text-center">
      <p class="font-medium text-foreground">还没有历史会话</p>
      <p class="mt-2 text-sm leading-relaxed text-muted-foreground">发送第一条消息后，会话会保存在这里。</p>
    </div>

    <ul v-else class="grid" aria-label="历史会话">
      <li
        v-for="session in sortedSessions"
        :key="session.conversationId"
        class="group flex min-w-0 items-start border-b border-l-2 border-b-border border-l-transparent px-2 py-2"
        :class="session.conversationId === currentConversationId ? 'border-l-primary bg-secondary' : 'hover:bg-muted/70'"
      >
        <Button
          variant="ghost"
          class="h-auto min-h-16 min-w-0 flex-1 flex-col items-start justify-center gap-1 rounded-md px-2 py-2 text-left whitespace-normal hover:bg-transparent"
          type="button"
          :disabled="disabled"
          :aria-current="session.conversationId === currentConversationId ? 'page' : undefined"
          :aria-label="`打开会话：${fullTitle(session)}`"
          :title="fullTitle(session)"
          @click="emit('select', session.conversationId)"
        >
          <span class="flex w-full min-w-0 items-center gap-2">
            <strong class="truncate text-sm font-semibold text-foreground">{{ fullTitle(session) }}</strong>
            <StatusBadge v-if="session.running" label="生成中" tone="running" class="ml-auto" />
          </span>
          <span class="line-clamp-2 text-xs leading-relaxed text-muted-foreground">{{ preview(session) }}</span>
          <span class="text-xs text-muted-foreground">{{ formatTime(session.updatedAt) }} · {{ messageCount(session) }} 条消息</span>
        </Button>
        <Button
          variant="ghost"
          size="icon-lg"
          class="mt-1 size-11 shrink-0 text-muted-foreground hover:text-destructive lg:size-9"
          type="button"
          :aria-label="`删除会话：${fullTitle(session)}`"
          :title="`删除会话：${fullTitle(session)}`"
          :disabled="disabled"
          @click="emit('delete', session.conversationId)"
        >
          <TrashIcon />
        </Button>
      </li>
    </ul>
  </div>
</template>
