<template>
  <section class="flex flex-col gap-3 overflow-hidden" :style="pageStyle">
    <!-- 页头 -->
    <header class="glass-card glass-edge flex flex-none items-center gap-4 rounded-glass border px-5 py-3.5">
      <div class="flex-none">
        <h2 class="m-0 text-base font-semibold text-foreground">知识路由追踪</h2>
      </div>
      <div class="flex flex-1 flex-wrap justify-center gap-5 max-[860px]:hidden">
        <span v-for="item in headerStats" :key="item.label" class="flex flex-col items-center gap-0.5">
          <strong class="text-lg leading-none text-foreground">{{ item.value }}</strong>
          <span class="text-micro text-muted-foreground">{{ item.label }}</span>
        </span>
      </div>
      <Button size="lg" class="flex-none rounded-md" :disabled="loading" @click="loadTraces">
        {{ loading ? '正在刷新...' : '刷新追踪' }}
      </Button>
    </header>

    <!-- 洞察区（可折叠） -->
    <div class="glass-card glass-edge flex-none overflow-hidden rounded-glass border">
      <div
        class="flex cursor-pointer select-none items-center justify-between px-[18px] py-2.5 text-compact font-semibold text-foreground hover:bg-secondary focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-inset focus-visible:ring-ring/40"
        role="button"
        tabindex="0"
        :aria-expanded="!insightCollapsed"
        aria-controls="route-trace-insight-panel"
        @click="insightCollapsed = !insightCollapsed"
        @keydown.enter.prevent="insightCollapsed = !insightCollapsed"
        @keydown.space.prevent="insightCollapsed = !insightCollapsed"
      >
        <span>路由洞察</span>
        <span class="text-technical text-muted-foreground transition-transform" :class="insightCollapsed ? '-rotate-90' : ''">&#9660;</span>
      </div>
      <div id="route-trace-insight-panel" v-show="!insightCollapsed" class="grid grid-cols-3 border-t border-border max-[1100px]:grid-cols-2 max-[860px]:grid-cols-1">
        <article class="border-r border-border p-4 max-[860px]:border-b max-[860px]:border-r-0 max-[1100px]:border-r">
          <div class="mb-2.5 flex items-center justify-between gap-2">
            <h5 class="m-0 text-compact font-semibold text-foreground">本页响应统计</h5>
            <span class="text-xs text-muted-foreground">当前页样本</span>
          </div>
          <div class="grid gap-2.5">
            <article v-for="item in routeHealthCards" :key="item.label" class="rounded-lg border border-border bg-background p-2.5">
              <div class="flex items-center justify-between gap-2">
                <span class="text-xs text-muted-foreground">{{ item.label }}</span>
                <strong class="text-compact text-foreground">{{ item.value }}</strong>
              </div>
              <div class="my-2 h-1.5 overflow-hidden rounded-full bg-foreground/[0.08]">
                <span :class="healthFillClass(item.tone)" :style="{ width: item.percent }"></span>
              </div>
              <small class="text-micro text-muted-foreground">{{ item.description }}</small>
            </article>
          </div>
        </article>
        <article class="border-r border-border p-4 max-[860px]:border-b max-[860px]:border-r-0">
          <div class="mb-2.5 flex items-center justify-between gap-2">
            <h5 class="m-0 text-compact font-semibold text-foreground">Top 候选文档分布</h5>
            <span class="text-xs text-muted-foreground">{{ topDocumentDistribution.length }} 个文档</span>
          </div>
          <div v-if="topDocumentDistribution.length" class="grid gap-2">
            <article v-for="item in topDocumentDistribution" :key="item.documentId" class="flex items-center justify-between gap-2.5 rounded-lg border border-border bg-background p-2.5">
              <div>
                <strong class="block text-compact text-foreground">{{ item.documentName }}</strong>
                <span class="text-xs text-muted-foreground">出现 {{ item.count }} 次 · 均值 {{ item.averageConfidenceText }}</span>
              </div>
              <span class="inline-flex whitespace-nowrap rounded-md px-2 py-1 text-micro font-bold"
                :class="item.lowConfidenceCount > 0 ? 'bg-amber-500/[0.14] text-amber-700' : 'bg-green-500/[0.12] text-green-700'"
              >{{ item.lowConfidenceCount > 0 ? `${item.lowConfidenceCount} 次低置信` : '全部成功' }}</span>
            </article>
          </div>
          <p v-else class="text-xs text-muted-foreground">当前页还没有可统计的 Top 文档。</p>
        </article>
        <article class="p-4 max-[1100px]:col-span-2 max-[1100px]:border-t max-[860px]:col-span-1">
          <div class="mb-2.5 flex items-center justify-between gap-2">
            <h5 class="m-0 text-compact font-semibold text-foreground">详细统计</h5>
          </div>
          <div class="grid grid-cols-3 gap-2.5">
            <div v-for="item in summaryCards" :key="item.label" class="flex flex-col gap-0.5">
              <strong class="text-body-sm text-foreground">{{ item.value }}</strong>
              <span class="text-micro text-muted-foreground">{{ item.label }}</span>
            </div>
          </div>
        </article>
      </div>
    </div>

    <div v-if="pageError && normalizedRecords.length" class="flex-none rounded-md border border-destructive/20 bg-destructive/[0.05] px-3 py-2 text-xs text-destructive" role="status">
      {{ pageError }}；已保留上一次成功的追踪列表。
    </div>
    <div v-else-if="pageError" class="flex-none rounded-md border border-destructive/20 bg-destructive/[0.05] px-3 py-2 text-xs text-destructive" role="alert">
      {{ pageError }}
      <Button variant="outline" size="sm" class="ml-2 rounded-md" type="button" @click="loadTraces('1')">重新加载</Button>
    </div>

    <!-- 主工作台 -->
    <div class="grid min-h-0 flex-1 grid-cols-[340px_minmax(0,1fr)] gap-3 max-[1100px]:grid-cols-[300px_minmax(0,1fr)] max-[860px]:grid-cols-1 max-[860px]:flex-col">
      <!-- 左侧列表 -->
      <aside data-testid="server-paged" class="glass-card glass-edge flex flex-col overflow-hidden rounded-glass border max-[860px]:h-[360px]" :class="workspacePane === 'list' ? 'flex' : 'max-[860px]:hidden'">
        <div class="flex flex-none flex-col gap-2 border-b border-border p-3">
          <Input v-model.trim="filters.conversationId" aria-label="按会话 ID 筛选路由追踪" class="w-full h-8 text-compact" placeholder="按会话 ID 筛选..." @keydown.enter="loadTraces('1')" />
          <div class="flex items-center gap-1.5">
            <div class="flex-1 min-w-0">
              <Select v-model="filters.mode">
                <SelectTrigger aria-label="按执行模式筛选路由追踪" class="w-full h-8 rounded-md border border-input bg-background px-2 text-xs text-foreground">
                  <SelectValue placeholder="全部模式" />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value="__all__">全部模式</SelectItem>
                    <SelectItem value="shadow">shadow</SelectItem>
                    <SelectItem value="auto">auto</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>
            <div class="flex-1 min-w-0">
              <Select v-model="filters.routeStatus">
                <SelectTrigger aria-label="按路由状态筛选路由追踪" class="w-full h-8 rounded-md border border-input bg-background px-2 text-xs text-foreground">
                  <SelectValue placeholder="全部状态" />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value="__all__">全部状态</SelectItem>
                    <SelectItem value="1">成功</SelectItem>
                    <SelectItem value="2">低置信</SelectItem>
                    <SelectItem value="3">失败</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>
            <Button variant="outline" size="sm" class="h-8 flex-none rounded-md px-2.5" type="button" :disabled="loading" @click="resetFilters">重置</Button>
            <Button size="sm" class="h-8 flex-none rounded-md px-2.5" type="button" :disabled="loading" @click="loadTraces('1')">筛选</Button>
          </div>
        </div>

        <div class="flex-1 overflow-y-auto p-1.5">
          <div v-if="loading && !normalizedRecords.length" class="py-8 text-center text-compact text-muted-foreground">正在加载...</div>
          <div v-else-if="!normalizedRecords.length" class="py-8 text-center text-compact text-muted-foreground">暂无追踪记录</div>
          <div
            v-else
            v-for="item in normalizedRecords"
            :key="item.id"
            class="mb-1 cursor-pointer rounded-lg border p-3 transition-colors focus-visible:outline-none focus-visible:ring-3 focus-visible:ring-ring/40"
            :class="selectedId === item.id ? 'border-primary/20 bg-primary/[0.09]' : 'border-transparent hover:bg-secondary'"
            role="button"
            tabindex="0"
            @click="selectRecord(item)"
            @keydown.enter.prevent="selectRecord(item)"
            @keydown.space.prevent="selectRecord(item)"
          >
            <div class="mb-1.5 flex flex-wrap gap-1">
              <Badge variant="outline" class="text-micro font-bold" :class="resolveRouteModeBadgeClass(item.modeTone)" :data-route-mode="item.mode">{{ item.modeLabel }}</Badge>
              <StatusBadge class="h-5 text-micro font-bold" :tone="item.statusTone" :label="item.statusLabel" />
              <Badge variant="secondary" class="text-micro font-bold">置信度 {{ item.confidenceText }}</Badge>
            </div>
            <p class="mb-1.5 line-clamp-2 text-compact leading-snug text-foreground">{{ item.question || '未记录问题' }}</p>
            <div class="flex justify-between gap-2 text-micro text-muted-foreground">
              <span class="min-w-0 flex-1 overflow-hidden text-ellipsis whitespace-nowrap">{{ primaryDocumentText(item) }}</span>
              <span class="shrink-0">{{ formatDateTime(item.createTimeNumber) }}</span>
            </div>
          </div>
        </div>

        <nav class="flex flex-none items-center justify-center gap-2.5 border-t border-border p-2.5 text-xs text-muted-foreground">
          <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="Number(page.pageNo) <= 1 || loading" @click="changePage(Number(page.pageNo) - 1)">上一页</Button>
          <span>{{ page.pageNo }} / {{ page.totalPages || '0' }}</span>
          <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="Number(page.pageNo) >= Number(page.totalPages || 0) || loading" @click="changePage(Number(page.pageNo) + 1)">下一页</Button>
        </nav>
      </aside>

      <!-- 右侧详情 -->
      <div class="glass-card glass-edge overflow-y-auto rounded-glass border p-5" :class="workspacePane === 'detail' ? 'block' : 'max-[860px]:hidden'">
        <Button variant="ghost" size="sm" class="mb-3 rounded-md px-0 max-[860px]:inline-flex md:hidden" type="button" @click="workspacePane = 'list'">
          ← 返回追踪列表
        </Button>
        <div v-if="!selectedRecord" class="flex h-full items-center justify-center text-sm text-muted-foreground">
          <p>从左侧选择一条追踪记录查看详情</p>
        </div>

        <template v-else>
          <div class="mb-5">
            <div class="mb-2.5 flex flex-wrap gap-1.5">
              <Badge variant="outline" class="text-micro font-bold" :class="resolveRouteModeBadgeClass(selectedRecord.modeTone)" :data-route-mode="selectedRecord.mode">{{ selectedRecord.modeLabel }}</Badge>
              <StatusBadge class="h-5 text-micro font-bold" :tone="selectedRecord.statusTone" :label="selectedRecord.statusLabel" />
              <Badge variant="secondary" class="text-micro font-bold">置信度 · {{ selectedRecord.confidenceText }}</Badge>
            </div>
            <h4 class="mb-1.5 text-base font-semibold text-foreground">{{ selectedRecord.question || '未记录问题' }}</h4>
            <p class="mb-2.5 text-compact leading-relaxed text-muted-foreground">改写问题：{{ selectedRecord.rewriteQuestion || '未记录改写问题' }}</p>
            <div class="flex flex-wrap gap-4 text-xs text-muted-foreground">
              <span>{{ formatDateTime(selectedRecord.createTimeNumber) }}</span>
            </div>
          </div>

          <div class="mb-5 grid grid-cols-2 gap-2.5 max-[860px]:grid-cols-1">
            <article class="rounded-lg border border-border border-l-2 border-l-primary bg-background p-3.5">
              <p class="mb-1 text-micro font-semibold text-primary">主候选文档</p>
              <strong class="mb-1.5 block leading-snug text-foreground">{{ primaryDocumentText(selectedRecord) }}</strong>
              <span class="text-xs leading-relaxed text-muted-foreground">{{ selectedRecord.reasonText || '当前未记录额外路由说明' }}</span>
            </article>
            <article v-for="card in detailSummaryCards" :key="card.label" class="rounded-lg border border-border bg-background p-3.5">
              <p class="mb-1 text-micro text-muted-foreground">{{ card.label }}</p>
              <strong class="mb-1.5 block leading-snug text-foreground">{{ card.value }}</strong>
              <span class="text-xs leading-relaxed text-muted-foreground">{{ card.desc }}</span>
            </article>
          </div>

          <section v-if="selectedRecord.documents.length" class="mb-5">
            <div class="mb-2.5 flex items-center justify-between gap-2.5">
              <h5 class="m-0 text-compact font-semibold text-foreground">候选文档</h5>
              <span class="text-xs text-muted-foreground">{{ selectedRecord.documents.length }} 份</span>
            </div>
            <div class="flex flex-col gap-2">
              <article v-for="(candidate, index) in selectedRecord.documents" :key="`doc-${candidate.documentId || index}`"
                class="flex gap-3 rounded-lg border p-3"
                :class="index === 0 ? 'border-border border-l-2 border-l-primary bg-background' : 'border-border bg-background'">
                <div class="grid h-6 w-6 shrink-0 place-items-center rounded-full border text-micro font-bold"
                  :class="index === 0 ? 'border-primary bg-primary text-white' : 'border-border bg-secondary text-muted-foreground'">{{ index + 1 }}</div>
                <div class="flex-1 min-w-0">
                  <strong class="mb-0.5 block text-foreground">{{ candidate.documentName || candidate.documentId }}</strong>
                  <span class="mb-1 inline-block text-xs text-muted-foreground">分数 {{ candidate.scoreText }}</span>
                  <small class="block text-xs leading-relaxed text-muted-foreground">{{ candidate.reason || '接口未记录候选理由' }}</small>
                </div>
              </article>
            </div>
          </section>

          <div class="mb-5 grid grid-cols-2 gap-3 max-[860px]:grid-cols-1">
            <section v-for="group in candidateGroups" :key="group.title">
              <div class="mb-2.5 flex items-center justify-between gap-2.5">
                <h5 class="m-0 text-compact font-semibold text-foreground">{{ group.title }}</h5>
                <span class="text-xs text-muted-foreground">{{ group.count }} 个</span>
              </div>
              <div v-if="group.items.length" class="flex flex-wrap gap-1.5">
              <span v-for="(c, i) in group.items" :key="i" class="inline-flex rounded-md bg-secondary px-2.5 py-1 text-xs text-foreground">{{ c.name }} · {{ c.scoreText }}</span>
              </div>
              <p v-else class="text-xs text-muted-foreground">{{ group.empty }}</p>
            </section>
          </div>

          <details class="border-t border-border pt-3">
            <summary class="cursor-pointer text-compact font-semibold text-primary">查看原始 JSON</summary>
            <div class="mt-3 grid grid-cols-3 gap-2.5 max-[860px]:grid-cols-1">
              <pre v-for="json in rawJsonBlocks" :key="json" class="m-0 overflow-auto rounded-lg bg-code p-3 text-xs text-code-foreground">{{ formatJson(json) }}</pre>
            </div>
          </details>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { manageApi } from '../../api/api'
