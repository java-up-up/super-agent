<template>
  <section class="workspace">
    <aside class="sidebar" :class="{ 'sidebar-open': sidebarOpen }">
      <div class="sidebar-header">
        <div>
          <h2>聊天记录</h2>
        </div>
        <button class="icon-button mobile-only" type="button" @click="sidebarOpen = false">
          <XMarkIcon class="icon" />
        </button>
      </div>

      <button class="primary-button new-chat-button" type="button" :disabled="isStreaming" @click="startNewConversation">
        <PlusIcon class="icon" />
        新对话
      </button>

      <div class="session-list">
        <article
          v-for="session in sortedSessions"
          :key="session.conversationId"
          class="session-card"
          :class="{ active: session.conversationId === currentConversationId }"
        >
          <button
            class="session-select"
            type="button"
            :disabled="isStreaming"
            @click="loadConversation(session.conversationId)"
          >
            <div class="session-main">
              <div class="session-title-row">
                <span class="session-title">{{ sessionTitle(session) }}</span>
                <span v-if="session.running" class="running-dot">运行中</span>
              </div>
              <p class="session-preview">{{ sessionPreview(session) }}</p>
              <div class="session-meta">
                <span>{{ formatTime(session.updatedAt) }}</span>
                <span>{{ sessionMessageCount(session) }} 条消息</span>
              </div>
            </div>
          </button>
          <button
            class="icon-button delete-button"
            type="button"
            title="删除会话"
            :disabled="isStreaming"
            @click.stop="deleteConversation(session.conversationId)"
          >
            <TrashIcon class="icon" />
          </button>
        </article>

        <div v-if="!loadingSessions && !sortedSessions.length" class="empty-sidebar">
          <p>还没有历史会话。</p>
          <span>发送第一条消息后，这里会自动出现会话记录。</span>
        </div>
      </div>
    </aside>

    <div v-if="sidebarOpen" class="sidebar-mask" @click="sidebarOpen = false"></div>

    <main class="chat-panel">
      <header class="chat-toolbar">
        <div class="toolbar-left">
          <button class="icon-button mobile-only" type="button" @click="sidebarOpen = true">
            <Bars3Icon class="icon" />
          </button>
          <div>
            <h2>{{ activeSessionTitle }}</h2>
          </div>
        </div>
        <div class="toolbar-actions">
          <a class="admin-entry-button" :href="adminConsoleHref" target="_blank" rel="noopener noreferrer">
            <BuildingOffice2Icon class="icon" />
            管理后台
          </a>
        </div>
      </header>

      <div class="messages-panel" ref="messagesPanelRef">
        <div v-if="pageError" class="notice notice-error">
          {{ pageError }}
        </div>

        <div v-if="loadingConversation" class="notice">
          正在加载会话内容...
        </div>

        <div v-if="!displayMessages.length && !loadingConversation" class="empty-state">
          <div class="empty-icon">
            <SparklesIcon class="icon" />
          </div>
          <h3>让零散问题更快落成可执行方案</h3>
          <p>结合业务问答、文档理解与知识检索，把想法整理成清晰结论和下一步动作</p>
          <div class="prompt-grid">
            <button type="button" class="prompt-chip" @click="sendMessage('请先介绍一下你能帮我做哪些事情，并给出几个典型使用场景')">
              助手能做什么
            </button>
            <button type="button" class="prompt-chip" @click="sendMessage('请帮我把一个复杂问题拆成清晰的分析步骤，并给出执行建议')">
              拆解复杂问题
            </button>
            <button type="button" class="prompt-chip" @click="sendMessage('结合当前项目，帮我梳理对话能力、知识库能力和后台能力之间的关系')">
              梳理项目能力
            </button>
          </div>
        </div>

        <Chat
          v-for="message in displayMessages"
          :key="message.id"
          :message="message"
          :is-streaming="isStreaming && message.id === currentAssistantMessageId"
          :show-recommendations="message.id === latestAssistantDisplayId"
          @recommend="sendMessage"
        />
      </div>

      <footer class="composer-panel">
        <div class="composer-header">
          <span class="composer-tip">按 Enter 发送，Shift + Enter 换行。</span>
          <span v-if="isStreaming" class="streaming-badge">正在生成回答...</span>
        </div>

        <div class="mode-toolbar">
          <span class="scope-label">回答模式</span>
          <div class="mode-switch" role="tablist" aria-label="聊天回答模式">
            <button
              class="mode-button"
              :class="{ active: isDocumentMode }"
              type="button"
              :disabled="isStreaming"
              @click="setChatMode(CHAT_MODES.DOCUMENT)"
            >
              当前文档问答
            </button>
            <button
              class="mode-button"
              :class="{ active: isAutoDocumentMode }"
              type="button"
              :disabled="isStreaming"
              @click="setChatMode(CHAT_MODES.AUTO_DOCUMENT)"
            >
              自动知识问答
            </button>
            <button
              class="mode-button"
              :class="{ active: !isDocumentMode && !isAutoDocumentMode }"
              type="button"
              :disabled="isStreaming"
              @click="setChatMode(CHAT_MODES.OPEN_CHAT)"
            >
              开放式提问
            </button>
          </div>
        </div>

        <div class="scope-toolbar">
          <template v-if="isDocumentMode">
            <span class="scope-label">提问文档</span>
            <select
              v-model="selectedDocumentId"
              class="scope-select"
              :disabled="isStreaming || loadingDocumentOptions"
              @change="handleDocumentScopeChange"
            >
              <option value="">请选择一个文档</option>
              <option
                v-for="item in documentOptions"
                :key="item.documentId"
                :value="item.documentId"
              >
                {{ item.documentName }}
              </option>
            </select>
            <span v-if="selectedDocumentName" class="scope-pill">当前文档：{{ selectedDocumentName }}</span>
            <span v-else-if="!loadingDocumentOptions" class="scope-pill scope-pill-warning">请先选择一个文档再发送问题</span>
          </template>
          <template v-else-if="isAutoDocumentMode">
            <span class="scope-label">知识库使用</span>
            <span class="scope-pill">系统会先自动预选 3-5 份候选文档</span>
            <span class="scope-pill scope-pill-neutral">候选选择只做预选，后续仍走稳定检索链路</span>
            <span v-if="latestAssistantRouteExplain?.topDocument" class="scope-pill">
              最近主候选：{{ latestAssistantRouteExplain.topDocument.documentName || latestAssistantRouteExplain.topDocument.documentId }}
            </span>
          </template>
          <template v-else>
            <span class="scope-label">知识库使用</span>
            <span class="scope-pill scope-pill-neutral">当前不会使用业务知识库文档</span>
          </template>
        </div>

        <div class="composer-input-row">
          <textarea
            ref="composerRef"
            v-model="userInput"
            class="composer-input"
            rows="1"
            :placeholder="composerPlaceholder"
            :disabled="isStreaming"
            @input="resizeComposer"
            @keydown="handleComposerKeydown"
          ></textarea>

          <div class="composer-actions">
            <button
              v-if="isStreaming"
              class="ghost-button"
              type="button"
              :disabled="isStopping"
              @click="stopStreaming"
            >
              <StopIcon class="icon" />
              <span>{{ isStopping ? '停止中...' : '停止生成' }}</span>
            </button>
            <button class="primary-button" type="button" :disabled="isStreaming || !canSend" @click="sendMessage()">
              <PaperAirplaneIcon class="icon" />
              <span>发送</span>
            </button>
          </div>
        </div>
      </footer>
    </main>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bars3Icon,
  BuildingOffice2Icon,
  PaperAirplaneIcon,
  PlusIcon,
  SparklesIcon,
  StopIcon,
  TrashIcon,
  XMarkIcon
} from '@heroicons/vue/24/outline'
import Chat from '../components/Chat.vue'
import { APIError, chatApi, createConversationId, manageApi } from '../api/api'
import { buildChatRouteExplain, buildRouteTraceLookup } from '../utils/knowledgeRoute'

