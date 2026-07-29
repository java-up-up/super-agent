<template>
  <section class="flex flex-col gap-6">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between gap-3 max-[760px]:flex-col max-[760px]:items-stretch">
      <RouterLink
        :to="listReturnTarget"
        class="inline-flex items-center gap-1.5 rounded-md border border-border bg-card px-3 py-2 text-sm font-semibold text-foreground hover:border-border-strong hover:bg-secondary"
      >
        <ArrowLeftIcon class="h-4 w-4" />
        返回会话列表
      </RouterLink>

      <div class="flex flex-wrap items-center gap-2">
        <span v-if="activeSession?.running || pollingSession" class="inline-flex items-center gap-1.5 rounded bg-running/10 px-2.5 py-1.5 text-xs font-semibold text-running">
          <span class="h-1.5 w-1.5 rounded-full bg-current"></span>
          {{ pollingSession ? '实时轮询中' : '会话运行中' }}
        </span>
        <span v-if="activeSession && !hasMemorySummary" class="inline-flex items-center px-1 text-xs text-muted-foreground">尚未形成长期摘要</span>
        <ChildPageDialog
          v-if="hasMemorySummary"
          v-model:open="summaryDialogOpen"
          title="长期摘要快照"
          :description="memorySummaryDescription"
          size="lg"
          close-label="关闭长期摘要"
        >
          <template #trigger>
            <Button type="button" variant="outline" size="lg" class="rounded-md">
              <DocumentTextIcon class="h-4 w-4" />
              查看长期摘要
            </Button>
          </template>

          <div class="mx-auto w-full max-w-prose space-y-4 break-words text-body leading-relaxed text-foreground">
            <template v-for="(block, blockIndex) in memorySummaryBlocks" :key="`${block.type}-${blockIndex}`">
              <h3 v-if="block.type === 'heading'" class="m-0 pt-1 text-body font-semibold leading-snug text-foreground first:pt-0">
                {{ block.text }}
              </h3>
              <ul v-else-if="block.type === 'list'" class="m-0 list-disc space-y-2 pl-5 marker:text-muted-foreground">
                <li v-for="(item, itemIndex) in block.items" :key="itemIndex" class="pl-1">{{ item }}</li>
              </ul>
              <p v-else class="m-0 whitespace-pre-line">{{ block.text }}</p>
            </template>
          </div>

          <template #footer>
            <Button type="button" variant="outline" class="rounded-md" @click="summaryDialogOpen = false">关闭</Button>
          </template>
        </ChildPageDialog>
        <Button size="lg" class="rounded-md" :disabled="!activeSession || rebuildingSummary" @click="rebuildSummary">
          <SparklesIcon class="h-4 w-4" />
          {{ rebuildingSummary ? '正在重建摘要...' : '重建长期摘要' }}
        </Button>
      </div>
    </div>

    <div v-if="pageError" class="rounded-md border border-destructive/10 bg-destructive/[0.06] px-3.5 py-3 text-sm text-destructive">{{ pageError }}</div>
    <div v-if="loadingSession && !activeSession" class="rounded-md border border-dashed border-border px-6 py-10 text-center text-sm text-muted-foreground">正在加载会话详情...</div>
    <div v-else-if="!activeSession" class="rounded-md border border-dashed border-border px-6 py-10 text-center text-sm text-muted-foreground">没有找到这条会话，请返回列表重新选择。</div>

    <template v-else>
      <!-- 页头 -->
      <header class="border-b border-border pb-5">
        <h2 class="my-1.5 text-xl font-semibold leading-snug text-foreground">{{ activeSession.selectedDocumentName || sessionTitle(activeSession) }}</h2>
      </header>

      <!-- 轮次时间线 -->
      <section class="flex flex-col gap-3">
        <h3 class="my-1 text-base font-semibold text-foreground">此会话的每次问答</h3>

        <div v-if="!assistantExchanges.length" class="rounded-md border border-dashed border-border px-6 py-6 text-center text-sm text-muted-foreground">
          当前会话还没有助手轮次，无法展示执行链路。
        </div>

        <div v-else class="flex flex-col gap-3">
          <article
            v-for="(exchange, index) in pagedAssistantExchanges"
            :key="exchange.exchangeId"
            class="flex gap-3 sm:gap-4"
          >
            <div class="flex w-5 shrink-0 flex-col items-center pt-5" aria-hidden="true">
              <span class="z-[1] h-2.5 w-2.5 shrink-0 rounded-full"
                :class="{
                  'bg-running': statusTone(exchange.status) === 'running',
                  'bg-[var(--status-success-fg)]': statusTone(exchange.status) === 'completed',
                  'bg-[var(--status-danger-fg)]': statusTone(exchange.status) === 'failed',
                  'bg-[var(--status-waiting-fg)]': statusTone(exchange.status) === 'stopped',
                  'bg-border-strong': !['running','completed','failed','stopped'].includes(statusTone(exchange.status))
                }"
              ></span>
              <span v-if="index < pagedAssistantExchanges.length - 1" class="w-0.5 flex-1 bg-border"></span>
            </div>

            <div
              data-exchange-row
              class="glass-card glass-edge flex min-w-0 flex-1 flex-col gap-4 rounded-glass border px-3 py-4 transition-colors hover:bg-muted/60 sm:px-4"
            >
              <div class="flex items-center justify-between gap-3 max-[760px]:flex-col max-[760px]:items-start">
                <div class="flex flex-wrap items-center gap-1.5">
                  <h4 class="m-0 font-mono text-compact font-semibold text-foreground">第 {{ exchangeStartIndex + index + 1 }} 轮</h4>
                  <span class="inline-flex rounded px-2 py-0.5 text-micro font-semibold"
                    :class="{
                      'bg-[var(--status-success-fg)]/10 text-[var(--status-success-fg)]': statusTone(exchange.status) === 'completed',
                      'bg-destructive/10 text-destructive': statusTone(exchange.status) === 'failed',
                      'bg-[var(--status-waiting-fg)]/10 text-[var(--status-waiting-fg)]': statusTone(exchange.status) === 'stopped',
                      'bg-running/10 text-running': statusTone(exchange.status) === 'running',
                      'bg-foreground/[0.06] text-foreground': !['completed','failed','stopped','running'].includes(statusTone(exchange.status))
                    }"
                  >{{ formatStatusLabel(exchange.status) }}</span>
                  <span v-if="exchange.debugTrace?.executionMode" class="text-caption text-muted-foreground">{{ formatExecutionMode(exchange.debugTrace.executionMode) }}</span>
                </div>
                <time class="whitespace-nowrap text-caption tabular-nums text-muted-foreground">{{ formatDateTime(exchange.editTime || exchange.createTime) }}</time>
              </div>

              <dl data-exchange-dialogue class="m-0 min-w-0 max-w-prose">
                <div class="grid min-w-0 grid-cols-[1.75rem_minmax(0,1fr)] gap-x-3 border-b border-border pb-3">
                  <dt class="pt-0.5 text-caption font-semibold text-foreground">问</dt>
                  <dd class="m-0 min-w-0 break-words text-body-sm font-medium leading-relaxed text-foreground">{{ exchange.question || '未记录问题' }}</dd>
                </div>
                <div class="grid min-w-0 grid-cols-[1.75rem_minmax(0,1fr)] gap-x-3 pt-3">
                  <dt class="pt-0.5 text-caption font-semibold text-muted-foreground">答</dt>
                  <dd class="m-0 min-w-0 whitespace-pre-line break-words text-body-sm leading-relaxed text-muted-foreground line-clamp-4">{{ exchange.answer || '还没有回答内容' }}</dd>
                </div>
              </dl>

              <div class="grid gap-3 border-t border-border pt-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-end">
                <dl data-exchange-metrics class="m-0 grid grid-cols-2 gap-x-4 gap-y-3 sm:grid-cols-3 lg:grid-cols-5">
                  <div v-for="metric in exchangeMetrics(exchange)" :key="metric.label" class="min-w-0">
                    <dt class="text-caption text-muted-foreground">{{ metric.label }}</dt>
                    <dd class="m-0 mt-1 truncate whitespace-nowrap text-body-sm font-semibold tabular-nums text-foreground">{{ metric.value }}</dd>
                  </div>
                </dl>

                <div class="flex justify-end max-[640px]:block">
                  <Button as-child variant="outline" size="sm" class="rounded-md max-[640px]:h-11 max-[640px]:w-full">
                    <RouterLink :to="exchangeTarget(exchange)" :aria-label="`查看第 ${exchangeStartIndex + index + 1} 轮详情`">
                      查看轮次详情
                      <ArrowRightIcon data-icon="inline-end" aria-hidden="true" />
                    </RouterLink>
                  </Button>
                </div>
              </div>
            </div>
          </article>
        </div>
        <nav v-if="exchangePageCount > 1" class="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-4" aria-label="会话轮次分页">
          <span class="text-xs tabular-nums text-muted-foreground">第 {{ exchangePage }} / {{ exchangePageCount }} 页 · 共 {{ assistantExchanges.length }} 轮</span>
          <div class="flex gap-2">
            <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="exchangePage <= 1" @click="exchangePage -= 1">上一页</Button>
            <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="exchangePage >= exchangePageCount" @click="exchangePage += 1">下一页</Button>
          </div>
        </nav>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeftIcon, ArrowRightIcon, DocumentTextIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import { chatApi } from '../../api/api'
