<template>
  <section class="flex flex-col gap-5">
    <PageHeader title="对话观测" description="按问题摘要、最近状态、消息规模和更新时间定位会话，再按需进入完整执行链路。">
      <template #actions>
        <Button size="lg" class="rounded-md" type="button" :loading="refreshing" loading-text="刷新中" @click="loadSessions">
          <ArrowPathIcon v-if="!refreshing" data-icon="inline-start" aria-hidden="true" />
          刷新会话列表
        </Button>
      </template>
    </PageHeader>

    <section class="glass-card glass-edge overflow-hidden rounded-glass border" aria-label="当前会话统计">
      <div class="grid grid-cols-2 divide-x divide-y divide-border sm:grid-cols-4 sm:divide-y-0">
        <div v-for="item in summaryStats" :key="item.label" class="px-4 py-3" :title="item.description">
          <span class="block text-caption text-muted-foreground">{{ item.label }}</span>
          <strong class="mt-1 block text-title-sm font-semibold tabular-nums text-foreground">{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <FilterToolbar>
      <div class="flex min-w-[18rem] flex-1 flex-col gap-1.5 max-sm:w-full max-sm:min-w-0 max-sm:basis-full">
        <Input id="session-search" v-model.trim="keyword" type="search" aria-label="搜索会话" placeholder="会话 ID、文档名、问题或回答" @keydown.enter.prevent="applyFilters" />
      </div>

      <div class="flex min-w-[9rem] flex-col gap-1.5 max-sm:min-w-0 max-sm:flex-1">
        <span class="text-caption text-muted-foreground">提问模式</span>
        <Select v-model="modeFilter">
          <SelectTrigger aria-label="按提问模式筛选会话"><SelectValue placeholder="全部模式" /></SelectTrigger>
          <SelectContent>
            <SelectGroup>
              <SelectItem value="ALL">全部模式</SelectItem>
              <SelectItem value="DOCUMENT">当前文档问答</SelectItem>
              <SelectItem value="AUTO_DOCUMENT">自动知识问答</SelectItem>
              <SelectItem value="OPEN_CHAT">开放式提问</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>

      <div class="flex min-w-[8rem] flex-col gap-1.5 max-sm:min-w-0 max-sm:flex-1">
        <span class="text-caption text-muted-foreground">最近状态</span>
        <Select v-model="statusFilter">
          <SelectTrigger aria-label="按最近状态筛选会话"><SelectValue placeholder="全部状态" /></SelectTrigger>
          <SelectContent>
            <SelectGroup>
              <SelectItem value="ALL">全部状态</SelectItem>
              <SelectItem value="RUNNING">进行中</SelectItem>
              <SelectItem value="COMPLETED">已完成</SelectItem>
              <SelectItem value="FAILED">失败</SelectItem>
              <SelectItem value="STOPPED">已停止</SelectItem>
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>

      <template #actions>
        <Button variant="ghost" size="sm" class="rounded-md" type="button" :disabled="loadingSessions" @click="resetFilters">重置</Button>
        <Button variant="outline" size="sm" class="rounded-md" type="button" :disabled="loadingSessions" @click="applyFilters">筛选</Button>
      </template>
    </FilterToolbar>

    <div v-if="pageError && sessions.length" class="glass-card rounded-md border px-4 py-3 text-body-sm text-foreground" role="status">
      {{ pageError }}；已保留上一次成功的列表。
    </div>

    <AsyncState v-if="initialLoading" state="loading" title="正在加载会话" description="会话摘要与最近轮次状态正在读取。" />
    <AsyncState v-else-if="pageError && !sessions.length" state="error" title="会话列表加载失败" :description="pageError">
      <template #action><Button variant="outline" size="sm" class="rounded-md" type="button" @click="loadSessions">重新加载</Button></template>
    </AsyncState>
    <AsyncState v-else-if="!sessions.length" :state="hasActiveFilters ? 'filtered' : 'empty'" :title="hasActiveFilters ? '没有匹配会话' : '还没有会话'" :description="hasActiveFilters ? '调整筛选条件后再试。' : '去学习端发起一轮对话后，这里会出现可观测链路。'" />

    <template v-else>
      <DataTableShell class="hidden md:block" :busy="refreshing" caption="对话观测会话列表">
        <thead>
          <tr class="bg-muted">
            <th scope="col" class="w-[42%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">问题与会话</th>
            <th scope="col" class="w-[14%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">状态</th>
            <th scope="col" class="w-[15%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">模式 / 规模</th>
            <th scope="col" class="w-[15%] border-b border-border px-4 py-3 text-left text-caption font-semibold text-muted-foreground">更新时间</th>
            <th scope="col" class="w-[14%] border-b border-border px-4 py-3 text-right text-caption font-semibold text-muted-foreground">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="session in sessions" :key="session.conversationId" class="border-b border-border last:border-0 hover:bg-muted/60">
            <td class="px-4 py-3 align-top">
              <div class="block min-w-0" data-session-summary>
                <strong class="block truncate text-body-sm font-semibold text-foreground">{{ sessionTitle(session) }}</strong>
                <span class="mt-1 block line-clamp-2 text-caption leading-relaxed text-muted-foreground">{{ sessionPreview(session) }}</span>
                <code class="mt-1.5 block truncate font-mono text-micro text-muted-foreground">{{ session.conversationId }}</code>
              </div>
              <p v-if="session.latestTurnErrorMessage" class="mt-2 line-clamp-2 text-caption leading-relaxed text-destructive">最近异常：{{ truncate(session.latestTurnErrorMessage, 88) }}</p>
            </td>
            <td class="px-4 py-3 align-top"><StatusBadge v-bind="resolveSessionStatus(session)" /></td>
            <td class="px-4 py-3 align-top">
              <Badge variant="secondary">{{ formatChatMode(session.chatMode) }}</Badge>
              <span class="mt-1.5 block text-caption tabular-nums text-muted-foreground">{{ sessionMessageCount(session) }} 条消息</span>
            </td>
            <td class="px-4 py-3 align-top text-body-sm tabular-nums text-foreground">{{ formatTime(session.updatedAt) }}</td>
            <td class="px-4 py-3 text-right align-top">
              <div class="inline-flex flex-col items-end gap-2" data-session-actions>
                <Button variant="outline" size="sm" class="rounded-md" type="button" @click="openSession(session)">查看会话</Button>
                <Button v-if="session.latestExchangeId" variant="secondary" size="sm" class="rounded-md" type="button" @click="openExchange(session)">{{ exchangeLinkLabel(session) }}</Button>
              </div>
            </td>
          </tr>
        </tbody>
      </DataTableShell>

      <div class="divide-y divide-border border-y border-border md:hidden" aria-label="移动端会话列表">
        <article v-for="session in sessions" :key="session.conversationId" class="py-4">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0 flex-1" data-session-summary>
              <h2 class="m-0 break-words text-body-sm font-semibold text-foreground">{{ sessionTitle(session) }}</h2>
              <p class="mt-1 line-clamp-2 text-caption leading-relaxed text-muted-foreground">{{ sessionPreview(session) }}</p>
            </div>
            <StatusBadge v-bind="resolveSessionStatus(session)" />
          </div>
          <dl class="mt-3 grid grid-cols-2 gap-3 text-caption">
            <div><dt class="text-muted-foreground">模式</dt><dd class="mt-0.5 text-foreground">{{ formatChatMode(session.chatMode) }}</dd></div>
            <div><dt class="text-muted-foreground">活动</dt><dd class="mt-0.5 tabular-nums text-foreground">{{ sessionMessageCount(session) }} 条消息</dd></div>
            <div class="col-span-2"><dt class="text-muted-foreground">更新</dt><dd class="mt-0.5 tabular-nums text-foreground">{{ formatTime(session.updatedAt) }}</dd></div>
          </dl>
          <p v-if="session.latestTurnErrorMessage" class="mt-3 text-caption leading-relaxed text-destructive">最近异常：{{ truncate(session.latestTurnErrorMessage, 88) }}</p>
          <div class="mt-3 flex justify-end gap-2">
            <Button variant="outline" size="lg" class="rounded-md" type="button" @click="openSession(session)">查看会话</Button>
            <Button v-if="session.latestExchangeId" variant="secondary" size="lg" class="rounded-md" type="button" @click="openExchange(session)">{{ exchangeLinkLabel(session) }}</Button>
          </div>
        </article>
      </div>

      <nav v-if="totalPagesCount > 0" class="flex items-center justify-between gap-4 border-t border-border pt-4 max-[980px]:flex-col max-[980px]:items-stretch" aria-label="会话列表分页">
        <div class="text-caption tabular-nums text-muted-foreground">
          <strong class="block text-body-sm text-foreground">第 {{ pageNo }} / {{ totalPages }} 页</strong>
          <span class="mt-1 block">共 {{ totalSize }} 条会话记录</span>
        </div>

        <div class="flex items-center gap-3 max-[980px]:flex-col max-[980px]:items-stretch">
          <label class="flex items-center gap-2 text-caption text-muted-foreground max-[980px]:justify-between">
            每页
            <Select v-model="pageSize" @update:model-value="handlePageSizeChange">
              <SelectTrigger aria-label="每页会话数量" class="w-[4.5rem]"><SelectValue /></SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  <SelectItem value="12">12</SelectItem>
                  <SelectItem value="24">24</SelectItem>
                  <SelectItem value="36">36</SelectItem>
                  <SelectItem value="48">48</SelectItem>
                </SelectGroup>
              </SelectContent>
            </Select>
          </label>

          <div class="flex flex-wrap gap-1">
            <Button variant="outline" size="sm" class="min-w-9 rounded-md max-[640px]:h-11" type="button" :disabled="!canPrev" @click="goPrevPage">上一页</Button>
            <Button
              v-for="(item, index) in paginationItems"
              :key="`page-${item}-${index}`"
              variant="outline"
              size="sm"
              class="min-h-8 min-w-9 rounded-md px-2.5 text-caption font-semibold disabled:cursor-default max-[640px]:min-h-11"
              :class="item === pageNo ? 'border-primary bg-selection text-foreground' : item === '...' ? 'border-dashed border-input bg-transparent text-muted-foreground' : 'border-input bg-background text-foreground hover:border-border-strong hover:bg-muted'"
              type="button"
              :disabled="item === '...'"
              :aria-current="item === pageNo ? 'page' : undefined"
              @click="item !== '...' ? goPage(item) : null"
            >{{ item }}</Button>
            <Button variant="outline" size="sm" class="min-w-9 rounded-md max-[640px]:h-11" type="button" :disabled="!canNext" @click="goNextPage">下一页</Button>
          </div>
        </div>
      </nav>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowPathIcon } from '@heroicons/vue/24/outline'
