<template>
  <section class="observability-session">
    <div class="detail-toolbar">
      <RouterLink :to="{ name: 'AdminObservabilityList' }" class="back-link">
        <ArrowLeftIcon class="tool-icon" />
        返回会话列表
      </RouterLink>

      <div class="toolbar-actions">
        <span v-if="activeSession?.running || pollingSession" class="live-chip">
          <span class="live-dot"></span>
          {{ pollingSession ? '实时轮询中' : '会话运行中' }}
        </span>
        <button class="ghost-button" type="button" :disabled="loadingSession" @click="loadSession()">
          <ArrowPathIcon class="tool-icon" />
          {{ loadingSession ? '刷新中...' : '刷新会话详情' }}
        </button>
        <button
          class="primary-button"
          type="button"
          :disabled="!activeSession || rebuildingSummary"
          @click="rebuildSummary"
        >
          <SparklesIcon class="tool-icon" />
          {{ rebuildingSummary ? '正在重建摘要...' : '重建长期摘要' }}
        </button>
      </div>
    </div>

    <div v-if="pageError" class="inline-notice error-notice">{{ pageError }}</div>
    <div v-if="loadingSession && !activeSession" class="empty-card">正在加载会话详情...</div>
    <div v-else-if="!activeSession" class="empty-card">没有找到这条会话，请返回列表重新选择。</div>

    <template v-else>
      <header class="page-header">
        <div class="header-copy">
          <span class="header-kicker">Conversation Chain</span>
          <h2>{{ activeSession.selectedDocumentName || sessionTitle(activeSession) }}</h2>
          <p>
            这个页面只负责看整条会话里的每次问答，不展示单轮内部细节。
            先从下方轮次列表里找到你关心的那一轮，再进入专门的轮次详情页。
          </p>
        </div>

        <div class="stat-badges">
          <span class="stat-badge mode-badge">{{ formatChatMode(activeSession.chatMode) }}</span>
          <span v-if="activeSession.running" class="stat-badge running-badge">当前会话仍在执行</span>
          <span v-else-if="activeSession.latestTurnStatus" class="stat-badge" :class="`badge-${statusTone(activeSession.latestTurnStatus)}`">
            最近一轮{{ formatStatusLabel(activeSession.latestTurnStatus) }}
          </span>
          <span class="stat-badge neutral-badge">会话ID {{ activeSession.conversationId }}</span>
          <span v-for="item in sessionMetrics" :key="item.label" class="stat-badge">
            <span class="stat-label">{{ item.label }}</span>
            <strong class="stat-value">{{ item.value }}</strong>
          </span>
        </div>
      </header>

      <section class="context-section">
        <h3 class="section-title">
          <span class="section-kicker">Session Context</span>
          会话级背景
        </h3>
        <p class="section-desc">只解释整条会话的上下文、最近状态和记忆压缩，不进入某一轮内部链路。</p>

        <dl class="context-list">
          <div class="context-item">
            <dt>最近用户问题</dt>
            <dd>{{ activeSession.latestUserMessage || '无' }}</dd>
          </div>
          <div class="context-item">
            <dt>最近助手回答</dt>
            <dd>{{ sessionPreview(activeSession) }}</dd>
          </div>
          <div class="context-item">
            <dt>Checkpoint / 消息数</dt>
            <dd>{{ activeSession.checkpointCount || 0 }} / {{ activeSession.messageCount || 0 }}</dd>
          </div>
        </dl>

        <div v-if="activeSession.memorySummary?.compressionApplied" class="memory-block">
          <h4 class="memory-title">
            <span class="section-kicker">Memory</span>
            长期摘要快照
          </h4>
          <div class="memory-chips">
            <span class="memory-chip">covered {{ activeSession.memorySummary?.coveredExchangeCount ?? 0 }}</span>
            <span class="memory-chip">version {{ activeSession.memorySummary?.summaryVersion ?? 0 }}</span>
            <span class="memory-chip">compress {{ activeSession.memorySummary?.compressionCount ?? 0 }}</span>
          </div>
          <pre class="code-block">{{ activeSession.memorySummary?.summaryText || '无' }}</pre>
        </div>

        <div v-else class="memory-empty">
          当前会话还没有形成长期摘要。常见原因是轮次还不够，或者摘要预热尚未完成。
        </div>
      </section>

      <section class="rounds-section">
        <h3 class="section-title">
          <span class="section-kicker">Round List</span>
          本会话的每次一来一回
        </h3>
        <p class="section-desc">这里是整条会话的轮次总览，点击某一轮后会跳转到独立的轮次详情页。</p>

        <div v-if="!assistantExchanges.length" class="empty-card compact-empty">
          当前会话还没有助手轮次，无法展示执行链路。
        </div>

        <div v-else class="rounds-list">
          <article
            v-for="(exchange, index) in assistantExchanges"
            :key="exchange.exchangeId"
            class="round-item"
            :class="`status-${statusTone(exchange.status)}`"
          >
            <div class="round-indicator">
              <span class="round-dot"></span>
              <span v-if="index < assistantExchanges.length - 1" class="round-line"></span>
            </div>

            <RouterLink class="round-content" :to="exchangeTarget(exchange)">
              <div class="round-header">
                <div class="round-badges">
                  <span class="round-seq">第 {{ index + 1 }} 轮</span>
                  <span class="round-badge" :class="`badge-${statusTone(exchange.status)}`">
                    {{ formatStatusLabel(exchange.status) }}
                  </span>
                  <span v-if="exchange.debugTrace?.executionMode" class="round-badge mode-badge">
                    {{ formatExecutionMode(exchange.debugTrace.executionMode) }}
                  </span>
                </div>
                <span class="round-time">{{ formatDateTime(exchange.editTime || exchange.createTime) }}</span>
              </div>

              <div class="round-qa">
                <p class="qa-question"><strong>问：</strong>{{ exchange.question || '未记录问题' }}</p>
                <p class="qa-answer"><strong>答：</strong>{{ truncate(exchange.answer || '还没有回答内容', 200) }}</p>
              </div>

              <div class="round-meta">
                <span>耗时 {{ exchange.totalResponseTimeMs ? `${exchange.totalResponseTimeMs} ms` : '无' }}</span>
                <span>引用 {{ exchange.references?.length || 0 }}</span>
                <span>推荐 {{ exchange.recommendations?.length || 0 }}</span>
                <span>Token {{ exchangeTokenCount(exchange) }}</span>
                <span>成本 {{ exchangeCost(exchange) }}</span>
              </div>

              <span class="round-link">进入这一轮的详情页 →</span>
            </RouterLink>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeftIcon, ArrowPathIcon, SparklesIcon } from '@heroicons/vue/24/outline'
