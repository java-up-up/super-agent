<template>
  <section class="round-detail-page">
    <div class="detail-toolbar">
      <RouterLink
        :to="{ name: 'AdminObservabilitySession', params: { conversationId } }"
        class="back-link"
      >
        <ArrowLeftIcon class="tool-icon" />
        返回会话轮次列表
      </RouterLink>

      <div class="toolbar-actions">
        <button class="ghost-button" type="button" :disabled="loadingPage" @click="loadPage()">
          <ArrowPathIcon class="tool-icon" />
          {{ loadingPage ? '刷新中...' : '刷新这一轮详情' }}
        </button>
      </div>
    </div>

    <div v-if="pageError" class="inline-notice error-notice">{{ pageError }}</div>
    <div v-if="loadingPage && !activeExchangeDetail" class="empty-card">正在加载轮次详情...</div>
    <div v-else-if="!activeExchange" class="empty-card">没有找到这条轮次，请返回会话页重新选择。</div>

    <template v-else>
      <header class="page-header">
        <div class="header-copy">
          <span class="header-kicker">Round Detail</span>
          <h2>{{ activeExchange.question || '未记录问题' }}</h2>
          <p>{{ currentExchangeNarrative }}</p>
        </div>

        <div class="stat-badges">
          <span class="stat-badge" :class="`badge-${statusTone(activeExchange.status)}`">
            {{ formatStatusLabel(activeExchange.status) }}
          </span>
          <span class="stat-badge mode-badge">{{ formatChatMode(activeSession?.chatMode) }}</span>
          <span v-if="activeExchange.debugTrace?.executionMode" class="stat-badge neutral-badge">
            {{ formatExecutionMode(activeExchange.debugTrace.executionMode) }}
          </span>
          <span class="stat-badge neutral-badge">会话 {{ conversationId }}</span>
          <span class="stat-badge neutral-badge">轮次 {{ exchangeId }}</span>
        </div>

        <dl class="header-meta">
          <div class="meta-pair">
            <dt>文档范围</dt>
            <dd>{{ activeSession?.selectedDocumentName || '未绑定文档' }}</dd>
          </div>
          <div class="meta-pair">
            <dt>执行时间</dt>
            <dd>{{ formatDateTime(activeExchange.editTime || activeExchange.createTime) }}</dd>
          </div>
          <div class="meta-pair">
            <dt>总耗时</dt>
            <dd>{{ activeExchange.totalResponseTimeMs ? `${activeExchange.totalResponseTimeMs} ms` : '无' }}</dd>
          </div>
          <div class="meta-pair">
            <dt>引用 / 推荐</dt>
            <dd>{{ activeExchange.references?.length || 0 }} / {{ activeExchange.recommendations?.length || 0 }}</dd>
          </div>
          <div class="meta-pair">
            <dt>总 Token / 成本</dt>
            <dd>{{ totalTokenCount }} / {{ totalCostText }}</dd>
          </div>
        </dl>
      </header>

      <section class="timeline-section">
        <h3 class="section-title">
          <span class="section-kicker">Trace Timeline</span>
          执行阶段时间线
        </h3>
        <p class="section-desc">先浏览整个执行顺序，再点击某个阶段进入子页面查看这个阶段的详细过程。</p>

        <div v-if="!stageTraces.length" class="empty-card compact-empty">
          当前轮次还没有可展示的阶段轨迹。
        </div>

        <div v-else class="timeline-list">
          <article
            v-for="(trace, index) in stageTraces"
            :key="trace.stageId"
            class="timeline-item"
            :class="{ active: String(trace.stageId) === selectedTraceStageId }"
          >
            <div class="timeline-indicator">
              <span class="timeline-dot" :class="`dot-${statusTone(trace.stageState)}`"></span>
              <span v-if="index < stageTraces.length - 1" class="timeline-line"></span>
            </div>

            <button
              type="button"
              class="timeline-content"
              @click="openTraceDetail(trace.stageId)"
            >
              <div class="timeline-header">
                <div class="timeline-title">
                  <strong>{{ trace.stageName }}</strong>
                  <span class="timeline-badge" :class="`badge-${statusTone(trace.stageState)}`">
                    {{ formatStatusLabel(trace.stageState) }}
                  </span>
                </div>
                <span class="timeline-time">{{ formatDateTime(trace.startTime) }}</span>
              </div>

              <p class="timeline-summary">{{ trace.summaryText || '当前阶段已记录。' }}</p>

              <div class="timeline-bar">
                <div class="timeline-bar-fill" :style="{ width: traceBarWidth(trace) }"></div>
              </div>

              <div class="timeline-meta">
                <span>耗时 {{ trace.durationMs ? `${trace.durationMs} ms` : '无' }}</span>
              </div>

              <span class="timeline-link">查看这个阶段 →</span>
            </button>
          </article>
        </div>
      </section>

      <section class="summary-section">
        <h3 class="section-title">
          <span class="section-kicker">Round Summary</span>
          这轮回答的关键结果
        </h3>
        <p class="section-desc">这里是当前轮次的摘要信息，帮助你快速判断这轮是否正常，再决定要点开哪个阶段。</p>

        <div class="summary-list">
          <article v-for="stage in exchangeStages" :key="stage.key" class="summary-item">
            <div class="summary-header">
              <div class="summary-title">
                <span class="summary-kicker">{{ stage.eyebrow || stage.key }}</span>
                <h4>{{ stage.title }}</h4>
                <p>{{ stage.subtitle }}</p>
              </div>
              <div v-if="stage.chips?.length" class="summary-chips">
                <span
                  v-for="item in stage.chips"
                  :key="`${stage.key}-${item.label}-${item.value}`"
                  class="summary-chip"
                  :class="`chip-${item.tone || 'neutral'}`"
                >
                  {{ item.label }}：{{ item.value }}
                </span>
              </div>
            </div>

            <div v-if="stage.metrics?.length" class="summary-metrics">
              <span v-for="item in stage.metrics" :key="`${stage.key}-${item.label}`">
                {{ item.label }}：{{ item.value }}
              </span>
            </div>

            <dl v-if="stage.textBlocks?.length" class="summary-pairs">
              <div v-for="item in stage.textBlocks.slice(0, 2)" :key="`${stage.key}-${item.label}`" class="summary-pair">
                <dt>{{ item.label }}</dt>
                <dd>{{ item.code ? truncate(item.value, 90) : item.value }}</dd>
              </div>
            </dl>

            <div v-if="stage.listBlocks?.length" class="summary-preview">
              <span class="preview-label">{{ stage.listBlocks[0].label }}</span>
              <p>{{ stage.listBlocks[0].items.slice(0, 2).join('；') || '无' }}</p>
            </div>

            <button
              v-if="canOpenStage(stage)"
              class="summary-link"
              type="button"
              @click="openSummaryStage(stage)"
            >
              查看这个阶段的执行过程 →
            </button>
          </article>
        </div>
      </section>

      <section v-if="channelExecutions.length > 0" class="observe-section">
        <h3 class="section-title">
          <span class="section-kicker">Channel Performance</span>
          通道执行对比
        </h3>
        <p class="section-desc">对比各检索通道的性能和效果。</p>

        <div class="channel-comparison-grid">
          <article v-for="exec in channelExecutions" :key="exec.id" class="channel-card">
            <div class="channel-card-header">
              <strong>{{ formatChannelType(exec.channelType) }}</strong>
              <span class="stat-badge" :class="`badge-${exec.executionState === 1 ? 'completed' : 'failed'}`">
                {{ formatExecutionState(exec.executionState) }}
              </span>
            </div>
            <p v-if="exec.subQuestion" class="channel-sub-question">子问题 {{ exec.subQuestionIndex }}：{{ truncate(exec.subQuestion, 60) }}</p>
            <div class="channel-metrics">
              <div class="metric-item"><span class="metric-label">召回数</span><span class="metric-value">{{ exec.recalledCount }}</span></div>
              <div class="metric-item"><span class="metric-label">闸门后</span><span class="metric-value">{{ exec.acceptedCount }}</span></div>
              <div class="metric-item"><span class="metric-label">最终选入</span><span class="metric-value metric-highlight">{{ exec.finalSelectedCount }}</span></div>
              <div class="metric-item"><span class="metric-label">耗时</span><span class="metric-value">{{ exec.durationMs ? `${exec.durationMs} ms` : '-' }}</span></div>
              <div class="metric-item"><span class="metric-label">平均分</span><span class="metric-value">{{ formatScore(exec.avgScore) }}</span></div>
              <div class="metric-item"><span class="metric-label">分数区间</span><span class="metric-value">{{ formatScore(exec.minScore) }} ~ {{ formatScore(exec.maxScore) }}</span></div>
            </div>
            <div v-if="exec.errorMessage" class="channel-error">{{ exec.errorMessage }}</div>
          </article>
        </div>
      </section>

      <section v-if="groupedRetrievalResults.length > 0" class="observe-section">
        <h3 class="section-title">
          <span class="section-kicker">Retrieval Results</span>
          检索结果详情
        </h3>
        <p class="section-desc">查看每个通道检索到的文档块、分数变化和最终选择情况。</p>

        <div v-for="subQ in groupedRetrievalResults" :key="subQ.index" class="sub-question-group">
          <h4 class="sub-question-title">子问题 {{ subQ.index }}：{{ subQ.question }}</h4>

          <div v-for="channel in subQ.channels" :key="channel.type" class="channel-results-block">
            <div class="channel-results-header">
              <strong>{{ formatChannelType(channel.type) }}</strong>
              <span class="result-count-badge">{{ channel.results.length }} 条</span>
            </div>

            <div class="results-table-wrapper">
              <table class="results-table">
                <thead>
                  <tr>
                    <th>排名变化</th>
                    <th>文档块</th>
                    <th>原始分</th>
                    <th>RRF 分</th>
                    <th>Rerank 分</th>
                    <th>状态</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="result in channel.results" :key="result.id" :class="{ 'row-selected': result.isSelected }">
                    <td class="rank-cell">
                      {{ result.channelRank || '-' }}
                      <span v-if="result.finalRank" class="rank-arrow">→ {{ result.finalRank }}</span>
                    </td>
                    <td class="chunk-cell">
                      <div class="chunk-doc-name">{{ result.documentName || '未知文档' }}</div>
                      <div v-if="result.sectionPath" class="chunk-section">{{ result.sectionPath }}</div>
                      <div v-if="result.chunkTextPreview" class="chunk-preview-text">{{ truncate(result.chunkTextPreview, 120) }}</div>
                    </td>
                    <td>{{ formatScore(result.originalScore) }}</td>
                    <td>{{ formatScore(result.rrfScore) }}</td>
                    <td>{{ formatScore(result.rerankScore) }}</td>
                    <td>
                      <span v-if="result.isSelected" class="selection-badge badge-selected">已选入</span>
                      <span v-else-if="!result.gatePassed" class="selection-badge badge-filtered">闸门过滤</span>
                      <span v-else class="selection-badge badge-omitted">未选入</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>

      <section v-if="evidenceBudgetSnapshot" class="observe-section">
        <h3 class="section-title">
          <span class="section-kicker">Evidence Budget</span>
          证据预算分析
        </h3>
        <p class="section-desc">查看证据选择过程和预算使用情况。</p>

        <div class="budget-summary-row">
          <div class="budget-item">
            <span class="budget-label">总预算</span>
            <span class="budget-value">{{ evidenceBudgetSnapshot.totalBudget || 0 }} 字符</span>
          </div>
          <div class="budget-item">
            <span class="budget-label">单子问题预算</span>
            <span class="budget-value">{{ evidenceBudgetSnapshot.perSubQuestionBudget || 0 }} 字符</span>
          </div>
          <div class="budget-item">
            <span class="budget-label">已纳入</span>
            <span class="budget-value metric-highlight">{{ evidenceBudgetSnapshot.renderedReferenceCount || 0 }} 条</span>
          </div>
          <div class="budget-item">
            <span class="budget-label">已省略</span>
            <span class="budget-value">{{ evidenceBudgetSnapshot.omittedReferenceCount || 0 }} 条</span>
          </div>
        </div>

        <div v-if="evidenceBudgetSnapshot.renderedReferenceDetails?.length" class="evidence-group">
          <h4 class="evidence-group-title evidence-included">已纳入 Prompt 的证据</h4>
          <ul class="evidence-list">
            <li v-for="(detail, idx) in evidenceBudgetSnapshot.renderedReferenceDetails" :key="`rendered-${idx}`">{{ detail }}</li>
          </ul>
        </div>

        <div v-if="evidenceBudgetSnapshot.omittedReferenceDetails?.length" class="evidence-group">
          <h4 class="evidence-group-title evidence-omitted">因预算限制省略的证据</h4>
          <ul class="evidence-list">
            <li v-for="(detail, idx) in evidenceBudgetSnapshot.omittedReferenceDetails" :key="`omitted-${idx}`">{{ detail }}</li>
          </ul>
        </div>
      </section>

      <section v-if="hasPromptData" class="observe-section">
        <h3 class="section-title">
          <span class="section-kicker">Prompt Preview</span>
          Prompt 预览
        </h3>
        <p class="section-desc">查看最终喂给模型的完整 Prompt。</p>

        <div class="prompt-tabs">
          <button
            type="button"
            class="prompt-tab"
            :class="{ active: activePromptTab === 'system' }"
            @click="activePromptTab = 'system'"
          >System Prompt</button>
          <button
            type="button"
            class="prompt-tab"
            :class="{ active: activePromptTab === 'user' }"
            @click="activePromptTab = 'user'"
          >User Prompt</button>
        </div>

        <div class="prompt-content">
          <pre v-if="activePromptTab === 'system'">{{ ragSystemPrompt || '无' }}</pre>
          <pre v-else>{{ ragUserPrompt || '无' }}</pre>
        </div>
      </section>

      <section v-if="stageTraces.length > 0 && stageBenchmarks.length > 0" class="observe-section">
        <h3 class="section-title">
          <span class="section-kicker">Performance Benchmark</span>
          阶段性能基准对比
        </h3>
        <p class="section-desc">对比当前执行与历史基准（P50/P90/P99），识别异常慢的阶段。</p>

        <div class="benchmark-grid">
          <article
            v-for="trace in stageTraces.filter(t => t.durationMs)"
            :key="trace.stageId"
            class="benchmark-card"
          >
            <div class="benchmark-header">
              <strong>{{ trace.stageName }}</strong>
              <span
                v-if="findBenchmark(trace.stageCode, trace.executionMode)"
                class="benchmark-level"
                :class="`level-${formatBenchmarkComparison(trace.durationMs, findBenchmark(trace.stageCode, trace.executionMode))?.level}`"
              >
                {{ formatBenchmarkComparison(trace.durationMs, findBenchmark(trace.stageCode, trace.executionMode))?.text || '-' }}
              </span>
            </div>
            <div class="benchmark-metrics">
              <div class="bm-item">
                <span class="bm-label">本次</span>
                <span class="bm-value bm-actual">{{ trace.durationMs }} ms</span>
              </div>
              <template v-if="findBenchmark(trace.stageCode, trace.executionMode)">
                <div class="bm-item">
                  <span class="bm-label">P50</span>
                  <span class="bm-value">{{ findBenchmark(trace.stageCode, trace.executionMode).p50DurationMs || '-' }} ms</span>
                </div>
                <div class="bm-item">
                  <span class="bm-label">P90</span>
                  <span class="bm-value">{{ findBenchmark(trace.stageCode, trace.executionMode).p90DurationMs || '-' }} ms</span>
                </div>
                <div class="bm-item">
                  <span class="bm-label">P99</span>
                  <span class="bm-value">{{ findBenchmark(trace.stageCode, trace.executionMode).p99DurationMs || '-' }} ms</span>
                </div>
                <div class="bm-item">
                  <span class="bm-label">样本数</span>
                  <span class="bm-value">{{ findBenchmark(trace.stageCode, trace.executionMode).sampleCount }}</span>
                </div>
              </template>
              <div v-else class="bm-item">
                <span class="bm-label">基准</span>
                <span class="bm-value">暂无数据</span>
              </div>
            </div>
          </article>
        </div>
      </section>

      <div
        v-if="traceDetailOpen && overlayInspector"
        class="trace-overlay"
        @click="closeTraceDetail"
      >
        <aside class="trace-panel" @click.stop>
          <div class="panel-head">
            <div>
              <span class="section-kicker">Trace Detail</span>
              <h3>{{ overlayInspector.title }}</h3>
              <p class="section-desc">{{ overlayInspector.summary || '这个阶段已经执行完成，下面是它记录下来的结构化细节。' }}</p>
            </div>
            <button class="panel-close" type="button" @click="closeTraceDetail">关闭</button>
          </div>

          <div class="panel-metrics">
            <span>状态：{{ formatStatusLabel(overlayInspector.status) }}</span>
            <span>开始：{{ formatDateTime(overlayInspector.startTime) }}</span>
            <span>结束：{{ formatDateTime(overlayInspector.endTime) }}</span>
            <span>耗时：{{ overlayInspector.durationMs ? `${overlayInspector.durationMs} ms` : '无' }}</span>
          </div>

          <div v-if="overlayInspector.summaryItems?.length" class="detail-grid">
            <div v-for="item in overlayInspector.summaryItems" :key="`trace-item-${item.label}`" class="detail-block">
              <span>{{ item.label }}</span>
              <pre v-if="item.code" class="code-block">{{ item.value }}</pre>
              <strong v-else>{{ item.value }}</strong>
            </div>
          </div>

          <div v-if="overlayInspector.listSections?.length" class="detail-list-stack">
            <section v-for="item in overlayInspector.listSections" :key="`trace-list-${item.label}`" class="detail-list-block">
              <span>{{ item.label }}</span>
              <ol v-if="item.ordered" class="plain-list ordered-list">
                <li v-for="(entry, index) in item.items" :key="`trace-list-${item.label}-${index}`">
                  {{ entry }}
                </li>
              </ol>
              <ul v-else class="plain-list">
                <li v-for="(entry, index) in item.items" :key="`trace-list-${item.label}-${index}`">
                  {{ entry }}
                </li>
              </ul>
            </section>
          </div>

          <div v-if="overlayInspector.tableSections?.length" class="table-section-stack">
            <section v-for="table in overlayInspector.tableSections" :key="`trace-table-${table.label}`" class="table-section">
              <span class="table-label">{{ table.label }}</span>
              <div class="table-wrapper">
                <table class="detail-table">
                  <thead>
                    <tr>
                      <th v-for="column in table.columns" :key="`col-${column}`">{{ column }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, rowIndex) in table.rows" :key="`row-${table.label}-${rowIndex}`">
                      <td v-for="(cell, cellIndex) in row.cells" :key="`cell-${rowIndex}-${cellIndex}`">{{ cell }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>
          </div>

          <details v-if="overlayInspector.advancedItems?.length" class="advanced-panel">
            <summary>查看这个阶段的原始快照</summary>
            <div class="advanced-grid">
              <div v-for="item in overlayInspector.advancedItems" :key="`trace-advanced-${item.label}`" class="advanced-block">
                <span>{{ item.label }}</span>
                <pre v-if="item.code" class="code-block">{{ item.value }}</pre>
                <strong v-else>{{ item.value }}</strong>
              </div>
            </div>
          </details>
        </aside>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, ref, watch, watchEffect } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { ArrowLeftIcon, ArrowPathIcon } from '@heroicons/vue/24/outline'
