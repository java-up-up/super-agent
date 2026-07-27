<template>
  <section class="observability-hub">
    <header class="page-header">
      <div class="header-top">
        <div class="header-copy">
          <span class="header-kicker">Conversation Observatory</span>
          <h2>先选会话，再进入整页观测详情</h2>
          <p>
            列表页只负责定位问题会话，详情页再按单轮执行阶段展开。这样不会把大量轨迹信息压缩在同一块区域里，
            也更适合教学演示和排障复盘。
          </p>
        </div>

        <div class="header-actions">
          <button class="primary-button" type="button" :disabled="loadingSessions" @click="loadSessions">
            {{ loadingSessions ? '正在刷新...' : '刷新会话列表' }}
          </button>
        </div>
      </div>

      <div class="stat-badges">
        <span v-for="item in summaryStats" :key="item.label" class="stat-badge" :title="item.description">
          <span class="stat-label">{{ item.label }}</span>
          <strong class="stat-value">{{ item.value }}</strong>
        </span>
      </div>
    </header>

    <section class="filter-bar">
      <label class="filter-field search-field">
        <span>搜索会话</span>
        <input
          v-model.trim="keyword"
          type="text"
          placeholder="按会话ID、文档名、问题或回答筛选"
          @keydown.enter.prevent="applyFilters"
        />
      </label>

      <label class="filter-field">
        <span>提问模式</span>
        <select v-model="modeFilter">
          <option value="ALL">全部模式</option>
          <option value="DOCUMENT">当前文档问答</option>
          <option value="AUTO_DOCUMENT">自动知识问答</option>
          <option value="OPEN_CHAT">开放式提问</option>
        </select>
      </label>

      <label class="filter-field">
        <span>最近状态</span>
        <select v-model="statusFilter">
          <option value="ALL">全部状态</option>
          <option value="RUNNING">进行中</option>
          <option value="COMPLETED">已完成</option>
          <option value="FAILED">失败</option>
          <option value="STOPPED">已停止</option>
        </select>
      </label>

      <div class="filter-actions">
        <button class="ghost-button" type="button" :disabled="loadingSessions" @click="resetFilters">
          重置筛选
        </button>
        <button class="primary-button inline-primary" type="button" :disabled="loadingSessions" @click="applyFilters">
          应用筛选
        </button>
      </div>
    </section>

    <div v-if="pageError" class="inline-notice error-notice">{{ pageError }}</div>
    <div v-if="loadingSessions" class="empty-card">正在加载会话列表...</div>
    <div v-else-if="!sessions.length" class="empty-card">
      当前筛选条件下没有匹配的会话。可以先清空筛选，或者去聊天页发起一轮对话再回来观察。
    </div>

    <div v-else class="session-list">
      <article
        v-for="session in sessions"
        :key="session.conversationId"
        class="session-item"
        :class="`status-${sessionTone(session)}`"
      >
        <RouterLink :to="detailTarget(session)" class="session-link">
          <div class="session-top">
            <div class="session-chips">
              <span class="chip mode-chip">{{ formatChatMode(session.chatMode) }}</span>
              <span v-if="session.running" class="chip running-chip">实时执行中</span>
              <span v-else-if="session.latestTurnStatus" class="chip" :class="`chip-${statusTone(session.latestTurnStatus)}`">
                {{ formatStatusLabel(session.latestTurnStatus) }}
              </span>
            </div>
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
          </div>

          <h3 class="session-title">{{ sessionTitle(session) }}</h3>
          <p class="session-desc">{{ sessionPreview(session) }}</p>

          <div class="session-meta">
            <code class="meta-id">{{ session.conversationId }}</code>
            <span>{{ sessionMessageCount(session) }} 条消息</span>
            <span v-if="session.selectedDocumentName">{{ session.selectedDocumentName }}</span>
          </div>

          <p v-if="session.latestTurnErrorMessage" class="session-error">
            最近一轮异常：{{ truncate(session.latestTurnErrorMessage, 88) }}
          </p>
        </RouterLink>

        <div class="session-foot">
          <RouterLink :to="detailTarget(session)" class="foot-link">查看整页详情</RouterLink>
          <RouterLink
            v-if="session.latestExchangeId"
            :to="exchangeTarget(session)"
            class="foot-link subtle"
          >
            {{ exchangeLinkLabel(session) }}
          </RouterLink>
        </div>
      </article>
    </div>

    <nav v-if="!loadingSessions && totalPagesCount > 0" class="pagination">
      <div class="pagination-info">
        <strong>第 {{ pageNo }} / {{ totalPages }} 页</strong>
        <span>共 {{ totalSize }} 条会话记录</span>
      </div>

      <div class="pagination-controls">
        <label class="page-size-select">
          <span>每页</span>
          <select v-model="pageSize" @change="handlePageSizeChange">
            <option value="12">12</option>
            <option value="24">24</option>
            <option value="36">36</option>
            <option value="48">48</option>
          </select>
        </label>

        <div class="page-buttons">
          <button class="page-btn" type="button" :disabled="!canPrev" @click="goPrevPage">上一页</button>
          <button
            v-for="(item, index) in paginationItems"
            :key="`page-${item}-${index}`"
            class="page-btn"
            :class="{ active: item === pageNo, gap: item === '...'}"
            type="button"
            :disabled="item === '...'"
            @click="typeof item === 'string' && item !== '...' ? goPage(item) : null"
          >
            {{ item }}
          </button>
          <button class="page-btn" type="button" :disabled="!canNext" @click="goNextPage">下一页</button>
        </div>
      </div>
    </nav>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { chatApi } from '../../api/api'