import { formatDateTime } from './observabilityHelpers'
import { buildTopDocumentDistribution, normalizeRouteTrace, summarizeRouteTraceRecords } from '../../utils/knowledgeRoute'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { resolveRouteModeBadgeClass } from '@/components/system/toneClasses'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'

const loading = ref(false)
const records = ref([])
const selectedId = ref(null)
const insightCollapsed = ref(true)
const workspacePane = ref('list')
const pageError = ref('')
const filters = reactive({ conversationId: '', mode: '__all__', routeStatus: '__all__' })
const page = reactive({ pageNo: '1', pageSize: '20', totalSize: '0', totalPages: '0' })
let requestSequence = 0

const normalizedRecords = computed(() => (records.value || []).map((item) => normalizeRouteTrace(item)))
const selectedRecord = computed(() => normalizedRecords.value.find((r) => r.id === selectedId.value) || null)
const traceStats = computed(() => summarizeRouteTraceRecords(records.value || []))
const topDocumentDistribution = computed(() => buildTopDocumentDistribution(records.value || []))

const pageStyle = computed(() => ({
  height: 'calc(100vh - 52px - 40px)',
  overflow: 'hidden'
}))

const headerStats = computed(() => [
  { label: '总追踪量', value: page.totalSize || '0' },
  { label: '成功率', value: traceStats.value.successRateText },
  { label: '平均置信度', value: traceStats.value.averageConfidenceText },
  { label: 'shadow 命中率', value: traceStats.value.shadowHitRateText },
  { label: '低置信或失败', value: String(traceStats.value.lowConfidenceCount + traceStats.value.failedCount) }
])