import { chatApi } from '../../api/api'
import {
  buildExchangeStages,
  buildExchangeStatusNarrative,
  buildTraceStageInspector,
  buildUsageStageInspector,
  formatChatMode,
  formatDateTime,
  formatExecutionMode,
  formatRelationType,
  formatRetrievalMode,
  formatStatusLabel,
  formatChannelType,
  formatExecutionState,
  formatScore,
  groupResultsBySubQuestion,
  normalizeError,
  stageHasAdvancedDetails,
  statusTone,
  truncate
} from './observabilityHelpers'

const route = useRoute()

const loadingPage = ref(false)
const activeSession = ref(null)
const activeExchangeDetail = ref(null)
const pageError = ref('')
const traceDetailOpen = ref(false)
const selectedTraceStageId = ref('')
const overlayInspector = ref(null)
const retrievalResults = ref([])
const channelExecutions = ref([])
const loadingRetrievalData = ref(false)

const conversationId = computed(() => String(route.params.conversationId || ''))
const exchangeId = computed(() => String(route.params.exchangeId || ''))
const activeExchange = computed(() => activeExchangeDetail.value?.exchange || null)
const stageTraces = computed(() => activeExchangeDetail.value?.stageTraces || [])
const activeTraceStage = computed(() => {
  if (!selectedTraceStageId.value) {
    return stageTraces.value[0] || null
  }
  return stageTraces.value.find((item) => String(item.stageId) === selectedTraceStageId.value) || stageTraces.value[0] || null
})
const activeTraceInspector = computed(() => buildTraceStageInspector(activeTraceStage.value, activeExchange.value))
const exchangeStages = computed(() => buildExchangeStages(activeSession.value, activeExchange.value))