import {
  formatChatMode,
  formatStatusLabel,
  formatTime,
  normalizeError,
  sessionMessageCount,
  sessionPreview,
  sessionTitle,
  statusTone,
  truncate
} from './observabilityHelpers'

const sessions = ref([])
const loadingSessions = ref(false)
const pageError = ref('')
const keyword = ref('')
const modeFilter = ref('ALL')
const statusFilter = ref('ALL')
const pageNo = ref('1')
const pageSize = ref('12')
const totalSize = ref('0')
const totalPages = ref('0')

const currentPageNumber = computed(() => Number(pageNo.value || '1') || 1)
const totalPagesCount = computed(() => Number(totalPages.value || '0') || 0)
const canPrev = computed(() => currentPageNumber.value > 1)
const canNext = computed(() => totalPagesCount.value > 0 && currentPageNumber.value < totalPagesCount.value)

const summaryStats = computed(() => {
  const total = totalSize.value
  const running = sessions.value.filter((item) => item.running).length
  const documentMode = sessions.value.filter((item) => item.chatMode === 'DOCUMENT').length
  const failed = sessions.value.filter((item) => item.latestTurnStatus === 'FAILED').length

  return [
    {
      label: '会话总数',
      value: total,
      description: '后台当前可回看的全部业务会话数'
    },
    {
      label: '本页运行中',
      value: running,
      description: '正在生成中的会话会在详情页实时轮询'
    },
    {
      label: '本页文档问答',
      value: documentMode,
      description: '当前页里走 RAG 编排链路的会话规模'
    },
    {
      label: '本页最近失败',
      value: failed,
      description: '优先进入这些会话可更快定位问题'
    }
  ]
})

const paginationItems = computed(() => {
  const total = totalPagesCount.value
  const current = currentPageNumber.value
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => String(index + 1))
  }
  if (current <= 4) {
    return ['1', '2', '3', '4', '5', '...', String(total)]
  }
  if (current >= total - 3) {
    return ['1', '...', String(total - 4), String(total - 3), String(total - 2), String(total - 1), String(total)]
  }
  return ['1', '...', String(current - 1), String(current), String(current + 1), '...', String(total)]
})

