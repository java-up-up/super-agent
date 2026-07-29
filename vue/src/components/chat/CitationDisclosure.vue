<script setup>
import { computed, ref } from 'vue'
import {
  ArrowLeftIcon,
  ArrowRightIcon,
  ArrowTopRightOnSquareIcon,
  BookOpenIcon,
  CircleStackIcon
} from '@heroicons/vue/24/outline'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import ChildPageDialog from '@/components/system/ChildPageDialog.vue'
import StatusBadge from '@/components/system/StatusBadge.vue'
import { projectSourceReferences } from '@/features/chat/chatBehavior'

const props = defineProps({
  references: { type: Array, default: () => [] },
  routeExplain: { type: Object, default: null }
})

const sources = computed(() => projectSourceReferences(props.references))
const dialogOpen = ref(false)
const detailKind = ref('source')
const selectedIdentity = ref('')

const selectedPosition = computed(() => {
  return Math.max(0, sources.value.findIndex((source) => source.identity === selectedIdentity.value))
})
const selectedSource = computed(() => sources.value[selectedPosition.value] || null)
const dialogTitle = computed(() => {
  if (detailKind.value === 'route') return '本轮检索说明'
  if (!selectedSource.value) return '引用来源'
  return `来源 [${selectedSource.value.index}] · ${selectedSource.value.title}`
})
const dialogDescription = computed(() => detailKind.value === 'route'
  ? '仅展示后端返回的检索范围、候选和状态事实。'
  : '本轮回答中由后端显式绑定的来源详情。')

function routeTone(tone) {
  if (tone === 'success') return 'success'
  if (tone === 'warning') return 'waiting'
  if (tone === 'danger') return 'danger'
  return 'default'
}

function openReference(index) {
  const source = sources.value.find((item) => item.index === Number(index))
  if (!source) return false
  detailKind.value = 'source'
  selectedIdentity.value = source.identity
  dialogOpen.value = true
  return true
}

function openRouteExplain() {
  if (!props.routeExplain) return
  detailKind.value = 'route'
  dialogOpen.value = true
}

function moveSource(offset) {
  const nextPosition = selectedPosition.value + offset
  if (nextPosition < 0 || nextPosition >= sources.value.length) return
  selectedIdentity.value = sources.value[nextPosition].identity
}

function locationText(source) {
  if (!source) return ''
  const parts = []
  if (source.sectionPath) parts.push(source.sectionPath)
  if (source.pageRange) parts.push(`第 ${source.pageRange} 页`)
  else if (source.pageNo) parts.push(`第 ${source.pageNo} 页`)
  if (source.chunkNo) parts.push(`片段 ${source.chunkNo}`)
  return parts.join(' · ')
}

function scoreText(score) {
  return score == null ? '' : score.toFixed(4)
}

function safeHttpUrl(value) {
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : ''
  } catch {
    return ''
  }
}

defineExpose({ openReference })
</script>