import { chatApi } from '../../api/api'
import {
  formatChatMode,
  formatDateTime,
  formatExecutionMode,
  formatStatusLabel,
  listAssistantExchanges,
  normalizeError,
  sessionPreview,
  sessionTitle,
  statusTone,
  truncate
} from './observabilityHelpers'

const route = useRoute()

const loadingSession = ref(false)
const pollingSession = ref(false)
const activeSession = ref(null)
const pageError = ref('')
const rebuildingSummary = ref(false)

const POLL_INTERVAL_MS = 2500
let pollTimer = 0
let sessionRequestInFlight = false

const conversationId = computed(() => String(route.params.conversationId || ''))
const assistantExchanges = computed(() => listAssistantExchanges(activeSession.value))

const sessionMetrics = computed(() => {
  if (!activeSession.value) {
    return []
  }
  return [
    {
      label: '助手轮次',
      value: assistantExchanges.value.length
    },
    {
      label: '会话消息数',
      value: activeSession.value.messageCount || 0
    },
    {
      label: '长期摘要',
      value: activeSession.value.memorySummary?.compressionApplied ? '已形成' : '未形成'
    },
    {
      label: '最近更新时间',
      value: formatDateTime(activeSession.value.updatedAt)
    }
  ]
})

async function loadSession(options = {}) {
  if (!conversationId.value || sessionRequestInFlight) {
    return
  }

  const silent = Boolean(options.silent)
  sessionRequestInFlight = true
  if (silent) {
    pollingSession.value = true
  } else {
    loadingSession.value = true
  }
  pageError.value = ''

  try {
    activeSession.value = await chatApi.getSession(conversationId.value)
  } catch (error) {
    activeSession.value = null
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
  if (!activeSession.value?.running) {
    return
  }
  pollTimer = window.setTimeout(() => {
    loadSession({ silent: true })
  }, POLL_INTERVAL_MS)
}

async function rebuildSummary() {
  if (!conversationId.value || rebuildingSummary.value) {
    return
  }

  rebuildingSummary.value = true
  pageError.value = ''

  try {
    const summary = await chatApi.rebuildConversationSummary(conversationId.value)
    if (activeSession.value?.conversationId === conversationId.value) {
      activeSession.value = {
        ...activeSession.value,
        memorySummary: summary
      }
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
    params: {
      conversationId: conversationId.value,
      exchangeId: String(exchange.exchangeId)
    }
  }
}

function exchangeTokenCount(exchange) {
  const traces = exchange?.debugTrace?.modelUsageTraces || []
  const total = traces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
  return total || '无'
}

function exchangeCost(exchange) {
  const traces = exchange?.debugTrace?.modelUsageTraces || []
  const total = traces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  return total > 0 ? `¥ ${total.toFixed(4)}` : '无'
}

watch(conversationId, () => {
  activeSession.value = null
  loadSession()
}, { immediate: true })

onMounted(() => {
  schedulePolling()
})

onUnmounted(() => {
  clearTimeout(pollTimer)
})
</script>

<style scoped>
.observability-session {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── Toolbar ── */
.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tool-icon { width: 16px; height: 16px; }

.back-link,
.ghost-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-weight: 600;
  background: #fff;
  color: var(--color-text);
  cursor: pointer;
}

.back-link:hover,
.ghost-button:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
}

.primary-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: var(--radius-sm);
  padding: 8px 14px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  cursor: pointer;
}