async function loadSessions(options = {}) {
  loadingSessions.value = true
  pageError.value = ''

  try {
    const page = await chatApi.listSessionsPage({
      keyword: options.keyword ?? keyword.value,
      chatMode: options.chatMode ?? modeFilter.value,
      turnStatus: options.turnStatus ?? statusFilter.value,
      pageNo: options.pageNo || pageNo.value,
      pageSize: options.pageSize || pageSize.value
    })
    sessions.value = page.sessions || []
    pageNo.value = page.pageNo || '1'
    pageSize.value = page.pageSize || pageSize.value
    totalSize.value = page.totalSize || '0'
    totalPages.value = page.totalPages || '0'
  } catch (error) {
    pageError.value = normalizeError(error, '加载会话列表失败')
  } finally {
    loadingSessions.value = false
  }
}

function sessionTone(session) {
  if (session.running) {
    return 'running'
  }
  return statusTone(session.latestTurnStatus)
}

function goPage(nextPageNo) {
  if (!nextPageNo || nextPageNo === pageNo.value || loadingSessions.value) {
    return
  }
  loadSessions({
    keyword: keyword.value,
    chatMode: modeFilter.value,
    turnStatus: statusFilter.value,
    pageNo: String(nextPageNo),
    pageSize: pageSize.value
  })
}

function goPrevPage() {
  if (!canPrev.value) {
    return
  }
  goPage(String(currentPageNumber.value - 1))
}

function goNextPage() {
  if (!canNext.value) {
    return
  }
  goPage(String(currentPageNumber.value + 1))
}

function handlePageSizeChange() {
  loadSessions({
    keyword: keyword.value,
    chatMode: modeFilter.value,
    turnStatus: statusFilter.value,
    pageNo: '1',
    pageSize: pageSize.value
  })
}

function applyFilters() {
  loadSessions({
    keyword: keyword.value,
    chatMode: modeFilter.value,
    turnStatus: statusFilter.value,
    pageNo: '1',
    pageSize: pageSize.value
  })
}

function resetFilters() {
  keyword.value = ''
  modeFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  loadSessions({
    keyword: '',
    chatMode: 'ALL',
    turnStatus: 'ALL',
    pageNo: '1',
    pageSize: pageSize.value
  })
}

function detailTarget(session) {
  return {
    name: 'AdminObservabilitySession',
    params: {
      conversationId: session.conversationId
    }
  }
}

function exchangeTarget(session) {
  return {
    name: 'AdminObservabilityExchangeDetail',
    params: {
      conversationId: session.conversationId,
      exchangeId: String(session.latestExchangeId)
    }
  }
}

function exchangeLinkLabel(session) {
  if (session.running) {
    return '直达当前轮次'
  }
  if (session.latestTurnStatus === 'FAILED' || session.latestTurnStatus === 'STOPPED') {
    return '直达异常轮次'
  }
  return '直达最近轮次'
}

onMounted(loadSessions)
</script>

<style scoped>
.observability-hub {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── Page Header ── */
.page-header {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--color-border);
}

.header-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.header-kicker {
  display: inline-block;
  color: var(--color-muted);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-family: 'Fira Code', var(--font-sans);
}

.header-copy h2 {
  margin: 10px 0 8px;
  font-size: 22px;
  line-height: 1.3;
  color: var(--color-text-strong);
}

.header-copy p {
  margin: 0;
  max-width: 680px;
  color: var(--color-muted-strong);
  line-height: 1.7;
}

.stat-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 16px;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: 6px;
  background: var(--color-surface-soft);
  font-size: 13px;
  color: var(--color-muted-strong);
  cursor: default;
}

.stat-label {
  color: var(--color-muted);
}

.stat-value {
  color: var(--color-text-strong);
  font-size: 14px;
  font-family: 'Fira Code', var(--font-sans);
}

/* ── Buttons ── */
.primary-button {
  border: none;
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary);
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.primary-button:hover:not(:disabled) {
  opacity: 0.88;
}

.primary-button:disabled {
  opacity: 0.55;
  cursor: default;
}

.ghost-button {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-weight: 600;
  color: var(--color-text);
  background: #fff;
  cursor: pointer;
}

