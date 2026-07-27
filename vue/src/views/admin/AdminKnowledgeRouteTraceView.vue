<template>
  <section class="trace-page">
    <!-- 顶部页头 + 统计条 -->
    <header class="page-header">
      <div class="page-header-left">
        <span class="section-eyebrow">Route Trace</span>
        <h3>知识路由追踪</h3>
      </div>
      <div class="page-header-stats">
        <span v-for="item in headerStats" :key="item.label" class="header-stat">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </span>
      </div>
      <button class="primary-button" type="button" :disabled="loading" @click="loadTraces">
        {{ loading ? '正在刷新...' : '刷新追踪' }}
      </button>
    </header>

    <!-- 洞察区（可折叠） -->
    <section class="insight-bar" :class="{ collapsed: insightCollapsed }">
      <div class="insight-bar-toggle" @click="insightCollapsed = !insightCollapsed">
        <span>路由洞察</span>
        <span class="collapse-arrow" :class="{ collapsed: insightCollapsed }">&#9660;</span>
      </div>
      <div v-show="!insightCollapsed" class="insight-panels">
        <article class="insight-panel">
          <div class="section-title-row">
            <h5>路由健康度</h5>
            <span>当前页样本</span>
          </div>
          <div class="health-meter-list">
            <article v-for="item in routeHealthCards" :key="item.label" class="health-meter">
              <div class="health-meter-head">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
              <div class="health-track">
                <span class="health-fill" :class="`health-fill-${item.tone}`" :style="{ width: item.percent }"></span>
              </div>
              <small>{{ item.description }}</small>
            </article>
          </div>
        </article>
        <article class="insight-panel">
          <div class="section-title-row">
            <h5>Top 候选文档分布</h5>
            <span>{{ topDocumentDistribution.length }} 个文档</span>
          </div>
          <div v-if="topDocumentDistribution.length" class="top-doc-list">
            <article v-for="item in topDocumentDistribution" :key="item.documentId" class="top-doc-row">
              <div>
                <strong>{{ item.documentName }}</strong>
                <span>出现 {{ item.count }} 次 · 均值 {{ item.averageConfidenceText }}</span>
              </div>
              <span class="top-doc-badge" :class="{ warning: item.lowConfidenceCount > 0 }">
                {{ item.lowConfidenceCount > 0 ? `${item.lowConfidenceCount} 次低置信` : '全部成功' }}
              </span>
            </article>
          </div>
          <div v-else class="candidate-empty">当前页还没有可统计的 Top 文档。</div>
        </article>
        <article class="insight-panel insight-panel-stats">
          <div class="section-title-row">
            <h5>详细统计</h5>
          </div>
          <div class="mini-stats-grid">
            <div v-for="item in summaryCards" :key="item.label" class="mini-stat">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- 主工作台：左侧列表 + 右侧详情 -->
    <div class="workbench">
      <!-- 左侧：筛选 + 列表 + 分页 -->
      <aside class="trace-sidebar">
        <div class="sidebar-toolbar">
          <input
            v-model.trim="filters.conversationId"
            class="sidebar-search"
            placeholder="按会话 ID 筛选..."
            @keydown.enter="loadTraces('1')"
          />
          <div class="sidebar-filters">
            <select v-model="filters.mode" class="filter-select-sm">
              <option value="">全部模式</option>
              <option value="shadow">shadow</option>
              <option value="auto">auto</option>
            </select>
            <select v-model="filters.routeStatus" class="filter-select-sm">
              <option value="">全部状态</option>
              <option value="1">成功</option>
              <option value="2">低置信</option>
              <option value="3">失败</option>
            </select>
          </div>
          <div class="sidebar-filter-actions">
            <button class="ghost-button-sm" type="button" :disabled="loading" @click="resetFilters">重置</button>
            <button class="primary-button-sm" type="button" :disabled="loading" @click="loadTraces('1')">筛选</button>
          </div>
        </div>

        <div class="trace-record-list">
          <div v-if="loading" class="list-empty">正在加载...</div>
          <div v-else-if="!normalizedRecords.length" class="list-empty">暂无追踪记录</div>
          <article
            v-else
            v-for="item in normalizedRecords"
            :key="item.id"
            class="trace-record-card"
            :class="{ active: selectedId === item.id }"
            @click="selectRecord(item)"
          >
            <div class="record-card-chips">
              <span class="trace-chip trace-chip-neutral">{{ item.modeLabel }}</span>
              <span class="trace-chip" :class="`trace-chip-${item.statusTone}`">{{ item.statusLabel }}</span>
              <span class="trace-chip" :class="`trace-chip-${item.confidenceBand.tone}`">{{ item.confidenceText }}</span>
            </div>
            <p class="record-card-question">{{ item.question || '未记录问题' }}</p>
            <div class="record-card-meta">
              <span>{{ primaryDocumentText(item) }}</span>
              <span>{{ formatDateTime(item.createTimeNumber) }}</span>
            </div>
          </article>
        </div>

        <nav class="sidebar-pagination">
          <button class="ghost-button-sm" type="button" :disabled="Number(page.pageNo) <= 1 || loading" @click="changePage(Number(page.pageNo) - 1)">上一页</button>
          <span>{{ page.pageNo }} / {{ page.totalPages || '0' }}</span>
          <button class="ghost-button-sm" type="button" :disabled="Number(page.pageNo) >= Number(page.totalPages || 0) || loading" @click="changePage(Number(page.pageNo) + 1)">下一页</button>
        </nav>
      </aside>

      <!-- 右侧：详情面板 -->
      <main class="trace-detail">
        <div v-if="!selectedRecord" class="detail-empty">
          <p>从左侧选择一条追踪记录查看详情</p>
        </div>

        <template v-else>
          <!-- 详情头部 -->
          <div class="detail-head">
            <div class="detail-head-chips">
              <span class="trace-chip trace-chip-neutral">{{ selectedRecord.modeLabel }}</span>
              <span class="trace-chip" :class="`trace-chip-${selectedRecord.statusTone}`">{{ selectedRecord.statusLabel }}</span>
              <span class="trace-chip" :class="`trace-chip-${selectedRecord.confidenceBand.tone}`">
                {{ selectedRecord.confidenceBand.label }} · {{ selectedRecord.confidenceText }}
              </span>
            </div>
            <h4>{{ selectedRecord.question || '未记录问题' }}</h4>
            <p class="detail-rewrite">改写问题：{{ selectedRecord.rewriteQuestion || '未记录改写问题' }}</p>
            <div class="detail-meta-row">
              <span>{{ formatDateTime(selectedRecord.createTimeNumber) }}</span>
              <span>会话 {{ shortenId(selectedRecord.conversationId) }}</span>
              <span>轮次 {{ selectedRecord.exchangeId || '-' }}</span>
            </div>
          </div>

          <!-- 四格摘要 -->
          <div class="detail-summary-grid">
            <article class="summary-card highlight-card">
              <p class="summary-label">主候选文档</p>
              <strong>{{ primaryDocumentText(selectedRecord) }}</strong>
              <span>{{ selectedRecord.reasonText || '当前未记录额外路由说明' }}</span>
            </article>
            <article class="summary-card">
              <p class="summary-label">实际落点</p>
              <strong>{{ actualSelectionText(selectedRecord) }}</strong>
              <span>{{ hitConclusion(selectedRecord) }}</span>
            </article>
            <article class="summary-card">
              <p class="summary-label">候选规模</p>
              <strong>{{ selectedRecord.candidateDocumentCount }} 文档 / {{ selectedRecord.candidateTopicCount }} 主题 / {{ selectedRecord.candidateScopeCount }} 范围</strong>
              <span>{{ candidateConclusion(selectedRecord) }}</span>
            </article>
            <article class="summary-card">
              <p class="summary-label">观察建议</p>
              <strong>{{ recommendationTitle(selectedRecord) }}</strong>
              <span>{{ recommendationText(selectedRecord) }}</span>
            </article>
          </div>

          <!-- 候选文档时间线 -->
          <section v-if="selectedRecord.documents.length" class="detail-section">
            <div class="section-title-row">
              <h5>候选文档</h5>
              <span>{{ selectedRecord.documents.length }} 份</span>
            </div>
            <div class="doc-timeline">
              <article
                v-for="(candidate, index) in selectedRecord.documents"
                :key="`doc-${candidate.documentId || index}`"
                class="doc-timeline-item"
                :class="{ 'doc-timeline-top': index === 0 }"
              >
                <div class="doc-timeline-rank">{{ index + 1 }}</div>
                <div class="doc-timeline-body">
                  <strong>{{ candidate.documentName || candidate.documentId }}</strong>
                  <span class="doc-score">分数 {{ candidate.scoreText }}</span>
                  <small>{{ candidate.reason || '基于文档画像与元数据综合召回' }}</small>
                </div>
              </article>
            </div>
          </section>

          <!-- 范围 + 主题候选 -->
          <div class="detail-columns">
            <section class="detail-section">
              <div class="section-title-row">
                <h5>范围候选</h5>
                <span>{{ selectedRecord.scopes.length }} 个</span>
              </div>
              <div v-if="selectedRecord.scopes.length" class="candidate-chip-list">
                <span
                  v-for="(c, i) in selectedRecord.scopes"
                  :key="`scope-${c.scopeCode || i}`"
                  class="candidate-chip"
                >{{ c.scopeName || c.scopeCode }} · {{ c.scoreText }}</span>
              </div>
              <p v-else class="candidate-empty">当前没有显式范围候选。</p>
            </section>
            <section class="detail-section">
              <div class="section-title-row">
                <h5>主题候选</h5>
                <span>{{ selectedRecord.topics.length }} 个</span>
              </div>
              <div v-if="selectedRecord.topics.length" class="candidate-chip-list">
                <span
                  v-for="(c, i) in selectedRecord.topics"
                  :key="`topic-${c.topicCode || i}`"
                  class="candidate-chip"
                >{{ c.topicName || c.topicCode }} · {{ c.scoreText }}</span>
              </div>
              <p v-else class="candidate-empty">当前没有显式主题候选。</p>
            </section>
          </div>

          <!-- 原始 JSON -->
          <details class="raw-toggle">
            <summary>查看原始 JSON</summary>
            <div class="raw-grid">
              <pre>{{ formatJson(selectedRecord.topScopesJson) }}</pre>
              <pre>{{ formatJson(selectedRecord.topTopicsJson) }}</pre>
              <pre>{{ formatJson(selectedRecord.topDocumentsJson) }}</pre>
            </div>
          </details>
        </template>
      </main>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { manageApi } from '../../api/api'