import {
  formatDateTime,
  formatExecutionMode,
  formatStatusLabel,
  listAssistantExchanges,
  normalizeError,
  sessionTitle,
  statusTone
} from './observabilityHelpers'
import { Button } from '@/components/ui/button'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'

const route = useRoute()
const loadingSession = ref(false)
const pollingSession = ref(false)
const activeSession = ref(null)
const pageError = ref('')
const rebuildingSummary = ref(false)
const summaryDialogOpen = ref(false)
const EXCHANGE_PAGE_SIZE = 20
const exchangePage = ref(Math.max(1, Number(route.query.turnPage || 1) || 1))

const POLL_INTERVAL_MS = 2500
let pollTimer = 0
let sessionRequestInFlight = false

const conversationId = computed(() => String(route.params.conversationId || ''))
const assistantExchanges = computed(() => listAssistantExchanges(activeSession.value))
const exchangePageCount = computed(() => Math.max(1, Math.ceil(assistantExchanges.value.length / EXCHANGE_PAGE_SIZE)))
const exchangeStartIndex = computed(() => (exchangePage.value - 1) * EXCHANGE_PAGE_SIZE)
const pagedAssistantExchanges = computed(() => assistantExchanges.value.slice(exchangeStartIndex.value, exchangeStartIndex.value + EXCHANGE_PAGE_SIZE))
const listReturnTarget = computed(() => ({
  name: 'AdminObservabilityList',
  query: observationListQuery(route.query)
}))