const router = useRouter()
const adminConsoleHref = router.resolve({
  name: 'AdminLogin',
  query: {
    redirect: '/admin/dashboard'
  }
}).href
const composerRef = ref(null)
const messagesPanelRef = ref(null)
const sidebarOpen = ref(false)
const sessions = ref([])
const currentConversationId = ref('')
const displayMessages = ref([])
const userInput = ref('')
const loadingSessions = ref(false)
const loadingConversation = ref(false)
const loadingDocumentOptions = ref(false)
const isStreaming = ref(false)
const isStopping = ref(false)
const pageError = ref('')
const currentStreamHandle = ref(null)
const currentAssistantMessageId = ref('')
const documentOptions = ref([])
const selectedDocumentId = ref('')
const selectedDocumentName = ref('')
const CHAT_MODES = Object.freeze({
  DOCUMENT: 'DOCUMENT',
  AUTO_DOCUMENT: 'AUTO_DOCUMENT',
  OPEN_CHAT: 'OPEN_CHAT'
})
const chatMode = ref(CHAT_MODES.OPEN_CHAT)

const isDocumentMode = computed(() => chatMode.value === CHAT_MODES.DOCUMENT)
const isAutoDocumentMode = computed(() => chatMode.value === CHAT_MODES.AUTO_DOCUMENT)
const canSend = computed(() => {
  if (!userInput.value.trim()) {
    return false
  }
  // 文档问答模式的边界应该在界面层就明确暴露出来：
  // 没选文档时，发送按钮直接禁用，而不是让后端再去“猜”该怎么兜底。
  return !isDocumentMode.value || Boolean(selectedDocumentId.value)
})
const composerPlaceholder = computed(() => {
  if (isAutoDocumentMode.value) {
    return '请输入你的问题，系统会自动选择最相关的知识文档，例如：上线观察与值班规则中观察时长有哪些？'
  }
  return isDocumentMode.value
    ? '请输入关于当前文档的问题，例如：这份培训手册里的试用期规则是怎么规定的？'
    : '请输入你的问题，例如：帮我分析一下这个智能对话方案应该怎么拆分模块。'
})