import { formatDateTime } from './observabilityHelpers'
import { buildTopDocumentDistribution, normalizeRouteTrace, summarizeRouteTraceRecords } from '../../utils/knowledgeRoute'

const loading = ref(false)
const records = ref([])
const selectedId = ref(null)
const insightCollapsed = ref(true)
const filters = reactive({
  conversationId: '',
  mode: '',
  routeStatus: ''
})
const page = reactive({
  pageNo: '1',
  pageSize: '20',
  totalSize: '0',
  totalPages: '0'
})

const normalizedRecords = computed(() => (records.value || []).map((item) => normalizeRouteTrace(item)))
const selectedRecord = computed(() => normalizedRecords.value.find((r) => r.id === selectedId.value) || null)
const traceStats = computed(() => summarizeRouteTraceRecords(records.value || []))
const topDocumentDistribution = computed(() => buildTopDocumentDistribution(records.value || []))

const headerStats = computed(() => [
  { label: '总追踪量', value: page.totalSize || '0' },
  { label: '成功率', value: traceStats.value.successRateText },
  { label: '平均置信度', value: traceStats.value.averageConfidenceText },
  { label: 'shadow 命中率', value: traceStats.value.shadowHitRateText },
  { label: '低置信或失败', value: String(traceStats.value.lowConfidenceCount + traceStats.value.failedCount) }
])