const currentExchangeNarrative = computed(() => {
  if (!activeExchange.value) {
    return '这页只负责看这一轮的执行链路。'
  }
  return buildExchangeStatusNarrative(activeExchange.value)
})

const totalTokenCount = computed(() => {
  const traces = activeExchange.value?.debugTrace?.modelUsageTraces || []
  return traces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
})

const totalCostText = computed(() => {
  const traces = activeExchange.value?.debugTrace?.modelUsageTraces || []
  const total = traces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  return total > 0 ? `¥ ${total.toFixed(4)}` : '无'
})

const maxTraceDuration = computed(() => {
  return stageTraces.value.reduce((max, item) => Math.max(max, Number(item?.durationMs || 0)), 0)
})

const groupedRetrievalResults = computed(() => groupResultsBySubQuestion(retrievalResults.value))

const stageBenchmarks = ref([])
const loadingBenchmarks = ref(false)

async function loadStageBenchmarks() {
  loadingBenchmarks.value = true
  try {
    const data = await chatApi.getStageBenchmarks()
    stageBenchmarks.value = data || []
  } catch (error) {
    stageBenchmarks.value = []
  } finally {
    loadingBenchmarks.value = false
  }
}

function findBenchmark(stageCode, executionMode) {
  if (!stageBenchmarks.value || !stageBenchmarks.value.length) {
    return null
  }
  return stageBenchmarks.value.find(
    (b) => b.stageCode === stageCode && b.executionMode === executionMode
  ) || null
}