const sortedSessions = computed(() => {
  return [...sessions.value].sort((left, right) => {
    const leftTime = left.updatedAt ? new Date(left.updatedAt).getTime() : 0
    const rightTime = right.updatedAt ? new Date(right.updatedAt).getTime() : 0
    return rightTime - leftTime
  })
})

const activeSessionTitle = computed(() => {
  const session = sessions.value.find((item) => item.conversationId === currentConversationId.value)
  return session ? sessionTitle(session) : '新的对话'
})
const latestAssistantDisplayId = computed(() => {
  const message = [...displayMessages.value].reverse().find((item) => item.role === 'assistant')
  return message?.id || ''
})
const latestAssistantRouteExplain = computed(() => {
  const message = [...displayMessages.value].reverse().find((item) => item.role === 'assistant' && item.routeExplain)
  return message?.routeExplain || null
})

function sessionTitle(session) {
  const latestUserMessage = session.latestUserMessage || latestExchangeQuestion(session)
  const latestAssistantMessage = session.latestAssistantMessage || latestExchangeAnswer(session)
  return truncate(latestUserMessage || latestAssistantMessage || '新的对话', 22)
}

function sessionPreview(session) {
  const latestAssistantMessage = session.latestAssistantMessage || latestExchangeAnswer(session)
  const latestUserMessage = session.latestUserMessage || latestExchangeQuestion(session)
  return truncate(latestAssistantMessage || latestUserMessage || '还没有消息内容', 48)
}

function sessionMessageCount(session) {
  if (session?.messageCount) {
    return session.messageCount
  }
  return mapExchangesToMessages(session?.exchanges || []).length
}

function latestExchangeQuestion(session) {
  const exchanges = session?.exchanges || []
  for (let index = exchanges.length - 1; index >= 0; index -= 1) {
    const question = exchanges[index]?.question
    if (question) {
      return question
    }
  }
  return ''
}

function latestExchangeAnswer(session) {
  const exchanges = session?.exchanges || []
  for (let index = exchanges.length - 1; index >= 0; index -= 1) {
    const answer = exchanges[index]?.answer
    if (answer) {
      return answer
    }
  }
  return ''
}

