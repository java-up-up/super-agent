const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const REQUEST_TIMEOUT = 30000
const ADMIN_TOKEN_KEY = 'super-agent-admin-token'
const ADMIN_USER_KEY = 'super-agent-admin-user'

export class APIError extends Error {
  constructor(message, status, cause) {
    super(message)
    this.name = 'APIError'
    this.status = status
    this.cause = cause
  }
}

function buildApiUrl(path) {
  return API_BASE_URL ? new URL(path, API_BASE_URL).toString() : path
}

function getAdminToken() {
  return window.localStorage.getItem(ADMIN_TOKEN_KEY) || ''
}

function clearAdminAuth() {
  window.localStorage.removeItem(ADMIN_TOKEN_KEY)
  window.localStorage.removeItem(ADMIN_USER_KEY)
}

function buildAuthHeaders(headers = {}) {
  const token = getAdminToken()
  if (!token) {
    return headers
  }
  return {
    Authorization: `Bearer ${token}`,
    ...headers
  }
}

function handleUnauthorized(response) {
  if (response.status !== 401) {
    return
  }
  clearAdminAuth()
  if (window.location.pathname.startsWith('/admin') && window.location.pathname !== '/admin/login') {
    window.location.href = '/admin/login'
  }
}

function stringifyManageValue(value) {
  if (Array.isArray(value)) {
    return value.map((item) => stringifyManageValue(item))
  }

  if (value && typeof value === 'object') {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, stringifyManageValue(item)])
    )
  }

  if (typeof value === 'number' || typeof value === 'bigint') {
    return String(value)
  }

  return value
}

async function parseJsonResponse(response) {
  const rawText = await response.text()
  if (!rawText) {
    return null
  }

  try {
    return JSON.parse(rawText)
  } catch (error) {
    throw new APIError(`无法解析后端响应: ${rawText}`, response.status, error)
  }
}

async function readResponseMessage(response) {
  const rawText = await response.text()
  if (!rawText) {
    return `请求失败，状态码 ${response.status}`
  }

  try {
    const payload = JSON.parse(rawText)
    return payload.message || payload.error || rawText
  } catch {
    return rawText
  }
}

async function requestJson(path, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const response = await fetch(buildApiUrl(path), {
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...buildAuthHeaders(options.headers || {})
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: controller.signal
    })

    if (!response.ok) {
      handleUnauthorized(response)
      throw new APIError(await readResponseMessage(response), response.status)
    }

    if (response.status === 204) {
      return null
    }

    return parseJsonResponse(response)
  } finally {
    clearTimeout(timeoutId)
  }
}

function unwrapApiResponse(payload, fallbackMessage = '请求失败') {
  const code = String(payload?.code ?? '')
  if (code !== '0') {
    throw new APIError(payload?.message || fallbackMessage, Number(payload?.code || 500), payload)
  }
  return payload?.data ?? null
}

async function requestApiEnvelope(path, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const response = await fetch(buildApiUrl(path), {
      method: options.method || 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...buildAuthHeaders(options.headers || {})
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: controller.signal
    })

    if (!response.ok) {
      handleUnauthorized(response)
      throw new APIError(await readResponseMessage(response), response.status)
    }

    const payload = await parseJsonResponse(response)
    return unwrapApiResponse(payload)
  } finally {
    clearTimeout(timeoutId)
  }
}

async function requestMultipartApiEnvelope(path, formData, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const response = await fetch(buildApiUrl(path), {
      method: options.method || 'POST',
      headers: {
        ...buildAuthHeaders(options.headers || {})
      },
      body: formData,
      signal: controller.signal
    })

    if (!response.ok) {
      handleUnauthorized(response)
      throw new APIError(await readResponseMessage(response), response.status)
    }

    const payload = await parseJsonResponse(response)
    return unwrapApiResponse(payload)
  } finally {
    clearTimeout(timeoutId)
  }
}

function extractDownloadFileName(disposition) {
  const text = String(disposition || '')
  const encodedMatch = text.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    try {
      return decodeURIComponent(encodedMatch[1])
    } catch {
      return encodedMatch[1]
    }
  }
  const plainMatch = text.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || ''
}

async function requestBlob(path, options = {}) {
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    const response = await fetch(buildApiUrl(path), {
      method: options.method || 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...buildAuthHeaders(options.headers || {})
      },
      body: options.body ? JSON.stringify(options.body) : undefined,
      signal: controller.signal
    })

    if (!response.ok) {
      handleUnauthorized(response)
      throw new APIError(await readResponseMessage(response), response.status)
    }

    const blob = await response.blob()
    return {
      blob,
      fileName: extractDownloadFileName(response.headers.get('content-disposition')),
      contentType: response.headers.get('content-type') || blob.type || 'application/octet-stream'
    }
  } finally {
    clearTimeout(timeoutId)
  }
}