function formatBenchmarkComparison(actualMs, benchmark) {
  if (!benchmark || !actualMs) {
    return null
  }
  const p50 = benchmark.p50DurationMs || 0
  const p90 = benchmark.p90DurationMs || 0
  const p99 = benchmark.p99DurationMs || 0

  if (actualMs <= p50) {
    return { level: 'excellent', text: '优秀（≤ P50）' }
  } else if (actualMs <= p90) {
    return { level: 'good', text: '良好（P50-P90）' }
  } else if (actualMs <= p99) {
    return { level: 'warning', text: '偏慢（P90-P99）' }
  } else {
    return { level: 'slow', text: '异常慢（> P99）' }
  }
}

const activePromptTab = ref('system')

const evidenceBudgetSnapshot = computed(() => {
  const traces = stageTraces.value || []
  const budgetTrace = traces.find((item) => item.stageCode === 'EVIDENCE_BUDGET')
  return budgetTrace?.snapshot || null
})

const ragSystemPrompt = computed(() => activeExchange.value?.debugTrace?.ragSystemPrompt || '')
const ragUserPrompt = computed(() => activeExchange.value?.debugTrace?.ragUserPrompt || '')
const hasPromptData = computed(() => Boolean(ragSystemPrompt.value || ragUserPrompt.value))