const summaryCards = computed(() => [
  { label: '总追踪量', value: page.totalSize || '0', description: '符合当前筛选条件的全部路由记录数' },
  { label: '本页 auto', value: String(traceStats.value.autoCount), description: '自动知识问答实际落下的路由记录' },
  { label: '本页 shadow', value: String(traceStats.value.shadowCount), description: '当前文档问答的影子路由对比记录' },
  { label: '高置信', value: String(traceStats.value.highConfidenceCount), description: '置信度 >= 0.8000 的样本数' },
  { label: '低置信或失败', value: String(traceStats.value.lowConfidenceCount + traceStats.value.failedCount), description: '这些样本最适合回头补知识范围、主题别名或文档画像' },
  { label: 'shadow Top3 命中率', value: traceStats.value.shadowHitRateText, description: '人工选文档是否与影子路由基本一致' },
  { label: '平均置信度', value: traceStats.value.averageConfidenceText, description: '当前页所有路由样本的平均置信度' },
  { label: '成功率', value: traceStats.value.successRateText, description: '当前页成功路由样本占比' },
  { label: '低置信率', value: traceStats.value.lowConfidenceRateText, description: '低置信或失败样本占比' },
  { label: '均候选文档', value: traceStats.value.averageDocumentCountText, description: '每轮路由平均产出的候选文档数量' },
  { label: '扩范围次数', value: String(traceStats.value.widenedCount), description: '低置信时自动放宽候选范围的次数' }
])