function truncate(value, maxLength) {
  if (!value) {
    return ''
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
}

function formatTime(value) {
  if (!value) {
    return '刚刚'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '刚刚'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

function createUserMessage(question) {
  return {
    id: `user-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role: 'user',
    content: question,
    createdAt: new Date().toISOString()
  }
}

function createAssistantMessage() {
  return {
    id: `assistant-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    role: 'assistant',
    content: '',
    thinkingSteps: [],
    references: [],
    recommendations: [],
    usedTools: [],
    status: 'RUNNING',
    statusText: '',
    errorMessage: '',
    firstResponseTimeMs: null,
    totalResponseTimeMs: null,
    debugTrace: null,
    routeExplain: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  }
}

// 后端把每一轮对话结构化成 exchange，这里把 exchange 展开成用户消息 + 助手消息，
// 这样前端展示层就不需要感知数据库 record 的结构细节。
function mapExchangesToMessages(exchanges = [], routeTraceLookup = {}) {
  return exchanges.flatMap((exchange) => {
    const createdAt = exchange.createdAt || exchange.createTime || null
    const updatedAt = exchange.updatedAt || exchange.editTime || createdAt
    const userMessage = {
      id: `exchange-${exchange.exchangeId}-user`,
      role: 'user',
      content: exchange.question || '',
      createdAt
    }

    const assistantMessage = {
      id: `exchange-${exchange.exchangeId}-assistant`,
      role: 'assistant',
      content: exchange.answer || '',
      thinkingSteps: exchange.thinkingSteps || [],
      references: exchange.references || [],
      recommendations: exchange.recommendations || [],
      usedTools: exchange.usedTools || [],
      status: exchange.status || '',
      statusText: '',
      errorMessage: exchange.errorMessage || '',
      firstResponseTimeMs: exchange.firstResponseTimeMs,
      totalResponseTimeMs: exchange.totalResponseTimeMs,
      debugTrace: exchange.debugTrace || null,
      routeExplain: buildChatRouteExplain(routeTraceLookup[String(exchange.exchangeId)]),
      createdAt,
      updatedAt
    }

    return [userMessage, assistantMessage]
  })
}

function upsertSession(session) {
  const index = sessions.value.findIndex((item) => item.conversationId === session.conversationId)
  if (index === -1) {
    sessions.value = [session, ...sessions.value]
    return
  }

  const nextSessions = [...sessions.value]
  nextSessions.splice(index, 1, session)
  sessions.value = nextSessions
}

// SSE 流里拿到的是增量事件，页面需要把它们持续合并进“当前这条助手消息”。
function updateCurrentAssistant(mutator) {
  const index = displayMessages.value.findIndex((message) => message.id === currentAssistantMessageId.value)
  if (index === -1) {
    return
  }

  const nextMessage = {
    ...displayMessages.value[index]
  }
  mutator(nextMessage)

  const nextMessages = [...displayMessages.value]
  nextMessages.splice(index, 1, nextMessage)
  displayMessages.value = nextMessages
}

async function scrollToBottom() {
  await nextTick()
  if (messagesPanelRef.value) {
    messagesPanelRef.value.scrollTop = messagesPanelRef.value.scrollHeight
  }
}

function resizeComposer() {
  nextTick(() => {
    if (!composerRef.value) {
      return
    }
    composerRef.value.style.height = 'auto'
    composerRef.value.style.height = `${Math.min(composerRef.value.scrollHeight, 220)}px`
  })
}

function focusComposer() {
  nextTick(() => {
    composerRef.value?.focus()
    resizeComposer()
  })
}

async function refreshSessions() {
  loadingSessions.value = true

  try {
    const data = await chatApi.listSessions()
    sessions.value = Array.isArray(data) ? data : []
  } catch (error) {
    pageError.value = normalizeError(error, '加载会话列表失败')
  } finally {
    loadingSessions.value = false
  }
}

async function refreshDocumentOptions() {
  loadingDocumentOptions.value = true
  try {
    const data = await chatApi.listKnowledgeDocumentOptions()
    documentOptions.value = Array.isArray(data) ? data : []
    syncSelectedDocumentName()
  } catch (error) {
    pageError.value = normalizeError(error, '加载可选知识文档失败')
  } finally {
    loadingDocumentOptions.value = false
  }
}

async function loadConversation(conversationId) {
  if (!conversationId || isStreaming.value) {
    return
  }

  loadingConversation.value = true
  pageError.value = ''

  try {
    const [sessionResult, routeTraceResult] = await Promise.allSettled([
      chatApi.getSession(conversationId),
      manageApi.queryKnowledgeRouteTracePage({
        conversationId,
        pageNo: '1',
        pageSize: '200'
      })
    ])

    if (sessionResult.status !== 'fulfilled') {
      throw sessionResult.reason
    }

    if (routeTraceResult.status === 'rejected') {
      console.warn('加载知识路由追踪失败', routeTraceResult.reason)
    }

    const session = sessionResult.value
    const routeTraceLookup = routeTraceResult.status === 'fulfilled'
      ? buildRouteTraceLookup(routeTraceResult.value?.records || [])
      : {}

    currentConversationId.value = conversationId
    displayMessages.value = mapExchangesToMessages(session.exchanges || [], routeTraceLookup)
    upsertSession(session)
    applySessionScope(session)
    sidebarOpen.value = false
    await scrollToBottom()
  } catch (error) {
    pageError.value = normalizeError(error, '加载会话详情失败')
  } finally {
    loadingConversation.value = false
  }
}

async function deleteConversation(conversationId) {
  if (!conversationId || isStreaming.value) {
    return
  }

  try {
    await chatApi.deleteSession(conversationId)
    sessions.value = sessions.value.filter((item) => item.conversationId !== conversationId)

    if (currentConversationId.value === conversationId) {
      const nextSession = sortedSessions.value[0]
      if (nextSession) {
        await loadConversation(nextSession.conversationId)
      } else {
        startNewConversation()
      }
    }
  } catch (error) {
    pageError.value = normalizeError(error, '删除会话失败')
  }
}

function startNewConversation() {
  if (isStreaming.value) {
    return
  }

  currentConversationId.value = createConversationId()
  displayMessages.value = []
  userInput.value = ''
  pageError.value = ''
  sidebarOpen.value = false
  syncSelectedDocumentName()
  focusComposer()
}

function applySessionScope(session) {
  // 会话详情回放时，前端要完整恢复“这条会话当时走的是哪一种产品能力”。
  // 这样学习者切回历史会话后，看到的模式开关、文档范围和后端执行结果才是一致的。
  chatMode.value = session?.chatMode || CHAT_MODES.OPEN_CHAT
  selectedDocumentId.value = session?.selectedDocumentId || ''
  selectedDocumentName.value = session?.selectedDocumentName || ''
  syncSelectedDocumentName()
}

function syncSelectedDocumentName() {
  if (!selectedDocumentId.value) {
    selectedDocumentName.value = ''
    return
  }
  const option = documentOptions.value.find((item) => item.documentId === selectedDocumentId.value)
  if (option) {
    selectedDocumentName.value = option.documentName
  }
}

function handleDocumentScopeChange() {
  syncSelectedDocumentName()
  if (isDocumentMode.value && displayMessages.value.length > 0 && !isStreaming.value) {
    startNewConversation()
  }
}

function setChatMode(nextMode) {
  if (isStreaming.value || chatMode.value === nextMode) {
    return
  }
  chatMode.value = nextMode
  pageError.value = ''

  // 模式切换代表“回答边界”已经改变。
  // 为了避免同一个 conversationId 混入两种完全不同的链路，
  // 这里直接起一个新会话，比在老会话里继续缝补更适合教学项目。
  if (displayMessages.value.length > 0) {
    startNewConversation()
  }
}

function handleComposerKeydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 历史会话是完整快照，流式回答是增量事件，这里统一负责把增量事件映射到展示态。
function applyStreamEvent(event) {
  updateCurrentAssistant((message) => {
    if (event.type === 'text') {
      message.content += event.content || ''
    }

    if (event.type === 'thinking' && event.content && !message.thinkingSteps.includes(event.content)) {
      message.thinkingSteps = [...message.thinkingSteps, event.content]
    }

    if (event.type === 'reference') {
      message.references = Array.isArray(event.content) ? event.content : []
    }

    if (event.type === 'recommend') {
      message.recommendations = Array.isArray(event.content) ? event.content : []
    }

    if (event.type === 'status') {
      message.statusText = event.content || ''
    }

    if (event.type === 'error') {
      message.errorMessage = event.content || '对话执行失败'
      message.status = 'FAILED'
    }

    message.updatedAt = event.timestamp || new Date().toISOString()
  })

  scrollToBottom()
}

async function sendMessage(presetQuestion) {
  const question = (presetQuestion || userInput.value).trim()
  if (!question || isStreaming.value) {
    return
  }
  if (isDocumentMode.value && !selectedDocumentId.value) {
    pageError.value = '当前文档问答模式下请先选择一个文档'
    return
  }

  const conversationId = currentConversationId.value || createConversationId()
  const assistantMessage = createAssistantMessage()
  currentConversationId.value = conversationId
  pageError.value = ''

  displayMessages.value = [
    ...displayMessages.value,
    createUserMessage(question),
    assistantMessage
  ]
  currentAssistantMessageId.value = assistantMessage.id
  isStreaming.value = true
  isStopping.value = false

  if (!presetQuestion) {
    userInput.value = ''
    resizeComposer()
  }

  await scrollToBottom()

  const streamHandle = chatApi.openStream(
    {
      question,
      conversationId,
      chatMode: chatMode.value,
      selectedDocumentId: isDocumentMode.value ? selectedDocumentId.value || null : null
    },
    {
      onEvent: applyStreamEvent
    }
  )

  currentStreamHandle.value = streamHandle

  try {
    await streamHandle.done
  } catch (error) {
    if (error.name !== 'AbortError') {
      updateCurrentAssistant((message) => {
        message.errorMessage = normalizeError(error, '流式对话失败')
        message.status = 'FAILED'
      })
      pageError.value = normalizeError(error, '流式对话失败')
    }
  } finally {
    currentStreamHandle.value = null
    currentAssistantMessageId.value = ''
    isStreaming.value = false
    isStopping.value = false

    try {
      await refreshSessions()
      const sessionExists = sessions.value.some((item) => item.conversationId === conversationId)
      if (sessionExists) {
        await loadConversation(conversationId)
      }
    } catch {
      // 这里的错误已经在各自方法里落到页面提示了，不需要再次抛出。
    }
  }
}

async function stopStreaming() {
  if (!isStreaming.value || !currentConversationId.value || !currentStreamHandle.value) {
    return
  }

  isStopping.value = true

  try {
    const result = await chatApi.stopSession(currentConversationId.value)
    updateCurrentAssistant((message) => {
      message.statusText = result?.message || '用户已停止生成'
    })
  } catch (error) {
    pageError.value = normalizeError(error, '停止会话失败')
    isStopping.value = false
    return
  }

  currentStreamHandle.value.controller.abort()
}

function normalizeError(error, fallback) {
  if (error instanceof APIError && error.message) {
    return error.message
  }

  if (error instanceof Error && error.message) {
    return error.message
  }

  return fallback
}

onMounted(async () => {
  await Promise.all([refreshDocumentOptions(), refreshSessions()])

  if (sortedSessions.value.length > 0) {
    await loadConversation(sortedSessions.value[0].conversationId)
  } else {
    startNewConversation()
  }
})
</script>

<style scoped>
.workspace {
  display: grid;
  grid-template-columns: 288px minmax(0, 1fr);
  gap: 0;
  min-height: 100dvh;
  height: 100dvh;
  overflow: hidden;
  background: var(--background);
}

.sidebar,
.chat-panel {
  background: var(--card);
}

.sidebar {
  min-width: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-height: 0;
  border-right: 1px solid var(--border);
}

.sidebar-header,
.chat-toolbar,
.composer-header,
.composer-actions,
.toolbar-left,
.toolbar-actions,
.session-title-row,
.session-meta {
  display: flex;
  align-items: center;
}

.sidebar-header {
  min-height: 64px;
  padding: 0 12px;
  border-bottom: 1px solid var(--border);
}

.new-chat-button {
  width: calc(100% - 24px);
  margin: 12px;
}

.sidebar-header,
.chat-toolbar,
.composer-actions {
  justify-content: space-between;
}

.sidebar h2 {
  margin: 0;
  font-size: 16px;
  color: var(--foreground);
}

.chat-toolbar h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-strong);
}

.session-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
  overflow-y: auto;
  padding: 0 12px 12px;
}