async function loadRetrievalObserveData() {
  if (!conversationId.value || !exchangeId.value) {
    return
  }
  loadingRetrievalData.value = true
  try {
    const [results, executions] = await Promise.all([
      chatApi.getRetrievalResults(conversationId.value, exchangeId.value),
      chatApi.getChannelExecutions(conversationId.value, exchangeId.value)
    ])
    retrievalResults.value = results || []
    channelExecutions.value = executions || []
  } catch (error) {
    retrievalResults.value = []
    channelExecutions.value = []
  } finally {
    loadingRetrievalData.value = false
  }
}

async function loadPage() {
  if (!conversationId.value || !exchangeId.value) {
    return
  }
  loadingPage.value = true
  pageError.value = ''
  try {
    const [session, exchangeDetail] = await Promise.all([
      chatApi.getSession(conversationId.value),
      chatApi.getExchangeDetail(conversationId.value, exchangeId.value)
    ])
    activeSession.value = session
    activeExchangeDetail.value = exchangeDetail
    selectedTraceStageId.value = String(exchangeDetail?.stageTraces?.[0]?.stageId || '')
    loadRetrievalObserveData()
    loadStageBenchmarks()
  } catch (error) {
    activeSession.value = null
    activeExchangeDetail.value = null
    pageError.value = normalizeError(error, '加载轮次详情失败')
  } finally {
    loadingPage.value = false
  }
}