import { chatApi } from '../../api/api'
import {
  formatChatMode,
  formatTime,
  normalizeError,
  sessionMessageCount,
  sessionPreview,
  sessionTitle,
  truncate
} from './observabilityHelpers'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectGroup, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import AsyncState from '@/components/system/AsyncState.vue'
import DataTableShell from '@/components/system/DataTableShell.vue'
import FilterToolbar from '@/components/system/FilterToolbar.vue'
import PageHeader from '@/components/system/PageHeader.vue'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { buildPaginationItems, createLatestRequestGuard, resolveSessionStatus } from '@/features/admin/adminBehavior'

const router = useRouter()
const route = useRoute()
const requestGuard = createLatestRequestGuard()
const sessions = ref([])
const loadingSessions = ref(false)
const listInitialized = ref(false)
const pageError = ref('')
const keyword = ref(String(route.query.listKeyword || ''))
const modeFilter = ref(String(route.query.listMode || 'ALL'))
const statusFilter = ref(String(route.query.listStatus || 'ALL'))
const pageNo = ref(String(route.query.listPage || '1'))
const pageSize = ref(String(route.query.listPageSize || '12'))
const totalSize = ref('0')
const totalPages = ref('0')

const currentPageNumber = computed(() => Number(pageNo.value || '1') || 1)
const totalPagesCount = computed(() => Number(totalPages.value || '0') || 0)
const canPrev = computed(() => currentPageNumber.value > 1)
const canNext = computed(() => totalPagesCount.value > 0 && currentPageNumber.value < totalPagesCount.value)
const initialLoading = computed(() => loadingSessions.value && !listInitialized.value)
const refreshing = computed(() => loadingSessions.value && listInitialized.value)
const hasActiveFilters = computed(() => Boolean(keyword.value || modeFilter.value !== 'ALL' || statusFilter.value !== 'ALL'))