.session-card {
  width: 100%;
  border: 0;
  border-bottom: 1px solid var(--border);
  border-left: 2px solid transparent;
  background: transparent;
  border-radius: 0;
  padding: 8px 6px 8px 8px;
  display: flex;
  gap: 10px;
  text-align: left;
  color: inherit;
  transition: border-color 0.2s ease;
}

.session-card:hover {
  background: color-mix(in srgb, var(--muted) 70%, transparent);
}

.session-card.active {
  border-left-color: var(--primary);
  background: var(--secondary);
}
/* PLACEHOLDER_CHAT_STYLES_PART2 */

.session-select {
  flex: 1;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
}

.session-select:disabled {
  cursor: not-allowed;
}

.session-main {
  min-width: 0;
  flex: 1;
}

.session-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-strong);
}

.running-dot {
  font-size: 12px;
  color: var(--color-primary);
  background: var(--color-primary-soft);
  border-radius: 999px;
  padding: 2px 8px;
}

.session-preview {
  margin: 6px 0 8px;
  color: var(--color-muted);
  font-size: 13px;
  line-height: 1.5;
}

.session-meta {
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--color-muted);
}

.empty-sidebar {
  padding: 40px 16px;
  border: 0;
  border-radius: 0;
  color: var(--color-muted);
  background: transparent;
  text-align: center;
}