function openTraceDetail(stageId) {
  selectedTraceStageId.value = String(stageId || '')
  overlayInspector.value = buildTraceStageInspector(activeTraceStage.value, activeExchange.value)
  traceDetailOpen.value = true
}

function closeTraceDetail() {
  traceDetailOpen.value = false
  overlayInspector.value = null
}

function traceBarWidth(trace) {
  const duration = Number(trace?.durationMs || 0)
  const maxDuration = maxTraceDuration.value
  if (!duration || !maxDuration) {
    return '6%'
  }
  return `${Math.max((duration / maxDuration) * 100, 6)}%`
}

function findStageTrace(stageTitle) {
  if (!stageTitle) {
    return null
  }
  if (stageTitle.includes('检索执行')) {
    return stageTraces.value.find((item) => item.stageCode === 'RAG_RETRIEVE' || item.stageCode === 'REACT_AGENT') || null
  }
  if (stageTitle.includes('前置编排')) {
    return stageTraces.value.find((item) => item.stageCode === 'INTENT') || null
  }
  if (stageTitle.includes('请求入口')) {
    return stageTraces.value.find((item) => item.stageCode === 'ROUTE') || null
  }
  if (stageTitle.includes('生成回答')) {
    return stageTraces.value.find((item) => item.stageCode === 'ANSWER_GENERATE') || null
  }
  if (stageTitle.includes('模型使用')) {
    return stageTraces.value.find((item) => item.stageCode === 'ANSWER_GENERATE') || null
  }
  if (stageTitle.includes('结果与诊断')) {
    return stageTraces.value.find((item) => item.stageCode === 'FINALIZE') || null
  }
  return null
}

function canOpenStage(stage) {
  if (!stage) {
    return false
  }
  return stage.key === 'usage' || Boolean(findStageTrace(stage.title))
}

function openSummaryStage(stage) {
  if (!stage) {
    return
  }
  if (stage.key === 'usage') {
    overlayInspector.value = buildUsageStageInspector(activeExchange.value)
    traceDetailOpen.value = true
    return
  }
  const trace = findStageTrace(stage.title)
  if (!trace) {
    return
  }
  selectedTraceStageId.value = String(trace.stageId)
  overlayInspector.value = buildTraceStageInspector(trace, activeExchange.value)
  traceDetailOpen.value = true
}

watch([conversationId, exchangeId], () => {
  activeSession.value = null
  activeExchangeDetail.value = null
  traceDetailOpen.value = false
  overlayInspector.value = null
  selectedTraceStageId.value = ''
  loadPage()
}, { immediate: true })

watchEffect(() => {
  if (typeof window === 'undefined') {
    return
  }
  window.__obsDetailState = {
    loadingPage: loadingPage.value,
    hasSession: Boolean(activeSession.value),
    hasExchangeDetail: Boolean(activeExchangeDetail.value),
    conversationId: conversationId.value,
    exchangeId: exchangeId.value,
    selectedTraceStageId: selectedTraceStageId.value,
    traceDetailOpen: traceDetailOpen.value,
    overlayTitle: overlayInspector.value?.title || ''
  }
})
</script>

<style scoped>
.round-detail-page {
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

.ghost-button:disabled { opacity: 0.55; cursor: default; }

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
.neutral-badge { background: rgba(23, 48, 79, 0.06); color: var(--color-muted-strong); }
.badge-completed { background: rgba(18, 125, 74, 0.1); color: var(--color-success); }
.badge-failed { background: rgba(177, 47, 38, 0.1); color: var(--color-danger); }
.badge-stopped { background: rgba(168, 101, 32, 0.1); color: var(--color-warning); }
.badge-running { background: rgba(13, 124, 124, 0.1); color: #0d7c7c; }

.header-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 14px;
  padding: 0;
}

.meta-pair {
  display: flex;
  gap: 8px;
  align-items: baseline;
}

.meta-pair dt {
  color: var(--color-muted);
  font-size: 12px;
}

.meta-pair dd {
  margin: 0;
  color: var(--color-text-strong);
  font-size: 13px;
  font-weight: 600;
}

/* ── Section Titles ── */
.section-title {
  margin: 0 0 4px;
  font-size: 16px;
  color: var(--color-text-strong);
}

.timeline-section,
.summary-section {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

/* ── Timeline (Vertical) ── */
.timeline-list {
  display: flex;
  flex-direction: column;
  margin-top: 16px;
}

.timeline-item {
  display: flex;
  gap: 16px;
  position: relative;
}

.timeline-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 20px;
  padding-top: 6px;
}

.timeline-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-border-strong);
  flex-shrink: 0;
  z-index: 1;
  border: 2px solid #fff;
}