function dispatchStreamPayload(rawPayload, handlers) {
  if (!rawPayload) {
    return
  }

  const payload = rawPayload.trim()
  if (!payload || payload === '[DONE]') {
    return
  }

  try {
    handlers.onEvent?.(JSON.parse(payload))
  } catch (error) {
    throw new APIError(`无法解析后端流式事件: ${payload}`, 500, error)
  }
}

function consumeEventBlock(block, handlers) {
  const normalizedBlock = block.trim()
  if (!normalizedBlock) {
    return
  }

  if (normalizedBlock.startsWith('data:')) {
    const payload = normalizedBlock
      .split(/\r?\n/)
      .filter((line) => line.startsWith('data:'))
      .map((line) => line.slice(5).trimStart())
      .join('\n')
    dispatchStreamPayload(payload, handlers)
    return
  }

  normalizedBlock
    .split(/\r?\n/)
    .filter(Boolean)
    .forEach((line) => dispatchStreamPayload(line, handlers))
}

async function consumeEventStream(stream, handlers) {
  const reader = stream.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value || new Uint8Array(), { stream: !done })

    let boundaryIndex = buffer.search(/\r?\n\r?\n/)
    while (boundaryIndex !== -1) {
      const block = buffer.slice(0, boundaryIndex)
      const separatorMatch = buffer.slice(boundaryIndex).match(/^\r?\n\r?\n/)
      const separatorLength = separatorMatch ? separatorMatch[0].length : 2
      buffer = buffer.slice(boundaryIndex + separatorLength)
      consumeEventBlock(block, handlers)
      boundaryIndex = buffer.search(/\r?\n\r?\n/)
    }

    if (done) {
      const tail = decoder.decode()
      if (tail) {
        buffer += tail
      }
      if (buffer.trim()) {
        consumeEventBlock(buffer, handlers)
      }
      return
    }
  }
}

export function createConversationId() {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
}

function normalizePageString(value, fallbackValue) {
  const normalized = String(value ?? fallbackValue).trim()
  return normalized || String(fallbackValue)
}

export const chatApi = {
  listKnowledgeDocumentOptions() {
    return requestApiEnvelope('/api/chat/document/options', {
      method: 'POST',
      body: {}
    })
  },

  listSessions(query = {}) {
    return chatApi.listSessionsPage({
      keyword: query.keyword || '',
      chatMode: query.chatMode || 'ALL',
      turnStatus: query.turnStatus || 'ALL',
      pageNo: normalizePageString(query.pageNo, '1'),
      pageSize: normalizePageString(query.pageSize, '200')
    }).then((data) => data?.sessions || [])
  },

  listSessionsPage(query = {}) {
    // 会话列表统一支持分页查询，分页参数显式使用字符串，
    // 避免前端在 JSON 往返时引入不必要的数值精度风险。
    return requestApiEnvelope('/api/chat/session/list', {
      method: 'POST',
      body: {
        keyword: String(query.keyword || '').trim(),
        chatMode: String(query.chatMode || 'ALL').trim(),
        turnStatus: String(query.turnStatus || 'ALL').trim(),
        pageNo: normalizePageString(query.pageNo, '1'),
        pageSize: normalizePageString(query.pageSize, '20')
      }
    }).then((data) => ({
      pageNo: data?.pageNo || '1',
      pageSize: data?.pageSize || '20',
      totalSize: data?.totalSize || '0',
      totalPages: data?.totalPages || '0',
      sessions: data?.sessions || []
    }))
  },

  getSession(conversationId) {
    // 详情查询也统一改成 body 传 conversationId，
    // 避免前后端同时维护 path 参数和 JSON 参数两套交互风格。
    return requestApiEnvelope('/api/chat/session/detail', {
      method: 'POST',
      body: {
        conversationId
      }
    })
  },

  getExchangeDetail(conversationId, exchangeId) {
    return requestApiEnvelope('/api/chat/exchange/detail', {
      method: 'POST',
      body: {
        conversationId,
        exchangeId: String(exchangeId)
      }
    })
  },

  deleteSession(conversationId) {
    // 页面按钮文案仍然叫“删除会话”，
    // 但后端实际执行的是 reset：会收口运行中任务、清理业务记录和 Graph checkpoint。
    return requestApiEnvelope('/api/chat/session/reset', {
      method: 'POST',
      body: {
        conversationId
      }
    })
  },

  stopSession(conversationId) {
    // stop 单独保留成动作接口，
    // 这样流式生成中的“停止”与会话彻底“删除/重置”在后端语义上是两条不同链路。
    return requestApiEnvelope('/api/chat/session/stop', {
      method: 'POST',
      body: {
        conversationId
      }
    })
  },

  rebuildConversationSummary(conversationId) {
    // 管理侧允许手动触发长期摘要重建，
    // 这样教学演示或排查时，不必等下一轮对话触发”顺手更新”。
    return requestApiEnvelope('/api/chat/session/summary/rebuild', {
      method: 'POST',
      body: {
        conversationId
      }
    })
  },

  getRetrievalResults(conversationId, exchangeId) {
    return requestApiEnvelope('/api/chat/exchange/retrieval/results', {
      method: 'POST',
      body: {
        conversationId,
        exchangeId: String(exchangeId)
      }
    })
  },

  getChannelExecutions(conversationId, exchangeId) {
    return requestApiEnvelope('/api/chat/exchange/channel/executions', {
      method: 'POST',
      body: {
        conversationId,
        exchangeId: String(exchangeId)
      }
    })
  },

  getStageBenchmarks() {
    return requestApiEnvelope('/api/chat/stage/benchmarks', {
      method: 'POST',
      body: {}
    })
  },

  openStream(payload, handlers = {}) {
    const controller = new AbortController()

    const done = (async () => {
      const response = await fetch(buildApiUrl('/api/chat/stream'), {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Accept: 'text/event-stream',
          ...buildAuthHeaders()
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      })

      if (!response.ok) {
        throw new APIError(await readResponseMessage(response), response.status)
      }

      if (!response.body) {
        throw new APIError('当前浏览器不支持流式响应', 500)
      }

      await consumeEventStream(response.body, handlers)
    })()

    return {
      controller,
      done
    }
  }
}