.empty-sidebar p {
  margin: 0 0 6px;
  font-weight: 600;
  color: var(--color-text-strong);
}

.chat-panel {
  min-width: 0;
  min-height: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toolbar-left,
.toolbar-actions {
  gap: 12px;
}

.chat-toolbar {
  min-height: 64px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border);
}
/* PLACEHOLDER_CHAT_STYLES_PART3 */

.messages-panel {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px;
  border: none;
  border-radius: 0;
  background: var(--background);
}

.messages-panel > :deep(.message-card),
.messages-panel > :deep(.notice),
.messages-panel > .notice,
.messages-panel > .empty-state {
  width: min(920px, 100%);
  margin-inline: auto;
}

.notice {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-soft);
  border: 1px solid rgba(178, 31, 196, 0.1);
  color: var(--color-primary);
}

.notice-error {
  background: rgba(177, 47, 38, 0.08);
  border-color: rgba(177, 47, 38, 0.14);
  color: var(--color-danger);
}

.empty-state {
  min-height: 100%;
  display: grid;
  place-items: center;
  text-align: center;
  padding: 56px 24px;
  border-radius: var(--radius-md);
  background: transparent;
  border: 0;
}

.empty-state h3 {
  max-width: 720px;
  margin: 16px 0 8px;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 600;
  color: var(--color-text-strong);
}