const routeHealthCards = computed(() => [
  { label: '成功率', value: traceStats.value.successRateText, percent: normalizePercent(traceStats.value.successRateText), tone: 'success', description: '越高说明自动候选越稳定。' },
  { label: '低置信率', value: traceStats.value.lowConfidenceRateText, percent: normalizePercent(traceStats.value.lowConfidenceRateText), tone: 'warning', description: '越高说明范围、主题或画像还需要补强。' },
  { label: '候选文档均值', value: traceStats.value.averageDocumentCountText, percent: `${Math.min(100, Number(traceStats.value.averageDocumentCountText || 0) * 20)}%`, tone: 'neutral', description: '高置信时通常接近 3，低置信时会放宽到 5。' }
])

function selectRecord(item) {
  selectedId.value = item.id
}

async function loadTraces(nextPage = page.pageNo) {
  loading.value = true
  try {
    const data = await manageApi.queryKnowledgeRouteTracePage({
      ...filters,
      pageNo: String(nextPage),
      pageSize: page.pageSize
    })
    records.value = data?.records || []
    page.pageNo = data?.pageNo || '1'
    page.pageSize = data?.pageSize || page.pageSize
    page.totalSize = data?.totalSize || '0'
    page.totalPages = data?.totalPages || '0'
    selectedId.value = null
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.conversationId = ''
  filters.mode = ''
  filters.routeStatus = ''
  loadTraces('1')
}

function changePage(nextPage) {
  if (nextPage <= 0) return
  loadTraces(String(nextPage))
}

function normalizePercent(value) {
  const numeric = Number(String(value || '').replace('%', ''))
  if (!Number.isFinite(numeric)) return '0%'
  return `${Math.max(0, Math.min(100, numeric))}%`
}

function formatJson(value) {
  if (!value) return '[]'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

function shortenId(value) {
  const normalized = String(value || '')
  if (normalized.length <= 14) return normalized || '-'
  return `${normalized.slice(0, 6)}...${normalized.slice(-6)}`
}

function primaryDocumentText(item) {
  return item.topDocument?.documentName || item.topDocument?.documentId || '未形成显式主候选'
}

function actualSelectionText(item) {
  if (item.mode === 'auto') {
    return item.topDocument?.documentName || item.topDocument?.documentId || '执行期可能回退到通用可检索文档池'
  }
  return item.selectedDocument?.documentName || item.selectedDocumentId || '未记录当前文档'
}

function hitConclusion(item) {
  if (item.mode === 'auto') {
    return item.topDocument ? '自动模式会以该主候选为中心，再进入稳定检索主链。' : '当前没有明确主候选，说明需要继续补范围、主题或文档画像。'
  }
  if (item.hitTop3) return '影子路由 Top3 已覆盖当前文档，人工选择与自动路由基本一致。'
  if (item.missedTop3) return '影子路由 Top3 未覆盖当前文档，说明这轮问题可能更像跨文档。'
  return '当前样本还不足以判断影子路由与人工选择是否一致。'
}

function candidateConclusion(item) {
  if (item.lowConfidenceWidened) return '当前是低置信样本，系统已经自动放宽候选文档规模。'
  if (!item.documents.length) return '当前没有候选文档，优先检查文档画像、主题关联与标签是否完整。'
  return '候选规模已经稳定，后续主要观察 Top1 和 Top3 的命中情况。'
}

function recommendationTitle(item) {
  if (item.mode === 'auto' && item.statusKey === 'SUCCESS' && (item.confidenceNumber || 0) >= 0.8) return '可以继续扩大样本观察'
  if (item.lowConfidenceWidened || item.statusKey === 'LOW_CONFIDENCE') return '建议补强知识范围和主题别名'
  if (item.statusKey === 'FAILED') return '建议优先排查空路由原因'
  if (item.mode === 'shadow' && item.missedTop3) return '建议检查当前文档是否放错范围'
  return '当前配置可继续保留'
}

function recommendationText(item) {
  if (item.lowConfidenceWidened || item.statusKey === 'LOW_CONFIDENCE') return '优先补 documentTags、knowledgeScopeName、topic 别名，以及 topic-document relation 的人工确认。'
  if (item.statusKey === 'FAILED') return '当前路由没有形成稳定候选，先检查上传元数据、文档画像和主题树是否为空。'
  if (item.mode === 'shadow' && item.missedTop3) return '人工选文档和自动路由差异较大，建议对比问题表达与文档画像的关键词覆盖情况。'
  return '当前样本已经接近可教学展示状态，下一步重点看不同问题类型下是否还能持续稳定。'
}

onMounted(() => loadTraces('1'))
</script>

<style scoped>
/* ── 页面骨架 ── */
.trace-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: calc(100vh - 52px - 40px);
  overflow: hidden;
}

/* ── 页头 ── */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm);
  flex: none;
}

