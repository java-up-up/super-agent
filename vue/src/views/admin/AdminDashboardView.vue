<template>
  <section class="flex flex-col gap-5">
    <PageHeader title="运营总览">
      <template #actions>
        <Button variant="outline" size="lg" class="rounded-md" type="button" :loading="refreshing" loading-text="刷新中" @click="loadDashboard">
          <ArrowPathIcon v-if="!refreshing" data-icon="inline-start" aria-hidden="true" />
          刷新数据
        </Button>
        <Button size="lg" class="rounded-md" type="button" @click="goDocuments">
          <DocumentArrowUpIcon data-icon="inline-start" aria-hidden="true" />
          前往文档接入
        </Button>
      </template>
    </PageHeader>

    <AsyncState v-if="loading" state="loading" title="正在加载运营数据" description="文档接入和处理状态正在读取。" />

    <AsyncState v-else-if="errorMessage" state="error" title="运营数据加载失败" :description="errorMessage">
      <template #action>
        <Button variant="outline" size="sm" class="rounded-md" type="button" @click="loadDashboard">重新加载</Button>
      </template>
    </AsyncState>

    <template v-else>
      <section class="glass-card glass-edge overflow-hidden rounded-glass border" aria-label="文档处理摘要">
        <div class="grid grid-cols-2 divide-x divide-y divide-border sm:grid-cols-4 sm:divide-y-0">
          <div v-for="metric in metricCards" :key="metric.label" class="min-w-0 px-4 py-3.5 sm:px-5">
            <span class="block text-caption text-muted-foreground">{{ metric.label }}</span>
            <strong class="mt-1 block text-title font-semibold tabular-nums text-foreground">{{ formatCount(metric.value) }}</strong>
            <p class="mt-1 text-micro leading-relaxed text-muted-foreground">{{ metric.hint }}</p>
          </div>
        </div>
      </section>

      <div class="grid grid-cols-[minmax(0,1.02fr)_minmax(22rem,0.98fr)] gap-4 max-[1080px]:grid-cols-1">
        <section class="glass-card glass-edge rounded-glass border p-4 sm:p-5" aria-labelledby="demo-path-title">
          <div class="border-b border-border pb-3">
            <h2 id="demo-path-title" class="m-0 text-title-sm font-semibold text-foreground">建议演示路径</h2>
          </div>

          <ol class="mt-2 divide-y divide-border">
            <li v-for="(step, index) in demoSteps" :key="step.title" class="grid grid-cols-[2rem_minmax(0,1fr)] gap-3 py-3.5">
              <span class="grid size-8 place-items-center rounded-sm border border-border bg-muted text-caption font-semibold tabular-nums text-foreground" aria-hidden="true">
                {{ String(index + 1).padStart(2, '0') }}
              </span>
              <div class="min-w-0 pt-0.5">
                <strong class="block text-body-sm font-semibold text-foreground">{{ step.title }}</strong>
                <p class="mt-1 text-caption leading-relaxed text-muted-foreground">{{ step.description }}</p>
              </div>
            </li>
          </ol>
        </section>

        <section class="glass-card glass-edge rounded-glass border p-4 sm:p-5" aria-labelledby="recent-documents-title">
          <div class="flex items-start justify-between gap-4 border-b border-border pb-3">
            <div>
              <h2 id="recent-documents-title" class="m-0 text-title-sm font-semibold text-foreground">最近接入文档</h2>
              <p class="mt-1 text-caption text-muted-foreground">展示本次接口返回结果中的前 6 条记录</p>
            </div>
            <Button variant="ghost" size="sm" class="rounded-md" type="button" :loading="refreshing" loading-text="刷新中" @click="loadDashboard">刷新</Button>
          </div>

          <AsyncState v-if="!documents.length" state="empty" title="当前还没有文档" description="先前往文档接入页面上传一份资料。" />
          <div v-else class="divide-y divide-border">
            <article v-for="item in documents.slice(0, 6)" :key="item.documentId" class="flex min-w-0 items-center justify-between gap-4 py-3.5 max-sm:items-start">
              <div class="min-w-0 flex-1">
                <strong class="block truncate text-body-sm font-semibold text-foreground">{{ item.documentName || item.originalFileName || '未命名文档' }}</strong>
                <p class="mt-1 truncate text-caption text-muted-foreground">{{ item.originalFileName || '-' }}</p>
              </div>
              <div class="flex shrink-0 flex-wrap justify-end gap-1.5 max-sm:max-w-32">
                <AdminStatusBadge :label="item.parseStatusName" :code="item.parseStatus" type="parse" />
                <AdminStatusBadge :label="item.indexStatusName" :code="item.indexStatus" type="index" />
              </div>
            </article>
          </div>
        </section>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowPathIcon, DocumentArrowUpIcon } from '@heroicons/vue/24/outline'
import { manageApi } from '../../api/api'
import AdminStatusBadge from '../../components/admin/AdminStatusBadge.vue'
import { formatCount, hasCode } from '../../utils/manageFormat'
import { Button } from '@/components/ui/button'
import AsyncState from '@/components/system/AsyncState.vue'
import PageHeader from '@/components/system/PageHeader.vue'

const router = useRouter()
const loading = ref(true)
const refreshing = ref(false)
const initialized = ref(false)
const errorMessage = ref('')
const documents = ref([])
const summary = reactive({
  total: 0,
  parseSuccess: 0,
  strategyConfirmed: 0,
  indexSuccess: 0
})

const metricCards = computed(() => [
  { label: '文档总数', value: summary.total, hint: '已进入管理台的文档记录' },
  { label: '解析成功', value: summary.parseSuccess, hint: '可进入策略确认阶段的文档' },
  { label: '策略已确认', value: summary.strategyConfirmed, hint: '已经形成最终切块链路' },
  { label: '索引完成', value: summary.indexSuccess, hint: '可直接参与 RAG 检索问答' }
])

const demoSteps = [
  { title: '上传文档', description: '通过管理台上传 PDF、Word、Markdown 等普通版支持的文档。' },
  { title: '查看系统推荐策略', description: '根据文档结构与内容长度，查看系统推荐的父块和子块切分策略。' },
  { title: '确认并构建索引', description: '在推荐结果基础上调整策略，再触发普通版的异步索引构建。' },
  { title: '做对话观测', description: '查看当前文档问答、自动知识和开放提问三种模式下的会话执行轨迹。' }
]

async function loadDashboard() {
  if (initialized.value) refreshing.value = true
  else loading.value = true
  errorMessage.value = ''

  try {
    const data = await manageApi.queryDocumentPage({
      pageNo: 1,
      pageSize: 50,
      keyword: ''
    })
    documents.value = Array.isArray(data?.records) ? data.records : []
    summary.total = Number(data?.total || documents.value.length || 0)
    summary.parseSuccess = documents.value.filter((item) => hasCode(item.parseStatus, 3)).length
    summary.strategyConfirmed = documents.value.filter((item) => hasCode(item.strategyStatus, 3)).length
    summary.indexSuccess = documents.value.filter((item) => hasCode(item.indexStatus, 3)).length
    initialized.value = true
  } catch (error) {
    documents.value = []
    summary.total = 0
    summary.parseSuccess = 0
    summary.strategyConfirmed = 0
    summary.indexSuccess = 0
    errorMessage.value = error instanceof Error && error.message ? error.message : '加载后台概览失败'
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function goDocuments() {
  router.push('/admin/documents')
}

onMounted(loadDashboard)
</script>