.empty-state p {
  max-width: 620px;
  margin: 0 auto;
  color: var(--color-muted);
  line-height: 1.7;
}

.empty-icon {
  width: 56px;
  height: 56px;
  margin: 0 auto 8px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
}

.empty-icon .icon {
  width: 28px;
  height: 28px;
  color: var(--color-primary);
}

.prompt-grid {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

.prompt-chip {
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 500;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.prompt-chip:hover {
  border-color: rgba(178, 31, 196, 0.2);
  background: var(--color-surface-soft);
}
/* PLACEHOLDER_CHAT_STYLES_PART4 */

.composer-panel {
  flex: none;
  padding: 12px 20px max(12px, env(safe-area-inset-bottom));
  border-top: 1px solid var(--border);
  background: var(--card);
}

.composer-panel > * {
  width: min(920px, 100%);
  margin-inline: auto;
}

.composer-header {
  justify-content: space-between;
  margin-bottom: 10px;
}

.mode-toolbar,
.scope-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mode-toolbar {
  margin-bottom: 10px;
}

.scope-toolbar {
  margin-bottom: 12px;
}

.scope-label {
  font-size: 13px;
  color: var(--color-text-muted);
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  border-radius: 999px;
  background: var(--muted);
}

.mode-button {
  border: 0;
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-muted);
  background: transparent;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.mode-button.active {
  background: var(--card);
  color: var(--color-primary);
  box-shadow: var(--shadow-sm);
}

.mode-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.scope-select {
  min-width: 240px;
  max-width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  font-size: 14px;
  color: var(--color-text-strong);
  background: var(--card);
}

.scope-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(178, 31, 196, 0.08);
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 500;
}

.scope-pill-neutral {
  background: var(--color-surface-soft);
  color: var(--color-text-muted);
}

.scope-pill-warning {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.composer-tip,
.streaming-badge {
  font-size: 13px;
  color: var(--color-muted);
}

.streaming-badge {
  color: var(--color-primary);
  font-weight: 600;
}

.composer-input-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 8px;
}

.composer-input {
  width: 100%;
  min-height: 44px;
  max-height: 220px;
  resize: none;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  background: #fff;
  color: var(--color-text);
  outline: none;
}

.composer-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.composer-input:disabled {
  background: var(--color-surface-soft);
}

.composer-actions {
  gap: 10px;
  margin-top: 0;
  justify-content: flex-end;
  flex-direction: column;
}

.primary-button,
.ghost-button,
.icon-button {
  border: 1px solid transparent;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  transition: opacity 0.2s ease, background 0.2s ease;
}

.primary-button,
.ghost-button {
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-weight: 600;
  font-size: 14px;
}

.primary-button {
  background: var(--primary);
  color: var(--primary-foreground);
}

.primary-button:hover:not(:disabled) {
  opacity: 0.9;
}

.ghost-button {
  background: #fff;
  color: var(--color-text);
  border-color: var(--color-border);
}

.ghost-button:hover:not(:disabled) {
  background: var(--color-surface-soft);
}

.admin-entry-button {
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 10px 16px;
  font-weight: 600;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--foreground);
  background: var(--secondary);
  border-color: var(--border);
  cursor: pointer;
  text-decoration: none;
  transition: opacity 0.2s ease;
}