const summaryCards = computed(() => [
  { label: '总追踪量', value: page.totalSize || '0' },
  { label: '本页 auto', value: String(traceStats.value.autoCount) },
  { label: '本页 shadow', value: String(traceStats.value.shadowCount) },
  { label: '置信度已记录', value: String(normalizedRecords.value.filter((item) => item.confidenceNumber != null).length) },
  { label: '低置信或失败', value: String(traceStats.value.lowConfidenceCount + traceStats.value.failedCount) },
  { label: 'shadow Top3 命中率', value: traceStats.value.shadowHitRateText },
  { label: '平均置信度', value: traceStats.value.averageConfidenceText },
  { label: '成功率', value: traceStats.value.successRateText },
  { label: '低置信率', value: traceStats.value.lowConfidenceRateText },
  { label: '均候选文档', value: traceStats.value.averageDocumentCountText },
  { label: '扩范围次数', value: String(traceStats.value.widenedCount) }
])

const routeHealthCards = computed(() => {
  const widenedRate = traceStats.value.total
    ? `${((traceStats.value.widenedCount / traceStats.value.total) * 100).toFixed(1)}%`
    : '-'
  return [
    { label: '成功率', value: traceStats.value.successRateText, percent: normalizePercent(traceStats.value.successRateText), tone: 'success', description: '根据本页 routeStatus 响应值聚合。' },
    { label: '低置信或失败率', value: traceStats.value.lowConfidenceRateText, percent: normalizePercent(traceStats.value.lowConfidenceRateText), tone: 'warning', description: '根据本页 routeStatus 响应值聚合。' },
    { label: '显式扩范围率', value: widenedRate, percent: normalizePercent(widenedRate), tone: 'neutral', description: '仅统计接口显式记录的扩范围事实。' }
  ]
})