.dot-running { background: #0d7c7c; }
.dot-completed { background: var(--color-success); }
.dot-failed { background: var(--color-danger); }
.dot-stopped { background: var(--color-warning); }

.timeline-line {
  width: 2px;
  flex: 1;
  background: var(--color-border);
  margin-top: 4px;
}

.timeline-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 0 20px;
  border: none;
  background: none;
  text-align: left;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.timeline-content:hover {
  opacity: 0.8;
}

.timeline-item.active .timeline-content {
  background: var(--color-surface-soft);
  padding: 12px;
  border-radius: var(--radius-sm);
  margin: -6px -12px 14px;
}

.timeline-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.timeline-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.timeline-title strong {
  font-size: 15px;
  color: var(--color-text-strong);
}

.timeline-badge {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.timeline-time {
  font-size: 12px;
  color: var(--color-muted);
  white-space: nowrap;
}

.timeline-summary {
  margin: 0;
  color: var(--color-muted-strong);
  line-height: 1.6;
  font-size: 13px;
}

.timeline-bar {
  height: 4px;
  background: rgba(0, 0, 0, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.timeline-bar-fill {
  height: 100%;
  background: var(--color-primary);
  transition: width 0.3s ease;
}

.timeline-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted);
}

.timeline-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-strong);
  margin-top: 4px;
}

/* ── Summary Section ── */
.summary-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-top: 16px;
}

.summary-item {
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.summary-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.summary-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.summary-kicker {
  display: block;
  color: var(--color-muted);
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 4px;
}

.summary-title h4 {
  margin: 0 0 4px;
  font-size: 15px;
  color: var(--color-text-strong);
}

.summary-title p {
  margin: 0;
  color: var(--color-muted-strong);
  font-size: 13px;
  line-height: 1.6;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.summary-chip {
  display: inline-flex;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.chip-neutral { background: rgba(23, 48, 79, 0.06); color: var(--color-muted-strong); }
.chip-completed { background: rgba(18, 125, 74, 0.1); color: var(--color-success); }
.chip-failed { background: rgba(177, 47, 38, 0.1); color: var(--color-danger); }
.chip-warning { background: rgba(168, 101, 32, 0.1); color: var(--color-warning); }

.summary-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted-strong);
  margin-bottom: 10px;
}

.summary-pairs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 10px 0;
  padding: 0;
}

.summary-pair {
  display: flex;
  gap: 12px;
}

.summary-pair dt {
  flex-shrink: 0;
  width: 120px;
  color: var(--color-muted);
  font-size: 12px;
}

.summary-pair dd {
  margin: 0;
  color: var(--color-text);
  font-size: 13px;
  word-break: break-word;
}

.summary-preview {
  margin: 10px 0;
}

.preview-label {
  display: block;
  color: var(--color-muted);
  font-size: 12px;
  margin-bottom: 4px;
}

.summary-preview p {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
}

.summary-link {
  display: inline-flex;
  align-items: center;
  margin-top: 10px;
  border: none;
  background: none;
  padding: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-strong);
  cursor: pointer;
}

.summary-link:hover {
  text-decoration: underline;
}

/* ── Trace Overlay ── */
.trace-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: grid;
  place-items: center;
  z-index: 100;
  padding: 20px;
  overflow-y: auto;
}

.trace-panel {
  width: 100%;
  max-width: 900px;
  max-height: calc(100vh - 40px);
  background: #fff;
  border-radius: var(--radius-lg);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.panel-head {
  position: sticky;
  top: 0;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  padding: 20px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  z-index: 1;
}

.panel-head h3 {
  margin: 6px 0 4px;
  font-size: 18px;
  color: var(--color-text-strong);
}

.panel-close {
  flex-shrink: 0;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 6px 12px;
  background: #fff;
  color: var(--color-text);
  font-weight: 600;
  cursor: pointer;
}

.panel-close:hover {
  background: var(--color-surface-soft);
}

.panel-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 20px;
  background: var(--color-surface-soft);
  font-size: 12px;
  color: var(--color-muted-strong);
  border-bottom: 1px solid var(--color-border);
}