export const adminAuthApi = {
  login(payload) {
    return requestApiEnvelope('/admin/auth/login', {
      method: 'POST',
      body: payload
    })
  },

  logout() {
    return requestApiEnvelope('/admin/auth/logout', {
      method: 'POST',
      body: {}
    })
  },

  currentUser() {
    return requestJson('/admin/auth/me')
      .then((payload) => unwrapApiResponse(payload))
  }
}

export const manageApi = {
  uploadDocument({ file, documentName, operatorId, knowledgeScopeCode, knowledgeScopeName, businessCategory, documentTags }) {
    const formData = new FormData()
    formData.append('file', file)

    const meta = stringifyManageValue({
      documentName: documentName || '',
      operatorId: operatorId ?? '',
      knowledgeScopeCode: knowledgeScopeCode || '',
      knowledgeScopeName: knowledgeScopeName || '',
      businessCategory: businessCategory || '',
      documentTags: documentTags || ''
    })
    formData.append('meta', new Blob([JSON.stringify(meta)], { type: 'application/json' }))

    return requestMultipartApiEnvelope('/manage/document/upload', formData)
  },

  queryDocumentPage(payload) {
    return requestApiEnvelope('/manage/document/page/query', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryDocumentDetail(documentId) {
    return requestApiEnvelope('/manage/document/detail/query', {
      method: 'POST',
      body: stringifyManageValue({
        documentId
      })
    })
  },

  deleteDocument(payload) {
    return requestApiEnvelope('/manage/document/delete', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryStrategyPlan(documentId) {
    return requestApiEnvelope('/manage/document/strategy/plan/query', {
      method: 'POST',
      body: stringifyManageValue({
        documentId
      })
    })
  },

  confirmStrategy(payload) {
    return requestApiEnvelope('/manage/document/strategy/confirm', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  buildIndex(payload) {
    return requestApiEnvelope('/manage/document/index/build', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryDocumentChunks(payload) {
    return requestApiEnvelope('/manage/document/chunk/query', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryDocumentChunkDetail(payload) {
    return requestApiEnvelope('/manage/document/chunk/detail/query', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryTaskLogs(payload) {
    return requestApiEnvelope('/manage/document/task/log/query', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  saveKnowledgeScope(payload) {
    return requestApiEnvelope('/manage/knowledge/scope/save', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  deleteKnowledgeScope(payload) {
    return requestApiEnvelope('/manage/knowledge/scope/delete', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  listKnowledgeScopes() {
    return requestApiEnvelope('/manage/knowledge/scope/list', {
      method: 'POST',
      body: {}
    })
  },

  saveKnowledgeTopic(payload) {
    return requestApiEnvelope('/manage/knowledge/topic/save', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  deleteKnowledgeTopic(payload) {
    return requestApiEnvelope('/manage/knowledge/topic/delete', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  listKnowledgeTopics(payload = {}) {
    return requestApiEnvelope('/manage/knowledge/topic/list', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryDocumentProfile(payload) {
    return requestApiEnvelope('/manage/knowledge/document/profile/detail', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  regenerateDocumentProfile(payload) {
    return requestApiEnvelope('/manage/knowledge/document/profile/regenerate', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  batchRegenerateDocumentProfiles(payload) {
    return requestApiEnvelope('/manage/knowledge/document/profile/batch/regenerate', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  listTopicDocuments(payload = {}) {
    return requestApiEnvelope('/manage/knowledge/topic/document/list', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  saveTopicDocumentRelation(payload) {
    return requestApiEnvelope('/manage/knowledge/topic/document/save', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  removeTopicDocumentRelation(payload) {
    return requestApiEnvelope('/manage/knowledge/topic/document/remove', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  },

  queryKnowledgeRouteTracePage(payload = {}) {
    return requestApiEnvelope('/manage/knowledge/route/trace/page/query', {
      method: 'POST',
      body: stringifyManageValue(payload)
    })
  }
}