const detailSummaryCards = computed(() => {
  if (!selectedRecord.value) return []
  return [
    { label: '已选文档', value: selectedRecord.value.selectedDocumentId || '未记录', desc: selectedRecord.value.hitSelectedDocument === '1' ? '接口记录为 Top3 命中。' : selectedRecord.value.hitSelectedDocument === '0' ? '接口记录为 Top3 未命中。' : '接口未记录命中对照。' },
    { label: '候选规模', value: `${selectedRecord.value.candidateDocumentCount} 文档 / ${selectedRecord.value.candidateTopicCount} 主题 / ${selectedRecord.value.candidateScopeCount} 范围`, desc: '仅展示响应中的候选数组。' },
    { label: '接口说明', value: selectedRecord.value.reasonText || '未记录', desc: '来自路由追踪响应的 errorMsg 或候选 reason。' }
  ]
})

const candidateGroups = computed(() => {
  if (!selectedRecord.value) return []
  return [
    {
      title: '范围候选', count: selectedRecord.value.scopes.length,
      items: selectedRecord.value.scopes.map((c, index) => ({ name: c.scopeName || `范围 ${c.scopeCode || index + 1}`, scoreText: c.scoreText })),
      empty: '当前没有显式范围候选。'
    },
    {
      title: '主题候选', count: selectedRecord.value.topics.length,
      items: selectedRecord.value.topics.map((c, index) => ({ name: c.topicName || `主题 ${c.topicCode || index + 1}`, scoreText: c.scoreText })),
      empty: '当前没有显式主题候选。'
    }
  ]
})