const hasMemorySummary = computed(() => {
  const m = activeSession.value?.memorySummary
  return Boolean(m?.compressionApplied && String(m.summaryText || '').trim())
})

const memorySummaryDescription = computed(() => {
  const m = activeSession.value?.memorySummary
  if (!m) return ''
  const details = []
  if (m.coveredExchangeCount != null) details.push(`覆盖 ${m.coveredExchangeCount} 轮`)
  if (m.summaryVersion != null) details.push(`第 ${m.summaryVersion} 版`)
  if (m.compressionCount != null) details.push(`已压缩 ${m.compressionCount} 次`)
  return details.join(' · ') || '当前会话形成的长期摘要内容'
})

const memorySummaryBlocks = computed(() => parseMemorySummary(activeSession.value?.memorySummary?.summaryText))

async function loadSession(options = {}) {
  if (!conversationId.value || sessionRequestInFlight) return
  const silent = Boolean(options.silent)
  sessionRequestInFlight = true
  if (silent) { pollingSession.value = true } else { loadingSession.value = true }
  pageError.value = ''
  try {
    activeSession.value = await chatApi.getSession(conversationId.value)
    exchangePage.value = Math.min(exchangePage.value, exchangePageCount.value)
  } catch (error) {
    pageError.value = normalizeError(error, '加载会话详情失败')
  } finally {
    sessionRequestInFlight = false
    loadingSession.value = false
    pollingSession.value = false
    schedulePolling()
  }
}