.ghost-button:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
}

/* ── Filter Bar ── */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.search-field {
  flex: 1;
  min-width: 200px;
}

.filter-field span {
  color: var(--color-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.filter-field input,
.filter-field select {
  width: 100%;
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text);
  padding: 9px 12px;
}

.filter-field input:focus,
.filter-field select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(178, 31, 196, 0.1);
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.inline-primary {
  min-height: 40px;
}

/* ── Session List ── */
.session-list {
  display: flex;
  flex-direction: column;
}

.session-item {
  position: relative;
  border-bottom: 1px solid var(--color-border);
  padding-left: 4px;
  border-left: 4px solid transparent;
  transition: background 0.15s ease;
}

.session-item:last-child {
  border-bottom: none;
}

.session-item:hover {
  background: var(--color-surface-soft);
}

.session-item.status-running {
  border-left-color: #0d7c7c;
}

.session-item.status-completed {
  border-left-color: var(--color-success);
}

.session-item.status-failed {
  border-left-color: var(--color-danger);
}

.session-item.status-stopped {
  border-left-color: var(--color-warning);
}

.session-link {
  display: block;
  padding: 16px 16px 12px;
}

.session-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.session-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.chip {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  padding: 3px 8px;
  font-size: 11px;
  font-weight: 600;
}

.mode-chip {
  background: rgba(23, 48, 79, 0.07);
  color: #17304f;
}

.running-chip {
  background: rgba(13, 124, 124, 0.1);
  color: #0d7c7c;
}

.chip-completed {
  background: rgba(18, 125, 74, 0.1);
  color: var(--color-success);
}

.chip-failed {
  background: rgba(177, 47, 38, 0.1);
  color: var(--color-danger);
}

.chip-stopped {
  background: rgba(168, 101, 32, 0.1);
  color: var(--color-warning);
}

.session-time {
  font-size: 12px;
  color: var(--color-muted);
  white-space: nowrap;
}

.session-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-strong);
  line-height: 1.4;
}

.session-desc {
  margin: 0 0 10px;
  color: var(--color-muted-strong);
  line-height: 1.65;
  font-size: 13px;
}

.session-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: var(--color-muted);
}

.meta-id {
  font-family: 'Fira Code', var(--font-sans);
  font-size: 11px;
  color: var(--color-muted);
  word-break: break-all;
}

.session-error {
  margin: 10px 0 0;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: rgba(177, 47, 38, 0.06);
  color: var(--color-danger);
  font-size: 13px;
  line-height: 1.6;
}

.session-foot {
  display: flex;
  gap: 16px;
  padding: 0 16px 14px;
}

.foot-link {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary-strong);
}

.foot-link:hover {
  text-decoration: underline;
}

.foot-link.subtle {
  color: var(--color-muted-strong);
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

/* ── Pagination ── */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.pagination-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: var(--color-muted-strong);
}

.pagination-info strong {
  color: var(--color-text-strong);
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-size-select {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-muted);
  font-size: 13px;
}

.page-size-select select {
  border-radius: var(--radius-sm);
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text);
  padding: 6px 8px;
}

.page-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.page-btn {
  min-width: 36px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 6px 10px;
  background: #fff;
  color: var(--color-text);
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--color-border-strong);
  background: var(--color-surface-soft);
}

.page-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary-strong);
}

.page-btn.gap {
  background: transparent;
  border-style: dashed;
  color: var(--color-muted);
  cursor: default;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: default;
}

/* ── Responsive ── */
@media (max-width: 980px) {
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    justify-content: flex-end;
  }

  .pagination {
    flex-direction: column;
    align-items: stretch;
  }

  .pagination-controls {
    flex-direction: column;
    align-items: stretch;
  }

  .page-size-select {
    justify-content: space-between;
  }
}

@media (max-width: 640px) {
  .header-top {
    flex-direction: column;
  }

  .session-top {
    flex-direction: column;
    align-items: flex-start;
  }

  .session-foot {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