const rawJsonBlocks = computed(() => selectedRecord.value
  ? [selectedRecord.value.topScopesJson, selectedRecord.value.topTopicsJson, selectedRecord.value.topDocumentsJson]
  : []
)

function healthFillClass(tone) {
  if (tone === 'success') return 'block h-full rounded-full bg-[var(--status-success-fg)]'
  if (tone === 'warning') return 'block h-full rounded-full bg-[var(--status-waiting-fg)]'
  return 'block h-full rounded-full bg-primary'
}

function selectRecord(item) {
  selectedId.value = item.id
  workspacePane.value = 'detail'
}

async function loadTraces(nextPage = page.pageNo) {
  const sequence = ++requestSequence
  loading.value = true
  pageError.value = ''
  try {
    const params = { ...filters, pageNo: String(nextPage), pageSize: page.pageSize }
    if (params.mode === '__all__') params.mode = ''
    if (params.routeStatus === '__all__') params.routeStatus = ''
    const data = await manageApi.queryKnowledgeRouteTracePage(params)
    if (sequence !== requestSequence) return
    records.value = data?.records || []
    page.pageNo = data?.pageNo || '1'
    page.pageSize = data?.pageSize || page.pageSize
    page.totalSize = data?.totalSize || '0'
    page.totalPages = data?.totalPages || '0'
    selectedId.value = null
    workspacePane.value = 'list'
  } catch (error) {
    if (sequence !== requestSequence) return
    pageError.value = error?.message || '读取路由追踪失败。'
  } finally {
    if (sequence === requestSequence) loading.value = false
  }
}

function resetFilters() {
  filters.conversationId = ''
  filters.mode = '__all__'
  filters.routeStatus = '__all__'
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
  try { return JSON.stringify(JSON.parse(value), null, 2) } catch { return value }
}

function primaryDocumentText(item) {
  return item.topDocument?.documentName || item.topDocument?.documentId || '未形成显式主候选'
}

// Route trace pages only render response facts. Recommendations belong to the
// backend owner of route/scope/evidence decisions and are intentionally absent.

onMounted(() => loadTraces('1'))
</script>