function schedulePolling() {
  clearTimeout(pollTimer)
  if (!activeSession.value?.running) return
  pollTimer = window.setTimeout(() => loadSession({ silent: true }), POLL_INTERVAL_MS)
}

async function rebuildSummary() {
  if (!conversationId.value || rebuildingSummary.value) return
  rebuildingSummary.value = true
  pageError.value = ''
  try {
    const summary = await chatApi.rebuildConversationSummary(conversationId.value)
    if (activeSession.value?.conversationId === conversationId.value) {
      activeSession.value = { ...activeSession.value, memorySummary: summary }
    }
  } catch (error) {
    pageError.value = normalizeError(error, '手动重建长期摘要失败')
  } finally {
    rebuildingSummary.value = false
  }
}

function exchangeTarget(exchange) {
  return {
    name: 'AdminObservabilityExchangeDetail',
    params: { conversationId: conversationId.value, exchangeId: String(exchange.exchangeId) },
    query: { ...observationListQuery(route.query), turnPage: String(exchangePage.value) }
  }
}

function observationListQuery(query = {}) {
  return ['listKeyword', 'listMode', 'listStatus', 'listPage', 'listPageSize'].reduce((result, key) => {
    if (query[key] != null && query[key] !== '') result[key] = String(query[key])
    return result
  }, {})
}

function exchangeTokenCount(exchange) {
  const traces = exchange?.debugTrace?.modelUsageTraces || []
  const total = traces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
  return total > 0 ? total.toLocaleString('zh-CN') : '无'
}

function exchangeCost(exchange) {
  const traces = exchange?.debugTrace?.modelUsageTraces || []
  const total = traces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  return total > 0 ? `¥ ${total.toFixed(4)}` : '无'
}

function exchangeDuration(exchange) {
  const durationMs = Number(exchange?.totalResponseTimeMs || 0)
  if (!Number.isFinite(durationMs) || durationMs <= 0) return '无'
  if (durationMs < 1000) return `${Math.round(durationMs).toLocaleString('zh-CN')} ms`
  return `${(durationMs / 1000).toFixed(1)} s`
}

function exchangeMetrics(exchange) {
  return [
    { label: '耗时', value: exchangeDuration(exchange) },
    { label: '引用', value: Number(exchange?.references?.length || 0).toLocaleString('zh-CN') },
    { label: '推荐', value: Number(exchange?.recommendations?.length || 0).toLocaleString('zh-CN') },
    { label: 'Token', value: exchangeTokenCount(exchange) },
    { label: '成本', value: exchangeCost(exchange) }
  ]
}

function parseMemorySummary(summaryText) {
  const blocks = []
  let paragraphLines = []
  let listItems = []

  const flushParagraph = () => {
    if (!paragraphLines.length) return
    blocks.push({ type: 'paragraph', text: paragraphLines.join(' ') })
    paragraphLines = []
  }

  const flushList = () => {
    if (!listItems.length) return
    blocks.push({ type: 'list', items: listItems })
    listItems = []
  }

  String(summaryText || '').replace(/\r\n?/g, '\n').split('\n').forEach((rawLine) => {
    const line = rawLine.trim()
    if (!line) {
      flushParagraph()
      flushList()
      return
    }

    const heading = line.match(/^【(.+?)】$/)
    if (heading) {
      flushParagraph()
      flushList()
      blocks.push({ type: 'heading', text: heading[1].trim() })
      return
    }

    const listItem = line.match(/^[-*]\s+(.+)$/)
    if (listItem) {
      flushParagraph()
      listItems.push(listItem[1].trim())
      return
    }

    flushList()
    paragraphLines.push(line)
  })

  flushParagraph()
  flushList()
  return blocks
}

watch(conversationId, () => { activeSession.value = null; loadSession() }, { immediate: true })
watch(exchangePageCount, (pageCount) => { exchangePage.value = Math.min(exchangePage.value, pageCount) })
watch(hasMemorySummary, (available) => { if (!available) summaryDialogOpen.value = false })
onMounted(() => schedulePolling())
onUnmounted(() => clearTimeout(pollTimer))
</script>