const summaryStats = computed(() => {
  const running = sessions.value.filter((item) => item.running).length
  const documentMode = sessions.value.filter((item) => item.chatMode === 'DOCUMENT').length
  const failed = sessions.value.filter((item) => item.latestTurnStatus === 'FAILED').length
  return [
    { label: '会话总数', value: totalSize.value, description: '当前筛选范围的会话总数' },
    { label: '本页运行中', value: running, description: '当前页正在生成的会话' },
    { label: '本页文档问答', value: documentMode, description: '当前页走 RAG 链路的会话' },
    { label: '本页最近失败', value: failed, description: '当前页最近一轮失败的会话' }
  ]
})

const paginationItems = computed(() => buildPaginationItems(totalPagesCount.value, currentPageNumber.value))

async function loadSessions(options = {}) {
  const requestId = requestGuard.begin()
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
    if (!requestGuard.isCurrent(requestId)) return
    sessions.value = page.sessions || []
    pageNo.value = page.pageNo || '1'
    pageSize.value = page.pageSize || pageSize.value
    totalSize.value = page.totalSize || '0'
    totalPages.value = page.totalPages || '0'
    listInitialized.value = true
  } catch (error) {
    if (!requestGuard.isCurrent(requestId)) return
    pageError.value = normalizeError(error, '加载会话列表失败')
    listInitialized.value = true
  } finally {
    if (requestGuard.isCurrent(requestId)) loadingSessions.value = false
  }
}