.admin-entry-button:hover:not(:disabled) {
  opacity: 0.9;
}

.new-chat-button {
  justify-content: center;
}

.icon-button {
  width: 36px;
  height: 36px;
  justify-content: center;
  border-radius: var(--radius-sm);
  background: #fff;
  border-color: var(--color-border);
  color: var(--color-text);
}

.icon-button:hover:not(:disabled) {
  background: var(--color-surface-soft);
}

.delete-button {
  width: 32px;
  height: 32px;
  background: rgba(177, 47, 38, 0.08);
  border-color: rgba(177, 47, 38, 0.12);
  color: var(--color-danger);
  flex: none;
}

.primary-button:disabled,
.ghost-button:disabled,
.admin-entry-button:disabled,
.icon-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.icon {
  width: 18px;
  height: 18px;
}

.mobile-only,
.sidebar-mask {
  display: none;
}

@media (max-width: 1120px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    width: min(320px, calc(100vw - 32px));
    z-index: 30;
    transform: translateX(-110%);
    transition: transform 0.24s ease;
  }

  .sidebar.sidebar-open {
    transform: translateX(0);
  }

  .sidebar-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: var(--overlay);
    z-index: 20;
  }

  .mobile-only {
    display: inline-flex;
  }
}

@media (max-width: 768px) {
  .chat-panel,
  .sidebar {
    padding: 0;
    border-radius: 0;
  }

  .chat-toolbar,
  .composer-panel {
    padding-inline: 12px;
  }

  .messages-panel {
    padding: 18px 12px;
  }

  .composer-input-row {
    grid-template-columns: minmax(0, 1fr);
  }

  .composer-actions {
    width: 100%;
    flex-direction: row;
  }

  .chat-toolbar,
  .composer-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
  }

  .toolbar-actions > button {
    flex: 1;
    justify-content: center;
  }
}
</style>