.primary-button:hover:not(:disabled) { opacity: 0.88; }
.primary-button:disabled,
.ghost-button:disabled { opacity: 0.55; cursor: default; }

.live-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  border-radius: 4px;
  background: rgba(13, 124, 124, 0.1);
  color: #0d7c7c;
  font-size: 12px;
  font-weight: 600;
}

.live-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

/* ── Page Header ── */
.page-header {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

.header-kicker,
.section-kicker {
  display: block;
  color: var(--color-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-family: 'Fira Code', var(--font-sans);
  margin-bottom: 4px;
}

.header-copy h2 {
  margin: 6px 0 8px;
  font-size: 20px;
  line-height: 1.3;
  color: var(--color-text-strong);
}

.header-copy p,
.section-desc {
  margin: 0;
  color: var(--color-muted-strong);
  line-height: 1.7;
  font-size: 13px;
}

.stat-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.mode-badge { background: rgba(23, 48, 79, 0.07); color: #17304f; }
.running-badge { background: rgba(13, 124, 124, 0.1); color: #0d7c7c; }
.neutral-badge { background: rgba(23, 48, 79, 0.06); color: var(--color-muted-strong); }
.badge-completed { background: rgba(18, 125, 74, 0.1); color: var(--color-success); }
.badge-failed { background: rgba(177, 47, 38, 0.1); color: var(--color-danger); }
.badge-stopped { background: rgba(168, 101, 32, 0.1); color: var(--color-warning); }
.badge-running { background: rgba(13, 124, 124, 0.1); color: #0d7c7c; }

.stat-label { color: var(--color-muted); font-weight: 400; }
.stat-value { color: var(--color-text-strong); font-family: 'Fira Code', var(--font-sans); }

/* ── Context Section ── */
.context-section {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

.section-title {
  margin: 0 0 4px;
  font-size: 16px;
  color: var(--color-text-strong);
}

.context-list {
  margin: 16px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.context-item {
  display: flex;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.context-item:last-child { border-bottom: none; }

.context-item dt {
  flex-shrink: 0;
  width: 140px;
  color: var(--color-muted);
  font-size: 13px;
}

.context-item dd {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
  word-break: break-word;
}

.memory-block {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.memory-title {
  margin: 0 0 10px;
  font-size: 15px;
  color: var(--color-text-strong);
}

.memory-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.memory-chip {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(23, 48, 79, 0.06);
  color: var(--color-muted-strong);
  font-size: 11px;
  font-weight: 600;
  font-family: 'Fira Code', var(--font-sans);
}

.code-block {
  margin: 0;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-surface-soft);
  color: var(--color-text);
  white-space: pre-wrap;
  line-height: 1.65;
  font-size: 13px;
  font-family: 'Fira Code', var(--font-sans);
}

.memory-empty {
  margin-top: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  background: var(--color-surface-soft);
  color: var(--color-muted-strong);
  font-size: 13px;
}

/* ── Rounds Section (Timeline) ── */
.rounds-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rounds-list {
  display: flex;
  flex-direction: column;
  margin-top: 4px;
}

.round-item {
  display: flex;
  gap: 16px;
  position: relative;
}

.round-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 20px;
  padding-top: 18px;
}

.round-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--color-border-strong);
  flex-shrink: 0;
  z-index: 1;
}

.status-running .round-dot { background: #0d7c7c; }
.status-completed .round-dot { background: var(--color-success); }
.status-failed .round-dot { background: var(--color-danger); }
.status-stopped .round-dot { background: var(--color-warning); }

.round-line {
  width: 2px;
  flex: 1;
  background: var(--color-border);
}

.round-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 0 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  transition: background 0.15s ease;
}

.round-item:last-child .round-content {
  border-bottom: none;
}

.round-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.round-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.round-seq {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-strong);
  font-family: 'Fira Code', var(--font-sans);
}

.round-badge {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.round-time {
  font-size: 12px;
  color: var(--color-muted);
  white-space: nowrap;
}

.round-qa {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.qa-question,
.qa-answer {
  margin: 0;
  line-height: 1.65;
  font-size: 13px;
}

.qa-question { color: var(--color-text); }
.qa-answer { color: var(--color-muted-strong); }

.round-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted);
}

.round-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-strong);
}

.round-content:hover .round-link {
  text-decoration: underline;
}

/* ── Empty & Error ── */
.empty-card {
  padding: 40px 20px;
  text-align: center;
  color: var(--color-muted);
  line-height: 1.8;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.compact-empty { padding: 24px 16px; }

.inline-notice {
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  line-height: 1.6;
}

.error-notice {
  color: var(--color-danger);
  background: rgba(177, 47, 38, 0.06);
  border: 1px solid rgba(177, 47, 38, 0.1);
}

/* ── Responsive ── */
@media (max-width: 760px) {
  .detail-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }

  .context-item {
    flex-direction: column;
    gap: 4px;
  }

  .context-item dt {
    width: auto;
  }

  .round-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