.page-header-left {
  flex: none;
}

.page-header-left h3 {
  margin: 0;
  font-size: 16px;
  color: var(--color-text-strong);
}

.section-eyebrow {
  font-size: 11px;
  color: var(--color-muted);
  font-weight: 500;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.page-header-stats {
  display: flex;
  gap: 20px;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

.header-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.header-stat strong {
  font-size: 18px;
  color: var(--color-text-strong);
  line-height: 1;
}

.header-stat span {
  font-size: 11px;
  color: var(--color-muted);
}

/* ── 洞察区 ── */
.insight-bar {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm);
  flex: none;
  overflow: hidden;
}

.insight-bar-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 18px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  user-select: none;
}

.insight-bar-toggle:hover {
  background: var(--color-surface-soft);
}

.collapse-arrow {
  font-size: 10px;
  color: var(--color-muted);
  transition: transform 0.2s;
}

.collapse-arrow.collapsed {
  transform: rotate(-90deg);
}

.insight-panels {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  border-top: 1px solid var(--color-border);
}

.insight-panel {
  padding: 16px 18px;
  border-right: 1px solid var(--color-border);
}

.insight-panel:last-child {
  border-right: none;
}

.mini-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mini-stat strong {
  font-size: 15px;
  color: var(--color-text-strong);
}