function goPage(nextPageNo) {
  if (!nextPageNo || nextPageNo === pageNo.value || loadingSessions.value) return
  loadSessions({ pageNo: String(nextPageNo) })
}

function goPrevPage() { if (canPrev.value) goPage(String(currentPageNumber.value - 1)) }
function goNextPage() { if (canNext.value) goPage(String(currentPageNumber.value + 1)) }
function handlePageSizeChange() { loadSessions({ pageNo: '1', pageSize: pageSize.value }) }
function applyFilters() { loadSessions({ pageNo: '1' }) }

function resetFilters() {
  keyword.value = ''
  modeFilter.value = 'ALL'
  statusFilter.value = 'ALL'
  loadSessions({ keyword: '', chatMode: 'ALL', turnStatus: 'ALL', pageNo: '1' })
}

function detailTarget(session) {
  return { name: 'AdminObservabilitySession', params: { conversationId: session.conversationId }, query: listNavigationQuery() }
}

function exchangeTarget(session) {
  return { name: 'AdminObservabilityExchangeDetail', params: { conversationId: session.conversationId, exchangeId: String(session.latestExchangeId) }, query: listNavigationQuery() }
}

function listNavigationQuery() {
  return {
    listKeyword: keyword.value || undefined,
    listMode: modeFilter.value !== 'ALL' ? modeFilter.value : undefined,
    listStatus: statusFilter.value !== 'ALL' ? statusFilter.value : undefined,
    listPage: pageNo.value,
    listPageSize: pageSize.value
  }
}

function openSession(session) { router.push(detailTarget(session)) }
function openExchange(session) { router.push(exchangeTarget(session)) }

function exchangeLinkLabel(session) {
  if (session.running) return '当前轮次'
  if (session.latestTurnStatus === 'FAILED' || session.latestTurnStatus === 'STOPPED') return '异常轮次'
  return '最近轮次'
}

onMounted(loadSessions)
</script>
