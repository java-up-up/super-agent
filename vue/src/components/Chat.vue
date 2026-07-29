<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import DOMPurify from 'dompurify'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import yaml from 'highlight.js/lib/languages/yaml'
import { Marked } from 'marked'
import {
  ArrowPathIcon,
  CheckIcon,
  DocumentDuplicateIcon,
  SparklesIcon
} from '@heroicons/vue/24/outline'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import CitationDisclosure from '@/components/chat/CitationDisclosure.vue'
import { projectSourceReferences } from '@/features/chat/chatBehavior'

const props = defineProps({
  message: { type: Object, required: true },
  isStreaming: { type: Boolean, default: false },
  showRecommendations: { type: Boolean, default: false }
})

const emit = defineEmits(['recommend', 'retry'])
const contentRef = ref(null)
const citationDisclosureRef = ref(null)
const copied = ref(false)

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('yaml', yaml)

const isUser = computed(() => props.message.role === 'user')
const copyButtonTitle = computed(() => copied.value ? '已复制' : '复制内容')
const hasAssistantContent = computed(() => !isUser.value && Boolean(props.message.content))
const showStatusNotice = computed(() => !isUser.value && Boolean(props.message.statusText))
const showErrorNotice = computed(() => !isUser.value && Boolean(props.message.errorMessage))
const showEmptyAssistantHint = computed(() => {
  return !isUser.value && !props.isStreaming && !props.message.content && !showStatusNotice.value && !showErrorNotice.value
})
const sourceReferences = computed(() => projectSourceReferences(props.message.references))
const copyableText = computed(() => {
  if (props.message.content) return props.message.content
  return [props.message.statusText, props.message.errorMessage].filter(Boolean).join('\n')
})
const showRecommendationBar = computed(() => {
  return !isUser.value
    && props.showRecommendations
    && Array.isArray(props.message.recommendations)
    && props.message.recommendations.length > 0
})
const canRetry = computed(() => showErrorNotice.value && Boolean(props.message.question))

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function createMarkdownParser() {
  const parser = new Marked({ breaks: true, gfm: true })
  parser.use({
    extensions: [{
      name: 'citationToken',
      level: 'inline',
      start(source) {
        const match = source.match(/\[[1-9]\d*\](?!\()/)
        return match?.index
      },
      tokenizer(source) {
        const match = /^\[([1-9]\d*)\](?!\()/.exec(source)
        if (!match) return undefined
        return { type: 'citationToken', raw: match[0], index: Number(match[1]) }
      },
      renderer(token) {
        const source = sourceReferences.value.find((item) => item.index === token.index)
        if (!source) return escapeHtml(token.raw)
        return `<button type="button" class="citation-token" data-citation-index="${token.index}" aria-label="查看来源 ${token.index}：${escapeHtml(source.title)}">[${token.index}]</button>`
      }
    }]
  })
  return parser
}

const renderedContent = computed(() => {
  if (!props.message.content) return ''
  const rendered = createMarkdownParser().parse(props.message.content)
  return DOMPurify.sanitize(rendered, {
    ADD_ATTR: ['target', 'rel', 'class', 'data-citation-index', 'aria-label', 'type'],
    FORBID_TAGS: ['style']
  })
})

async function highlightCodeBlocks() {
  await nextTick()
  if (!contentRef.value || isUser.value) return
  contentRef.value.querySelectorAll('pre code').forEach((block) => hljs.highlightElement(block))
}

function handleContentClick(event) {
  const trigger = event.target instanceof Element
    ? event.target.closest('[data-citation-index]')
    : null
  if (!trigger || !contentRef.value?.contains(trigger)) return
  citationDisclosureRef.value?.openReference(Number(trigger.dataset.citationIndex))
}

async function copyContent() {
  try {
    await navigator.clipboard.writeText(copyableText.value || '')
    copied.value = true
    window.setTimeout(() => { copied.value = false }, 1800)
  } catch (error) {
    console.error('复制消息失败', error)
  }
}

function formatTime(value) {
  if (!value) return '刚刚'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'
  return new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}

watch(() => props.message.content, () => { if (!isUser.value) highlightCodeBlocks() })
onMounted(() => { if (!isUser.value) highlightCodeBlocks() })
</script>

<template>
  <article
    class="mx-auto w-full max-w-[920px] px-4 py-5 sm:px-6"
    :class="isUser ? 'flex justify-end' : ''"
    :data-message-role="message.role"
  >
    <div v-if="isUser" class="max-w-[min(42rem,88%)] rounded-lg border border-border bg-secondary px-4 py-3 text-foreground">
      <div class="mb-1 flex items-center justify-between gap-3">
        <span class="text-xs font-medium">你 · {{ formatTime(message.updatedAt || message.createdAt) }}</span>
        <Button variant="ghost" size="icon-sm" class="size-11 sm:size-8" type="button" :title="copyButtonTitle" :aria-label="copyButtonTitle" @click="copyContent">
          <CheckIcon v-if="copied" />
          <DocumentDuplicateIcon v-else />
        </Button>
      </div>
      <p class="whitespace-pre-wrap break-words leading-7">{{ message.content }}</p>
    </div>

    <div v-else class="min-w-0">
      <header class="mb-4 flex items-center justify-between gap-3">
        <div class="flex min-w-0 items-center gap-2.5">
          <span class="grid size-9 flex-none place-items-center rounded-md border border-border bg-card text-primary" aria-hidden="true">
            <SparklesIcon class="size-4" />
          </span>
          <div class="min-w-0">
            <p class="font-semibold text-foreground">智能助手</p>
            <p class="mt-0.5 text-xs text-muted-foreground">{{ formatTime(message.updatedAt || message.createdAt) }}</p>
          </div>
        </div>
        <Button variant="ghost" size="icon-lg" class="size-11 sm:size-9" type="button" :title="copyButtonTitle" :aria-label="copyButtonTitle" @click="copyContent">
          <CheckIcon v-if="copied" />
          <DocumentDuplicateIcon v-else />
        </Button>
      </header>

      <p v-if="showStatusNotice" class="mb-4 border-l-2 border-[var(--status-running-border)] pl-3 text-sm leading-6 text-[var(--status-running-fg)]" role="status">
        {{ message.statusText }}
      </p>

      <Alert v-if="showErrorNotice" variant="destructive" class="mb-4">
        <AlertTitle>回答生成失败</AlertTitle>
        <AlertDescription class="mt-1">{{ message.errorMessage }}</AlertDescription>
        <Button v-if="canRetry" variant="outline" size="sm" class="mt-3" type="button" @click="emit('retry', message.question)">
          <ArrowPathIcon data-icon="inline-start" />
          重新发送
        </Button>
      </Alert>

      <div
        v-if="hasAssistantContent"
        ref="contentRef"
        class="markdown-body max-w-[78ch] break-words text-foreground"
        @click="handleContentClick"
        v-html="renderedContent"
      ></div>
      <p v-else-if="showEmptyAssistantHint" class="border-y border-border py-4 text-sm text-muted-foreground">本次回答没有生成可展示的正文内容。</p>

      <div v-if="isStreaming" class="mt-4 flex items-center gap-2 text-sm text-[var(--status-running-fg)]" role="status" aria-live="polite">
        <span class="stream-cursor" aria-hidden="true"></span>
        正在生成回答
      </div>

      <CitationDisclosure
        ref="citationDisclosureRef"
        :references="message.references"
        :route-explain="message.routeExplain"
      />

      <section v-if="showRecommendationBar" class="mt-5 border-t border-border pt-4" aria-label="推荐追问">
        <p class="mb-2 text-xs font-medium text-muted-foreground">推荐追问</p>
        <div class="flex flex-wrap gap-2">
          <Button
            v-for="(item, index) in message.recommendations"
            :key="`${message.id}-recommend-${index}`"
            variant="outline"
            size="sm"
            class="h-auto min-h-11 whitespace-normal rounded-md px-2.5 py-1.5 text-left leading-5 sm:min-h-8"
            type="button"
            @click="emit('recommend', item)"
          >
            {{ item }}
          </Button>
        </div>
      </section>
    </div>
  </article>
</template>

<style scoped>
.markdown-body {
  font-size: var(--text-body);
  line-height: 1.8;
  overflow-wrap: anywhere;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-block: 1.5em 0.65em;
  color: var(--foreground);
  font-weight: 650;
  line-height: 1.35;
  letter-spacing: 0;
}

.markdown-body :deep(h1) { font-size: var(--text-title); }
.markdown-body :deep(h2) { font-size: var(--text-title-sm); }
.markdown-body :deep(h3) { font-size: var(--text-body); }
.markdown-body :deep(p) { margin-block: 0.85em; }
.markdown-body :deep(p:first-child) { margin-top: 0; }
.markdown-body :deep(p:last-child) { margin-bottom: 0; }
.markdown-body :deep(ul),
.markdown-body :deep(ol) { margin-block: 0.9em; padding-left: 1.5rem; }
.markdown-body :deep(li) { margin-block: 0.35em; }
.markdown-body :deep(blockquote) {
  margin-block: 1rem;
  border-left: 2px solid var(--citation-border);
  padding: 0.25rem 0 0.25rem 1rem;
  color: var(--muted-foreground);
}
.markdown-body :deep(a) {
  color: var(--citation-fg);
  text-decoration: underline;
  text-decoration-color: var(--citation-border);
  text-underline-offset: 3px;
}
.markdown-body :deep(pre) {
  max-width: 100%;
  overflow-x: auto;
  margin-block: 1rem;
  border-radius: var(--radius-token-card);
  background: var(--code);
  color: var(--code-foreground);
  padding: 1rem;
}
.markdown-body :deep(code:not(pre code)) {
  border-radius: var(--radius-token-sm);
  background: var(--neutral-100);
  padding: 0.125rem 0.375rem;
  font-family: var(--font-mono-token);
  font-size: 0.875em;
}
.markdown-body :deep(table) {
  display: block;
  width: max-content;
  max-width: 100%;
  overflow-x: auto;
  margin-block: 1rem;
  border-collapse: collapse;
  font-size: var(--text-body-sm);
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  min-width: 8rem;
  border: 1px solid var(--border);
  padding: 0.625rem 0.75rem;
  text-align: left;
  vertical-align: top;
}
.markdown-body :deep(th) { background: var(--muted); font-weight: 600; }
.markdown-body :deep(.citation-token) {
  border: 0;
  background: transparent;
  color: var(--citation-fg);
  cursor: pointer;
  font: inherit;
  font-weight: 650;
  text-decoration: underline;
  text-decoration-color: var(--citation-border);
  text-underline-offset: 3px;
}
.markdown-body :deep(.citation-token:focus-visible) {
  border-radius: var(--radius-token-sm);
  outline: 3px solid color-mix(in srgb, var(--ring) 40%, transparent);
  outline-offset: 2px;
}

.stream-cursor {
  width: 0.25rem;
  height: 1rem;
  border-radius: var(--radius-token-sm);
  background: var(--status-running-fg);
  animation: chat-pulse 1s infinite;
}

@keyframes chat-pulse {
  0%, 100% { opacity: 0.35; }
  50% { opacity: 1; }
}

@media (prefers-reduced-motion: reduce) {
  .stream-cursor { animation: none; opacity: 1; }
}
</style>