.mini-stat span {
  font-size: 11px;
  color: var(--color-muted);
}

/* ── 主工作台 ── */
.workbench {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 12px;
  flex: 1;
  min-height: 0;
}

/* ── 左侧列表 ── */
.trace-sidebar {
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.sidebar-toolbar {
  padding: 10px 12px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: none;
}

.sidebar-search {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 8px);
  padding: 8px 10px;
  font-size: 13px;
  color: var(--color-text-strong);
  background: var(--color-surface-soft);
  box-sizing: border-box;
}

.sidebar-filters {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
}

.filter-select-sm {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm, 8px);
  padding: 6px 8px;
  font-size: 12px;
  color: var(--color-text-strong);
  background: #fff;
}

.sidebar-filter-actions {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.ghost-button-sm,
.primary-button-sm {
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
}

.ghost-button-sm {
  background: #fff;
  color: var(--color-text);
  border-color: var(--color-border);
}

.ghost-button-sm:disabled,
.primary-button-sm:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.primary-button-sm {
  background: var(--color-primary);
  color: #fff;
}

.trace-record-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.list-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--color-muted);
  font-size: 13px;
}

.trace-record-card {
  padding: 12px;
  border-radius: var(--radius-md, 10px);
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 4px;
  transition: background 0.12s, border-color 0.12s;
}

.trace-record-card:hover {
  background: var(--color-surface-soft);
}

.trace-record-card.active {
  background: var(--color-primary-soft);
  border-color: rgba(178, 31, 196, 0.2);
}

.record-card-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.record-card-question {
  margin: 0 0 6px;
  font-size: 13px;
  color: var(--color-text-strong);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.record-card-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
  color: var(--color-muted);
}

.record-card-meta span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.sidebar-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 10px;
  border-top: 1px solid var(--color-border);
  font-size: 12px;
  color: var(--color-muted);
  flex: none;
}

/* ── 右侧详情 ── */
.trace-detail {
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg, 12px);
  box-shadow: var(--shadow-sm);
  overflow-y: auto;
  padding: 20px 24px;
}

.detail-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted);
  font-size: 14px;
}

.detail-head {
  margin-bottom: 20px;
}

.detail-head-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.detail-head h4 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--color-text-strong);
  line-height: 1.5;
}

.detail-rewrite {
  margin: 0 0 10px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.6;
}

.detail-meta-row {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--color-muted);
}

.detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 20px;
}

.summary-card {
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.82);
}

.highlight-card {
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.07), rgba(239, 123, 57, 0.07));
}

.summary-label {
  margin: 0 0 4px;
  font-size: 11px;
  color: var(--color-muted);
}

.summary-card strong {
  display: block;
  margin-bottom: 6px;
  color: var(--color-text-strong);
  line-height: 1.5;
}

.summary-card span {
  display: block;
  font-size: 12px;
  color: var(--color-muted);
  line-height: 1.7;
}

/* ── 候选文档时间线 ── */
.detail-section {
  margin-bottom: 20px;
}

.doc-timeline {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-timeline-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.82);
}

.doc-timeline-top {
  border-color: rgba(178, 31, 196, 0.2);
  background: linear-gradient(135deg, rgba(178, 31, 196, 0.05), rgba(255, 255, 255, 0.9));
}

.doc-timeline-rank {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--color-surface-soft);
  border: 1px solid var(--color-border);
  display: grid;
  place-items: center;
  font-size: 11px;
  font-weight: 700;
  color: var(--color-muted);
  flex: none;
}