.detail-grid,
.detail-list-stack,
.table-section-stack {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-block,
.detail-list-block,
.table-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-block span,
.detail-list-block span,
.table-label {
  color: var(--color-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-block strong {
  color: var(--color-text-strong);
  line-height: 1.6;
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
  word-break: break-all;
}

.plain-list {
  margin: 0;
  padding-left: 20px;
  color: var(--color-text);
  line-height: 1.7;
}

.plain-list li {
  margin-bottom: 6px;
}

.table-wrapper {
  overflow-x: auto;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.detail-table th,
.detail-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
}

.detail-table th {
  background: var(--color-surface-soft);
  color: var(--color-muted-strong);
  font-weight: 600;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-table td {
  color: var(--color-text);
}

.advanced-panel {
  margin: 0;
  padding: 20px;
  border-top: 1px solid var(--color-border);
}

.advanced-panel summary {
  cursor: pointer;
  font-weight: 600;
  color: var(--color-primary-strong);
  font-size: 13px;
}

.advanced-panel summary:hover {
  text-decoration: underline;
}

.advanced-grid {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.advanced-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.advanced-block span {
  color: var(--color-muted);
  font-size: 12px;
}

/* ── Empty & Error ── */
.empty-card {
  padding: 48px 24px;
  text-align: center;
  color: var(--color-muted);
  line-height: 1.8;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
}

.compact-empty {
  padding: 32px 20px;
  font-size: 13px;
}

.inline-notice {
  padding: 12px 14px;
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

  .header-meta {
    flex-direction: column;
    gap: 8px;
  }

  .timeline-header,
  .summary-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .trace-panel {
    max-width: 100%;
    border-radius: var(--radius-md);
  }
}

/* 检索观测样式 */
.observe-section {
  margin-top: 32px;
  padding: 24px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.channel-comparison-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.channel-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-soft);
}

.channel-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.channel-sub-question {
  font-size: 13px;
  color: var(--color-muted-strong);
  margin-bottom: 12px;
  padding: 8px;
  background: #fff;
  border-radius: 4px;
}

.channel-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  padding: 6px 8px;
  background: #fff;
  border-radius: 4px;
  font-size: 13px;
}

.metric-label {
  color: var(--color-muted);
}

.metric-value {
  font-weight: 600;
  color: var(--color-text);
}

.metric-highlight {
  color: var(--color-primary);
}

.channel-error {
  margin-top: 12px;
  padding: 8px;
  background: #fff3f3;
  border-left: 3px solid #e74c3c;
  font-size: 12px;
  color: #c0392b;
}

.sub-question-group {
  margin-top: 24px;
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-soft);
}

.sub-question-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.channel-results-block {
  margin-top: 16px;
}

.channel-results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-radius: 4px 4px 0 0;
  border: 1px solid var(--color-border);
  border-bottom: none;
}

.result-count-badge {
  padding: 2px 8px;
  background: var(--color-primary);
  color: #fff;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.results-table-wrapper {
  overflow-x: auto;
}

.results-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 0 0 4px 4px;
  font-size: 13px;
}

.results-table th {
  padding: 10px 12px;
  text-align: left;
  background: var(--color-surface-soft);
  color: var(--color-muted-strong);
  font-weight: 600;
  border-bottom: 2px solid var(--color-border);
}

.results-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
}

.results-table tbody tr:last-child td {
  border-bottom: none;
}

.results-table tbody tr:hover {
  background: var(--color-surface-soft);
}

.row-selected {
  background: #f0f9ff !important;
}

.rank-cell {
  white-space: nowrap;
  font-weight: 600;
}

.rank-arrow {
  color: var(--color-primary);
  margin-left: 4px;
}

.chunk-cell {
  max-width: 400px;
}

.chunk-doc-name {
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 4px;
}

.chunk-section {
  font-size: 12px;
  color: var(--color-muted);
  margin-bottom: 4px;
}

.chunk-preview-text {
  font-size: 12px;
  color: var(--color-muted-strong);
  line-height: 1.4;
}

.selection-badge {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  white-space: nowrap;
}

.badge-selected {
  background: #d4edda;
  color: #155724;
}

.badge-filtered {
  background: #fff3cd;
  color: #856404;
}

.badge-omitted {
  background: #f8f9fa;
  color: #6c757d;
}

.budget-summary-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 16px;
  padding: 16px;
  background: var(--color-surface-soft);
  border-radius: var(--radius-sm);
}

.budget-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.budget-label {
  font-size: 12px;
  color: var(--color-muted);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.budget-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text);
}

.evidence-group {
  margin-top: 20px;
}

.evidence-group-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  padding-left: 12px;
  border-left: 3px solid var(--color-border);
}

.evidence-included {
  border-left-color: #28a745;
  color: #155724;
}

.evidence-omitted {
  border-left-color: #6c757d;
  color: #495057;
}

.evidence-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.evidence-list li {
  padding: 8px 12px;
  margin-bottom: 6px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.5;
}

.prompt-tabs {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  border-bottom: 2px solid var(--color-border);
}

.prompt-tab {
  padding: 8px 16px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  color: var(--color-muted-strong);
  font-weight: 600;
  cursor: pointer;
  margin-bottom: -2px;
}

.prompt-tab:hover {
  color: var(--color-text);
}

.prompt-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.prompt-content {
  margin-top: 16px;
  padding: 16px;
  background: #f8f9fa;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 600px;
  overflow-y: auto;
}

.benchmark-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.benchmark-card {
  padding: 14px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-surface-soft);
}

.benchmark-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 14px;
}

.benchmark-level {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.level-excellent { background: #d4edda; color: #155724; }
.level-good { background: #d1ecf1; color: #0c5460; }
.level-warning { background: #fff3cd; color: #856404; }
.level-slow { background: #f8d7da; color: #721c24; }

.benchmark-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.bm-item {
  display: flex;
  justify-content: space-between;
  padding: 5px 8px;
  background: #fff;
  border-radius: 4px;
  font-size: 12px;
}

.bm-label { color: var(--color-muted); }
.bm-value { font-weight: 600; }
.bm-actual { color: var(--color-primary); }
</style>