<template>
  <section v-if="sources.length || routeExplain" class="mt-5 border-t border-border pt-4" aria-label="回答来源">
    <div class="flex flex-wrap items-center gap-2">
      <span class="mr-1 flex items-center gap-1.5 text-xs font-medium text-muted-foreground">
        <BookOpenIcon class="size-4" aria-hidden="true" />
        回答来源
      </span>
      <Button
        v-for="source in sources"
        :key="source.identity"
        variant="outline"
        size="sm"
        type="button"
        class="min-h-11 rounded-md bg-[var(--citation-bg)] text-[var(--citation-fg)] hover:bg-[var(--citation-bg)]/70 sm:min-h-8"
        :aria-label="`查看来源 ${source.index}：${source.title}`"
        @click="openReference(source.index)"
      >
        [{{ source.index }}] {{ source.title }}
      </Button>
      <Button v-if="routeExplain" variant="ghost" size="sm" type="button" class="min-h-11 rounded-md sm:min-h-8" @click="openRouteExplain">
        <CircleStackIcon data-icon="inline-start" />
        检索说明
      </Button>
    </div>

    <ChildPageDialog
      v-model:open="dialogOpen"
      :title="dialogTitle"
      :description="dialogDescription"
      :size="detailKind === 'route' ? 'lg' : 'default'"
      close-label="关闭来源详情"
    >
      <article v-if="detailKind === 'source' && selectedSource" class="grid gap-5">
        <header class="grid gap-2">
          <div class="flex flex-wrap items-center gap-2">
            <Badge variant="outline" class="bg-[var(--citation-bg)] text-[var(--citation-fg)]">来源 [{{ selectedSource.index }}]</Badge>
            <Badge v-if="selectedSource.evidenceType" variant="secondary">{{ selectedSource.evidenceType }}</Badge>
          </div>
          <h3 class="text-lg font-semibold leading-snug text-foreground">{{ selectedSource.title }}</h3>
          <p v-if="locationText(selectedSource)" class="text-sm text-muted-foreground">{{ locationText(selectedSource) }}</p>
        </header>

        <blockquote
          v-if="selectedSource.snippet"
          class="border-l-2 border-[var(--citation-border)] bg-[var(--citation-bg)] px-4 py-3 text-sm leading-7 text-foreground"
        >
          {{ selectedSource.snippet }}
        </blockquote>
        <p v-else class="border-y border-border py-4 text-sm text-muted-foreground">后端未返回可展示的来源片段。</p>

        <section aria-labelledby="citation-technical-title">
          <h4 id="citation-technical-title" class="mb-3 text-sm font-semibold text-foreground">来源定位</h4>
          <dl class="grid border-t border-border text-sm sm:grid-cols-2">
            <div class="border-b border-border py-3 sm:pr-4">
              <dt class="text-xs text-muted-foreground">稳定 identity</dt>
              <dd class="mt-1 break-all font-mono text-xs text-foreground">{{ selectedSource.identity }}</dd>
            </div>
            <div class="border-b border-border py-3 sm:pl-4">
              <dt class="text-xs text-muted-foreground">来源类型</dt>
              <dd class="mt-1 text-foreground">{{ selectedSource.sourceType || selectedSource.evidenceType || '-' }}</dd>
            </div>
            <div class="border-b border-border py-3 sm:pr-4">
              <dt class="text-xs text-muted-foreground">知识域</dt>
              <dd class="mt-1 text-foreground">{{ selectedSource.knowledgeScopeName || selectedSource.knowledgeScopeCode || '-' }}</dd>
            </div>
            <div class="border-b border-border py-3 sm:pl-4">
              <dt class="text-xs text-muted-foreground">检索通道</dt>
              <dd class="mt-1 text-foreground">{{ selectedSource.channel || '-' }}</dd>
            </div>
            <div class="border-b border-border py-3 sm:pr-4">
              <dt class="text-xs text-muted-foreground">文档 / 片段 ID</dt>
              <dd class="mt-1 font-mono text-xs text-foreground">{{ selectedSource.documentId || '-' }} / {{ selectedSource.chunkId || '-' }}</dd>
            </div>
            <div class="border-b border-border py-3 sm:pl-4">
              <dt class="text-xs text-muted-foreground">后端分数</dt>
              <dd class="mt-1 font-mono text-xs text-foreground">{{ scoreText(selectedSource.score) || '-' }}</dd>
            </div>
          </dl>
        </section>

        <Button
          v-if="safeHttpUrl(selectedSource.url)"
          as="a"
          :href="safeHttpUrl(selectedSource.url)"
          target="_blank"
          rel="noopener noreferrer"
          variant="outline"
          size="lg"
          class="justify-self-start"
        >
          打开原始来源
          <ArrowTopRightOnSquareIcon data-icon="inline-end" />
        </Button>
      </article>

      <article v-else-if="detailKind === 'route' && routeExplain" class="grid gap-5">
        <header class="grid gap-2 border-b border-border pb-4">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <p class="text-sm font-medium text-foreground">{{ routeExplain.modeLabel }}</p>
            <StatusBadge :label="routeExplain.statusLabel" :tone="routeTone(routeExplain.statusTone)" />
          </div>
          <p class="leading-7 text-foreground">{{ routeExplain.summary }}</p>
          <p class="text-sm text-muted-foreground">{{ routeExplain.confidenceBand?.label }} · 置信度 {{ routeExplain.confidenceText }}</p>
        </header>

        <ul v-if="routeExplain.notes?.length" class="grid gap-2 text-sm leading-6 text-foreground">
          <li v-for="(note, index) in routeExplain.notes" :key="index" class="flex gap-2">
            <span class="mt-2 size-1.5 flex-none rounded-full bg-muted-foreground" aria-hidden="true"></span>
            <span>{{ note }}</span>
          </li>
        </ul>

        <section v-if="routeExplain.topDocuments?.length" aria-labelledby="route-candidates-title">
          <h4 id="route-candidates-title" class="mb-2 text-sm font-semibold text-foreground">候选文档</h4>
          <ol class="divide-y divide-border border-y border-border">
            <li v-for="(item, index) in routeExplain.topDocuments" :key="item.documentId || index" class="grid gap-1 py-3 sm:grid-cols-[minmax(0,1fr)_7rem] sm:items-start">
              <div class="min-w-0">
                <p class="font-medium text-foreground">{{ index + 1 }}. {{ item.documentName || item.documentId }}</p>
                <p class="mt-1 text-sm leading-6 text-muted-foreground">{{ item.reason || '后端未返回候选理由' }}</p>
              </div>
              <span class="font-mono text-xs text-muted-foreground sm:text-right">{{ item.scoreText }}</span>
            </li>
          </ol>
        </section>

        <section v-if="routeExplain.scopePreview?.length || routeExplain.topicPreview?.length" class="grid gap-4 sm:grid-cols-2">
          <div v-if="routeExplain.scopePreview?.length">
            <h4 class="mb-2 text-sm font-semibold text-foreground">范围候选</h4>
            <ul class="grid gap-2 text-sm text-muted-foreground">
              <li v-for="(item, index) in routeExplain.scopePreview" :key="item.scopeCode || index">{{ item.scopeName || `范围 ${item.scopeCode || index + 1}` }} · {{ item.scoreText }}</li>
            </ul>
          </div>
          <div v-if="routeExplain.topicPreview?.length">
            <h4 class="mb-2 text-sm font-semibold text-foreground">主题候选</h4>
            <ul class="grid gap-2 text-sm text-muted-foreground">
              <li v-for="(item, index) in routeExplain.topicPreview" :key="item.topicCode || index">{{ item.topicName || `主题 ${item.topicCode || index + 1}` }} · {{ item.scoreText }}</li>
            </ul>
          </div>
        </section>
      </article>

      <template v-if="detailKind === 'source' && sources.length > 1" #footer>
        <div class="flex w-full items-center justify-between gap-3">
          <Button variant="outline" size="lg" type="button" :disabled="selectedPosition === 0" @click="moveSource(-1)">
            <ArrowLeftIcon data-icon="inline-start" />
            上一条
          </Button>
          <span class="text-sm text-muted-foreground">{{ selectedPosition + 1 }} / {{ sources.length }}</span>
          <Button variant="outline" size="lg" type="button" :disabled="selectedPosition === sources.length - 1" @click="moveSource(1)">
            下一条
            <ArrowRightIcon data-icon="inline-end" />
          </Button>
        </div>
      </template>
    </ChildPageDialog>
  </section>
</template>