.doc-timeline-top .doc-timeline-rank {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: #fff;
}

.doc-timeline-body {
  flex: 1;
  min-width: 0;
}

.doc-timeline-body strong {
  display: block;
  color: var(--color-text-strong);
  margin-bottom: 2px;
}

.doc-score {
  display: inline-block;
  font-size: 12px;
  color: var(--color-muted);
  margin-bottom: 4px;
}

.doc-timeline-body small {
  display: block;
  font-size: 12px;
  color: var(--color-muted);
  line-height: 1.6;
}

/* ── 范围 + 主题双列 ── */
.detail-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.section-title-row h5 {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-strong);
}

.section-title-row span {
  font-size: 12px;
  color: var(--color-muted);
}

.candidate-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.candidate-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.06);
  color: var(--color-text);
  font-size: 12px;
}

.candidate-empty {
  font-size: 12px;
  color: var(--color-muted);
}

/* ── 原始 JSON ── */
.raw-toggle {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
}

.raw-toggle summary {
  cursor: pointer;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 600;
}

.raw-grid {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

pre {
  overflow: auto;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 12px;
  font-size: 12px;
  margin: 0;
}

/* ── Chip 颜色 ── */
.trace-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
}

.trace-chip-neutral { background: rgba(15, 23, 42, 0.06); color: var(--color-text); }
.trace-chip-success { background: rgba(34, 197, 94, 0.12); color: #15803d; }
.trace-chip-warning { background: rgba(245, 158, 11, 0.14); color: #b45309; }
.trace-chip-danger  { background: rgba(239, 68, 68, 0.12); color: #b91c1c; }

/* ── 健康度 ── */
.health-meter-list { display: grid; gap: 10px; }

.health-meter {
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.82);
}

.health-meter-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.health-meter-head span, .health-meter small { color: var(--color-muted); font-size: 12px; }
.health-meter-head strong { color: var(--color-text-strong); font-size: 13px; }

.health-track {
  margin: 8px 0 6px;
  height: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.health-fill { display: block; height: 100%; border-radius: 999px; }
.health-fill-success { background: linear-gradient(90deg, #22c55e, #16a34a); }
.health-fill-warning { background: linear-gradient(90deg, #f59e0b, #d97706); }
.health-fill-neutral { background: linear-gradient(90deg, #b21fc4, #0a6d7a); }

/* ── Top 文档 ── */
.top-doc-list { display: grid; gap: 8px; }

.top-doc-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.82);
}

.top-doc-row strong { display: block; color: var(--color-text-strong); font-size: 13px; }
.top-doc-row span { color: var(--color-muted); font-size: 12px; }

.top-doc-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.top-doc-badge.warning { background: rgba(245, 158, 11, 0.14); color: #b45309; }

/* ── 按钮 ── */
.primary-button {
  border: 1px solid transparent;
  border-radius: 999px;
  padding: 9px 16px;
  font-weight: 600;
  cursor: pointer;
  background: var(--color-primary);
  color: #fff;
  flex: none;
}

.primary-button:disabled { opacity: 0.6; cursor: not-allowed; }

/* ── 响应式 ── */
@media (max-width: 1100px) {
  .workbench { grid-template-columns: 300px minmax(0, 1fr); }
  .insight-panels { grid-template-columns: 1fr 1fr; }
  .insight-panel:nth-child(3) { grid-column: span 2; border-right: none; border-top: 1px solid var(--color-border); }
}

@media (max-width: 860px) {
  .workbench { grid-template-columns: 1fr; }
  .trace-page { height: auto; overflow: visible; }
  .trace-sidebar { height: 360px; }
  .insight-panels { grid-template-columns: 1fr; }
  .insight-panel { border-right: none; border-bottom: 1px solid var(--color-border); }
  .detail-summary-grid, .detail-columns, .raw-grid { grid-template-columns: 1fr; }
  .page-header-stats { display: none; }
}
</style>
