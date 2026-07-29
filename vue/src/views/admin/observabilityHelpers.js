import { APIError } from '../../api/api'

const STATUS_LABELS = {
  RUNNING: '进行中',
  COMPLETED: '已完成',
  FAILED: '失败',
  STOPPED: '已停止',
  SKIPPED: '跳过',
  WARNING: '警告'
}

const STATUS_TONES = {
  RUNNING: 'running',
  COMPLETED: 'completed',
  FAILED: 'failed',
  STOPPED: 'stopped',
  SKIPPED: 'idle',
  WARNING: 'warning'
}

const EXECUTION_MODE_LABELS = {
  RETRIEVAL: '文档检索问答',
  REACT_AGENT: 'Agent 自主执行',
  CLARIFICATION: '路由澄清'
}

const RELATION_TYPE_LABELS = {
  FOLLOW_UP: '承接上文追问',
  TOPIC_SWITCH: '切换到新主题',
  FRESH_TOPIC: '独立新问题',
  UNKNOWN: '未识别'
}

const RETRIEVAL_MODE_LABELS = {
  DIRECT_QUERY: '直接检索',
  SECTION_FOCUSED: '定向查章节',
  ANALYTIC_DECOMPOSITION: '拆成多个子问题',
  UNKNOWN: '未识别'
}

const ANSWER_SHAPE_LABELS = {
  LIST: '列表型回答',
  STEPS: '步骤型回答',
  OUTLINE: '提纲型回答',
  COMPARISON: '对比型回答',
  EXPLANATION: '解释型回答',
  JUDGMENT: '判断型回答',
  FACT: '事实型回答',
  UNKNOWN: '未识别'
}

const CHANNEL_LABELS = {
  keyword: '关键词检索',
  vector: '向量检索',
  rerank: '重排精排',
  hybrid: '融合结果',
  'web-search': '网页搜索'
}

const EXECUTION_STATE_LABELS = {
  1: '成功',
  2: '失败',
  3: '超时',
  4: '跳过'
}

const TOOL_LABELS = {
  tavily_search: 'Tavily 联网搜索',
  keyword: '关键词检索通道',
  vector: '向量检索通道',
  rerank: '重排精排',
}

function asList(value) {
  return Array.isArray(value) ? value.filter(Boolean) : []
}

function mapLabel(value, mapping, fallback = '未识别') {
  if (!value) {
    return fallback
  }
  return mapping[value] || value
}

// 命中/过滤原因码 → 中文。前缀兜底：SELECTED_* 归“命中并选入”，FILTERED_* 归“被筛除”。
const REASON_LABELS = {
  SELECTED_BY_RANK: '按综合排名选入',
  SELECTED_TOP_RANK: '排名靠前选入',
  SELECTED_ROUTE_CANDIDATE_RESERVE: '路由候选保留选入',
  SELECTED_SAME_SECTION_BODY: '同章节正文选入',
  SELECTED_STRUCTURE_ANCHOR: '结构锚点选入',
  SELECTED_STRUCTURE_ANCHOR_BODY: '结构锚点正文选入',
  SELECTED_STRUCTURE_DESCENDANT_BODY: '结构下级正文选入',
  SELECTED_STRUCTURE_NAVIGATION_CHILD: '结构导航·子块选入',
  SELECTED_STRUCTURE_NAVIGATION_CURRENT: '结构导航·当前块选入',
  SELECTED_STRUCTURE_NAVIGATION_PARENT: '结构导航·父块选入',
  SELECTED_STRUCTURE_NAVIGATION_SIBLING: '结构导航·同级块选入',
  FILTERED_BY_CANDIDATE_TOP_K: '超出候选数上限被筛除',
  FILTERED_BY_CHANNEL_GATE: '未过通道质量闸门',
  FILTERED_BY_FINAL_TOP_K: '超出最终证据数上限',
  FILTERED_BY_KEYWORD_RELATIVE_SCORE: '关键词相对分过低被筛除',
  FILTERED_BY_RERANK_CANDIDATE_TOP_K: '超出重排候选数上限',
  FILTERED_BY_VECTOR_GATE: '未过向量相似度闸门',
  FILTERED_NOT_APPLICABLE_TO_TARGET_ENTITY: '与目标实体不相关被筛除',
  CONTEXT_ONLY: '仅作上下文·不可引用'
}

function formatReason(value) {
  if (!value) return '未进入最终证据'
  if (REASON_LABELS[value]) return REASON_LABELS[value]
  if (value.startsWith('SELECTED_')) return '已选入'
  if (value.startsWith('FILTERED_')) return '被筛除'
  return value
}

function uniqueStrings(values) {
  const result = []
  const seen = new Set()
  values.filter(Boolean).forEach((item) => {
    if (seen.has(item)) {
      return
    }
    seen.add(item)
    result.push(item)
  })
  return result
}

function formatList(values, fallback = '无') {
  const list = asList(values).map((item) => String(item || '').trim()).filter(Boolean)
  return list.length ? uniqueStrings(list).join('、') : fallback
}

function hasAnyListValue(values) {
  return asList(values).some((item) => item != null && String(item).trim() !== '')
}

function numericValue(value) {
  if (value == null || value === '') {
    return null
  }
  const num = Number(value)
  return Number.isNaN(num) ? null : num
}

function formatCount(value) {
  const num = numericValue(value)
  return num == null ? '0' : String(num)
}

function scoreBarWidth(value) {
  const num = numericValue(value)
  if (num == null || num <= 0) {
    return '0%'
  }
  const normalized = num <= 1 ? num * 100 : Math.min(num, 100)
  return `${Math.max(6, Math.min(normalized, 100))}%`
}

function firstPresent(...values) {
  return values.find((value) => value != null && value !== '')
}

function isTableReference(reference) {
  if (!reference || typeof reference !== 'object') {
    return false
  }
  return Boolean(
    reference.tableId
    || reference.tableNo
    || reference.tableTitle
    || reference.tableOperation
    || hasAnyListValue(reference.tableEvidenceRowNos)
    || hasAnyListValue(reference.tableEvidenceColumnNames)
    || hasAnyListValue(reference.tableEvidenceCellCoordinates)
    || hasAnyListValue(reference.tableEvidenceCellBboxJsons)
  )
}

function formatTableLocation(reference) {
  const parts = []
  if (reference?.pageNo) {
    parts.push(`第 ${reference.pageNo} 页`)
  }
  if (reference?.pageRange) {
    parts.push(reference.pageRange)
  }
  if (reference?.bboxJson) {
    parts.push('表格 bbox')
  }
  return parts.join(' / ') || '无'
}

function formatTableOperation(reference) {
  const parts = []
  if (reference?.tableOperation) {
    parts.push(reference.tableOperation)
  }
  if (reference?.tableMetricColumn) {
    parts.push(`指标 ${reference.tableMetricColumn}`)
  }
  if (reference?.tableGroupByColumn) {
    parts.push(`分组 ${reference.tableGroupByColumn}`)
  }
  return parts.join(' / ') || '无'
}

function formatBboxPresence(reference) {
  const cellBboxes = asList(reference?.tableEvidenceCellBboxJsons).filter((item) => item && String(item).trim())
  const parts = []
  if (reference?.bboxJson) {
    parts.push('表格 bbox')
  }
  if (cellBboxes.length) {
    parts.push(`${cellBboxes.length} 个单元格 bbox`)
  }
  return parts.join('，') || '无'
}

export function buildTableEvidenceItems(references) {
  return asList(references)
    .filter(isTableReference)
    .map((reference, index) => ({
      key: `${reference.referenceId || index}-${reference.tableId || reference.tableNo || index}`,
      referenceId: reference.referenceId || '-',
      documentId: reference.documentId || '',
      documentName: reference.documentName || reference.title || '未命名文档',
      channel: formatChannelName(reference.channel || 'table'),
      tableTitle: reference.tableTitle || (reference.tableNo ? `表格 T#${reference.tableNo}` : '未命名表格'),
      tableId: reference.tableId || '',
      tableNo: reference.tableNo || '',
      operationText: formatTableOperation(reference),
      matchedRowCount: reference.tableMatchedRowCount ?? '',
      rowNos: asList(reference.tableEvidenceRowNos).filter((item) => item != null && String(item).trim() !== ''),
      columnNames: asList(reference.tableEvidenceColumnNames).filter((item) => item != null && String(item).trim() !== ''),
      cellCoordinates: asList(reference.tableEvidenceCellCoordinates).filter((item) => item != null && String(item).trim() !== ''),
      rowsText: formatList(reference.tableEvidenceRowNos),
      columnsText: formatList(reference.tableEvidenceColumnNames),
      cellsText: formatList(reference.tableEvidenceCellCoordinates),
      locationText: formatTableLocation(reference),
      bboxText: formatBboxPresence(reference),
      tableBboxJson: reference.bboxJson || '',
      cellBboxJsons: asList(reference.tableEvidenceCellBboxJsons).filter((item) => item && String(item).trim()),
      sectionPath: reference.sectionPath || '',
      snippet: reference.quoteText || reference.snippet || '',
      citationScore: reference.citationScore
    }))
}

export function normalizeError(error, fallbackMessage) {
  if (error instanceof APIError && error.message) {
    return error.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallbackMessage
}

export function truncate(value, maxLength) {
  if (!value) {
    return ''
  }
  return value.length > maxLength ? `${value.slice(0, maxLength)}...` : value
}

export function formatTime(value) {
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

export function formatDateTime(value) {
  if (!value) {
    return '无'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '无'
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date)
}

export function formatChatMode(value) {
  if (value === 'DOCUMENT') {
    return '当前文档问答'
  }
  if (value === 'AUTO_DOCUMENT') {
    return '自动知识问答'
  }
  if (value === 'OPEN_CHAT') {
    return '开放式提问'
  }
  return value || '未知模式'
}

export function formatStatusLabel(value) {
  return STATUS_LABELS[value] || value || '未知状态'
}

export function statusTone(value) {
  return STATUS_TONES[value] || 'idle'
}

export function formatExecutionMode(value) {
  return mapLabel(value, EXECUTION_MODE_LABELS)
}

export function formatRelationType(value) {
  return mapLabel(value, RELATION_TYPE_LABELS)
}

export function formatRetrievalMode(value) {
  return mapLabel(value, RETRIEVAL_MODE_LABELS)
}

export function formatAnswerShape(value) {
  return mapLabel(value, ANSWER_SHAPE_LABELS)
}

export function formatChannelName(value) {
  return mapLabel(value, CHANNEL_LABELS, value || '未知通道')
}

export function formatToolName(value) {
  return mapLabel(value, TOOL_LABELS, value || '未知工具')
}

export function formatChannelType(value) {
  return mapLabel(value, CHANNEL_LABELS, value || '未知通道')
}

export function formatExecutionState(value) {
  return mapLabel(value, EXECUTION_STATE_LABELS, '未知')
}

export function formatScore(value) {
  if (value == null || value === '') {
    return '-'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return num.toFixed(4)
}

export function formatRank(value) {
  if (value == null || value === '') {
    return '-'
  }
  return String(value)
}

function latestExchangeQuestion(session) {
  const exchanges = asList(session?.exchanges)
  for (let index = exchanges.length - 1; index >= 0; index -= 1) {
    if (exchanges[index]?.question) {
      return exchanges[index].question
    }
  }
  return ''
}

function latestExchangeAnswer(session) {
  const exchanges = asList(session?.exchanges)
  for (let index = exchanges.length - 1; index >= 0; index -= 1) {
    if (exchanges[index]?.answer) {
      return exchanges[index].answer
    }
  }
  return ''
}

export function sessionTitle(session) {
  const latestUserMessage = session?.latestUserMessage || latestExchangeQuestion(session)
  const latestAssistantMessage = session?.latestAssistantMessage || latestExchangeAnswer(session)
  return truncate(latestUserMessage || latestAssistantMessage || '未命名会话', 28)
}

export function sessionPreview(session) {
  const latestAssistantMessage = session?.latestAssistantMessage || latestExchangeAnswer(session)
  const latestUserMessage = session?.latestUserMessage || latestExchangeQuestion(session)
  return truncate(latestAssistantMessage || latestUserMessage || '暂无内容', 72)
}

export function sessionMessageCount(session) {
  if (session?.messageCount) {
    return session.messageCount
  }
  return asList(session?.exchanges).reduce((count, exchange) => {
    let total = count
    if (exchange?.question) {
      total += 1
    }
    if (exchange?.answer) {
      total += 1
    }
    return total
  }, 0)
}

export function listAssistantExchanges(session) {
  return asList(session?.exchanges).filter((item) => item && item.status)
}

export function resolvePreferredExchange(exchanges, preferredId) {
  if (!exchanges.length) {
    return ''
  }
  const normalizedPreferredId = preferredId ? String(preferredId) : ''
  if (normalizedPreferredId) {
    const matched = exchanges.find((item) => String(item.exchangeId) === normalizedPreferredId)
    if (matched) {
      return String(matched.exchangeId)
    }
  }
  return String(exchanges[exchanges.length - 1].exchangeId)
}

function pushTextBlock(target, label, value, options = {}) {
  if (!value) {
    return
  }
  target.push({
    label,
    value,
    code: Boolean(options.code)
  })
}

function pushListBlock(target, label, items, options = {}) {
  const values = asList(items)
  if (!values.length) {
    return
  }
  target.push({
    label,
    items: values,
    ordered: Boolean(options.ordered)
  })
}

function formatLatency(value) {
  if (value == null || value <= 0) {
    return '无'
  }
  const durationMs = Number(value)
  if (durationMs < 1000) {
    return `${Math.round(durationMs).toLocaleString('zh-CN')} ms`
  }
  return `${(durationMs / 1000).toFixed(1)} s`
}

function formatConfidence(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return ''
  }
  return `${Math.round(Number(value) * 100)}%`
}

function buildChips(...entries) {
  return entries
    .flat()
    .filter((item) => item && item.value)
    .map((item) => ({
      label: item.label,
      value: item.value,
      tone: item.tone || 'neutral'
    }))
}

function buildMetrics(...entries) {
  return entries
    .flat()
    .filter((item) => item && item.value && item.value !== '无')
    .map((item) => ({
      label: item.label,
      value: item.value,
      mono: Boolean(item.mono)
    }))
}

function buildOutcomeSummary(exchange, references) {
  if (!exchange) {
    return ''
  }
  if (exchange.status === 'FAILED') {
    return exchange.errorMessage
      ? `本轮执行失败，结束原因是：${exchange.errorMessage}`
      : '本轮执行失败，但当前没有拿到更具体的错误说明。'
  }
  if (exchange.status === 'STOPPED') {
    return exchange.errorMessage
      ? `本轮被主动停止，结束说明是：${exchange.errorMessage}`
      : '本轮被主动停止。'
  }
  if (exchange.status === 'COMPLETED') {
    if (references.length > 0) {
      return `本轮已完成，并基于 ${references.length} 条最终证据生成回答。`
    }
    return '本轮已完成，但未记录最终引用。'
  }
  return '这是一条正在执行中的轮次，建议优先关注执行过程提示和实时状态。'
}

export function buildExchangeSignalMap(session, exchange) {
  if (!exchange) {
    return {
      inputs: [],
      evidence: null,
      answer: null,
      resources: []
    }
  }

  const trace = exchange.debugTrace || {}
  const references = asList(exchange.references)
  const usedChannels = uniqueStrings(asList(trace.usedChannels).map(formatChannelName))
  const modelUsageTraces = asList(trace.modelUsageTraces)
  const totalTokens = modelUsageTraces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
  const totalCost = modelUsageTraces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  const originalQuestion = trace.originalQuestion || exchange.question || ''
  const retrievalQuestion = trace.retrievalQuestion || trace.rewriteQuestion || ''
  const selectedDocumentName = session?.selectedDocumentName || (trace.selectedDocumentId ? `文档 ${trace.selectedDocumentId}` : '')
  const limitTriggered = Boolean(trace.limitStats?.limitTriggered)

  return {
    inputs: [
      {
        key: 'scope',
        label: '文档范围',
        stageKey: 'request',
        value: selectedDocumentName || '未记录明确文档范围',
        summary: formatChatMode(trace.chatMode || session?.chatMode)
      },
      {
        key: 'understanding',
        label: '系统理解',
        stageKey: 'planning',
        value: retrievalQuestion || '未记录检索问题',
        summary: retrievalQuestion && retrievalQuestion === originalQuestion ? '沿用用户原始问题' : retrievalQuestion ? '已整理检索问题' : '缺少问题理解记录'
      },
      {
        key: 'retrieval',
        label: '检索执行',
        stageKey: 'execution',
        value: usedChannels.length ? `${usedChannels.length} 个检索通道` : '未记录检索通道',
        summary: formatExecutionMode(trace.executionMode),
        details: usedChannels
      }
    ],
    evidence: {
      key: 'evidence',
      label: '最终证据',
      stageKey: 'outcome',
      count: references.length,
      value: references.length ? `${references.length} 条` : '无',
      summary: references.length ? '已形成回答证据' : '未记录最终引用',
      state: references.length ? 'available' : 'missing'
    },
    answer: {
      key: 'answer',
      label: '回答结果',
      stageKey: 'generation',
      status: formatStatusLabel(exchange.status),
      statusCode: exchange.status || '',
      latency: formatLatency(exchange.firstResponseTimeMs),
      preview: truncate(exchange.answer || exchange.errorMessage || '未记录回答内容', 120)
    },
    resources: [
      { label: '模型调用', value: modelUsageTraces.length ? `${modelUsageTraces.length} 次` : '未记录' },
      { label: '总 Token', value: modelUsageTraces.length ? totalTokens.toLocaleString('zh-CN') : '未记录' },
      { label: '预估成本', value: modelUsageTraces.length ? `¥ ${totalCost.toFixed(4)}` : '未记录' },
      {
        label: '调用限制',
        value: limitTriggered ? (trace.limitStats?.limitReason || '已触发') : '未触发',
        tone: limitTriggered ? 'warning' : 'neutral'
      }
    ]
  }
}

export function buildExchangeStatusNarrative(exchange) {
  if (!exchange) {
    return ''
  }
  const trace = exchange.debugTrace || {}
  const intent = trace.intentResolution || null
  const parts = [
    `当前查看的是 exchange ${exchange.exchangeId}。`,
    `执行路径是“${formatExecutionMode(trace.executionMode)}”。`
  ]

  if (intent?.relationType) {
    parts.push(`系统把这句判定为“${formatRelationType(intent.relationType)}”。`)
  }
  if (intent?.retrievalMode) {
    parts.push(`检索策略是“${formatRetrievalMode(intent.retrievalMode)}”。`)
  }
  if (exchange.status === 'FAILED' && exchange.errorMessage) {
    parts.push(`当前结束原因：${exchange.errorMessage}`)
  }
  else if (exchange.status === 'COMPLETED') {
    parts.push('这轮已经成功完成，先看诊断汇流图，再按需查看执行阶段。')
  }
  return parts.join(' ')
}

export function buildExchangeStages(session, exchange) {
  if (!exchange) {
    return []
  }

  const trace = exchange.debugTrace || {}
  const intent = trace.intentResolution || null
  const references = asList(exchange.references)
  const recommendations = asList(exchange.recommendations)
  const thinkingSteps = asList(exchange.thinkingSteps)
  const retrievalNotes = asList(trace.retrievalNotes)
  const modelUsageTraces = asList(trace.modelUsageTraces)
  const executionNotes = uniqueStrings([...thinkingSteps, ...retrievalNotes])
  const usedChannels = uniqueStrings(asList(trace.usedChannels).map(formatChannelName))
  const usedTools = uniqueStrings(asList(exchange.usedTools).map(formatToolName))
  const totalTokens = modelUsageTraces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
  const totalCost = modelUsageTraces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)
  const originalQuestion = trace.originalQuestion || exchange.question || ''
  const retrievalQuestion = trace.retrievalQuestion || trace.rewriteQuestion || ''
  const selectedDocumentName = session?.selectedDocumentName || (trace.selectedDocumentId ? `文档 ${trace.selectedDocumentId}` : '')
  const toolTraces = asList(trace.toolTraces).map((item) => ({
    ...item,
    toolName: formatToolName(item?.toolName),
    topic: item?.topic || ''
  }))

  const requestPrimaryBlocks = []
  pushTextBlock(requestPrimaryBlocks, '当前文档范围', selectedDocumentName)
  pushTextBlock(requestPrimaryBlocks, '用户原始问题', originalQuestion)
  pushTextBlock(requestPrimaryBlocks, '当前日期锚点', trace.currentDateText)

  const requestAdvancedBlocks = []
  pushTextBlock(requestAdvancedBlocks, '会话 ID', session?.conversationId, { code: true })
  pushTextBlock(requestAdvancedBlocks, '轮次 ID', exchange.exchangeId ? String(exchange.exchangeId) : '', { code: true })
  pushTextBlock(requestAdvancedBlocks, 'Agent 增强问题', trace.agentQuestion, { code: true })

  const planningPrimaryBlocks = []
  pushTextBlock(planningPrimaryBlocks, '系统理解后的问题', trace.retrievalQuestion)
  pushTextBlock(planningPrimaryBlocks, '信息需求', intent?.informationNeed)
  pushTextBlock(planningPrimaryBlocks, '判定说明', intent?.rationale)
  pushTextBlock(planningPrimaryBlocks, '检索锚点主问题', trace.retrievalAnchorResolvedQuestion)

  const planningPrimaryLists = []
  if (asList(trace.retrievalSubQuestions).length > 1) {
    pushListBlock(planningPrimaryLists, '最终检索子问题', trace.retrievalSubQuestions, { ordered: true })
  }

  const planningAdvancedBlocks = []
  pushTextBlock(planningAdvancedBlocks, 'Rewrite 独立问题', trace.rewriteQuestion)
  pushTextBlock(planningAdvancedBlocks, '长期摘要', trace.longTermSummary, { code: true })
  pushTextBlock(planningAdvancedBlocks, '回答承接上下文', trace.answerHistoryContext, { code: true })
  pushTextBlock(planningAdvancedBlocks, '规划历史摘要', trace.historySummary, { code: true })
  pushTextBlock(planningAdvancedBlocks, '根主题', trace.retrievalAnchorRootTopic)
  pushTextBlock(planningAdvancedBlocks, '根章节标题', trace.retrievalAnchorRootSectionTitle)
  pushTextBlock(planningAdvancedBlocks, '目标章节提示', trace.retrievalAnchorTargetSectionHint)
  pushTextBlock(planningAdvancedBlocks, '编号项文本', trace.retrievalAnchorItemText)

  const planningAdvancedLists = []
  pushListBlock(planningAdvancedLists, 'Rewrite 子问题拆分', trace.rewriteSubQuestions, { ordered: true })
  pushListBlock(planningAdvancedLists, '软章节提示', intent?.softSectionHints)
  pushListBlock(planningAdvancedLists, '上下文提示词', intent?.queryContextHints)

  const executionPrimaryLists = []
  pushListBlock(executionPrimaryLists, '关键执行节点', executionNotes)

  const executionAdvancedLists = []
  pushListBlock(executionAdvancedLists, '原始 thinking 事件', thinkingSteps)
  pushListBlock(executionAdvancedLists, '原始检索/Agent 轨迹', retrievalNotes)

  const generationPrimaryBlocks = []
  pushTextBlock(generationPrimaryBlocks, '回答预览', exchange.answer, { code: true })

  const generationAdvancedBlocks = []
  pushTextBlock(generationAdvancedBlocks, '系统 Prompt', trace.ragSystemPrompt, { code: true })
  pushTextBlock(generationAdvancedBlocks, '用户 Prompt', trace.ragUserPrompt, { code: true })

  const outcomePrimaryBlocks = []
  pushTextBlock(outcomePrimaryBlocks, '结束说明', exchange.errorMessage)

  const outcomeAdvancedLists = []
  pushListBlock(outcomeAdvancedLists, '推荐追问', recommendations, { ordered: true })

  const stages = [
    {
      key: 'outcome',
      eyebrow: '1. 排障结论',
      title: '结果与诊断',
      summary: buildOutcomeSummary(exchange, references),
      layout: 'wide',
      tone: statusTone(exchange.status),
      chips: buildChips(
        { label: '最终状态', value: formatStatusLabel(exchange.status), tone: statusTone(exchange.status) },
        { label: '引用情况', value: references.length ? `${references.length} 条证据` : '未看到最终引用', tone: references.length ? 'success' : 'warning' }
      ),
      metrics: buildMetrics(
        { label: '最终引用数', value: references.length ? `${references.length}` : '', mono: true },
        { label: '推荐追问', value: recommendations.length ? `${recommendations.length}` : '', mono: true }
      ),
      textBlocks: outcomePrimaryBlocks,
      listBlocks: [],
      references,
      advancedTextBlocks: [],
      advancedListBlocks: outcomeAdvancedLists
    },
    {
      key: 'execution',
      eyebrow: '2. 执行过程',
      title: trace.executionMode === 'REACT_AGENT' ? 'Agent 执行' : '检索执行',
      summary: trace.executionMode === 'REACT_AGENT'
        ? `本轮记录 ${usedTools.length} 个执行组件、${toolTraces.length} 次工具调用和 ${executionNotes.length} 个关键节点。`
        : `本轮启用 ${usedChannels.length} 个检索通道、${usedTools.length} 个执行组件，记录 ${executionNotes.length} 个关键节点。`,
      layout: 'wide',
      tone: 'warning',
      chips: buildChips(
        trace.limitStats?.modelCallsRunLimit ? {
          label: 'ModelHook',
          value: `${trace.limitStats?.modelCallsUsed || 0}/${trace.limitStats?.modelCallsRunLimit || 0}`,
          tone: trace.limitStats?.limitTriggered ? 'warning' : 'neutral'
        } : null,
        trace.limitStats?.toolCallsRunLimit ? {
          label: 'ToolHook',
          value: `${trace.limitStats?.toolCallsUsed || 0}/${trace.limitStats?.toolCallsRunLimit || 0}`,
          tone: trace.limitStats?.limitTriggered ? 'warning' : 'neutral'
        } : null
      ),
      groups: [
        usedChannels.length ? { label: '检索通道', tone: 'success', items: usedChannels } : null,
        usedTools.length ? { label: '执行组件', tone: 'warning', items: usedTools } : null
      ].filter(Boolean),
      metrics: buildMetrics(
        { label: '关键节点数', value: executionNotes.length ? String(executionNotes.length) : '', mono: true },
        { label: '工具调用次数', value: toolTraces.length ? String(toolTraces.length) : '', mono: true }
      ),
      textBlocks: [],
      listBlocks: executionPrimaryLists,
      toolTraces,
      advancedTextBlocks: [],
      advancedListBlocks: executionAdvancedLists
    },
    {
      key: 'planning',
      eyebrow: '3. 系统理解',
      title: '前置编排',
      summary: retrievalQuestion
        ? retrievalQuestion === originalQuestion
          ? '系统沿用用户原始问题作为检索问题，没有改写问题表述。'
          : `系统将检索问题整理为“${truncate(retrievalQuestion, 72)}”。`
        : '当前没有记录系统理解后的检索问题。',
      layout: 'compact',
      tone: 'success',
      chips: buildChips(
        { label: '会话关系', value: formatRelationType(intent?.relationType), tone: 'primary' },
        { label: '检索方式', value: formatRetrievalMode(intent?.retrievalMode), tone: 'success' },
        { label: '答案形态', value: formatAnswerShape(intent?.answerShape), tone: 'neutral' },
        { label: '意图置信度', value: formatConfidence(intent?.confidence), tone: 'warning' },
        { label: '锚点应用', value: trace.retrievalAnchorApplied ? '已使用锚点' : '未使用锚点', tone: trace.retrievalAnchorApplied ? 'success' : 'neutral' }
      ),
      metrics: buildMetrics(
        { label: '摘要覆盖轮次', value: trace.historyCoveredExchangeCount != null ? String(trace.historyCoveredExchangeCount) : '', mono: true },
        { label: '摘要压缩次数', value: trace.historyCompressionCount != null ? String(trace.historyCompressionCount) : '', mono: true }
      ),
      textBlocks: planningPrimaryBlocks,
      listBlocks: planningPrimaryLists,
      advancedTextBlocks: planningAdvancedBlocks,
      advancedListBlocks: planningAdvancedLists
    },
    {
      key: 'request',
      eyebrow: '4. 请求边界',
      title: '请求入口',
      summary: selectedDocumentName
        ? `当前按“${formatChatMode(trace.chatMode || session?.chatMode)}”处理，范围限定到“${truncate(selectedDocumentName, 56)}”。`
        : `当前按“${formatChatMode(trace.chatMode || session?.chatMode)}”处理，没有记录明确的文档名称。`,
      layout: 'compact',
      tone: 'primary',
      chips: buildChips(
        { label: '回答模式', value: formatChatMode(trace.chatMode || session?.chatMode), tone: 'primary' },
        { label: '执行路径', value: formatExecutionMode(trace.executionMode), tone: 'success' },
        { label: '时间解释', value: trace.requiresCurrentDateAnchoring ? '按当前日期解释' : '', tone: 'warning' },
        { label: '实时核实', value: trace.requiresFreshSearch ? '优先核实最新事实' : '', tone: 'warning' }
      ),
      metrics: [],
      textBlocks: requestPrimaryBlocks,
      listBlocks: [],
      advancedTextBlocks: requestAdvancedBlocks,
      advancedListBlocks: []
    },
    {
      key: 'generation',
      eyebrow: '5. 回答生成',
      title: '生成回答',
      summary: `回答状态为“${formatStatusLabel(exchange.status)}”，首包耗时 ${formatLatency(exchange.firstResponseTimeMs)}，共使用 ${references.length} 条最终引用。`,
      layout: 'compact',
      tone: 'neutral',
      chips: buildChips(
        { label: '当前状态', value: formatStatusLabel(exchange.status), tone: statusTone(exchange.status) }
      ),
      metrics: buildMetrics(
        { label: '首包耗时', value: formatLatency(exchange.firstResponseTimeMs), mono: true },
        { label: '总耗时', value: formatLatency(exchange.totalResponseTimeMs), mono: true },
        { label: '引用数', value: references.length ? `${references.length}` : '', mono: true }
      ),
      textBlocks: generationPrimaryBlocks,
      listBlocks: [],
      advancedTextBlocks: generationAdvancedBlocks,
      advancedListBlocks: []
    },
    {
      key: 'usage',
      eyebrow: '6. 模型用量',
      title: '模型使用与限制',
      summary: modelUsageTraces.length
        ? `本轮记录 ${modelUsageTraces.length} 次模型调用，共 ${totalTokens.toLocaleString('zh-CN')} Token，预估成本 ¥ ${totalCost.toFixed(4)}${trace.limitStats ? `；${trace.limitStats.limitTriggered ? '已触发调用限制' : '未触发调用限制'}` : ''}。`
        : '当前没有记录模型调用明细。',
      layout: 'compact',
      tone: 'neutral',
      chips: buildChips(
        trace.limitStats?.limitTriggered ? {
          label: '限制触发',
          value: trace.limitStats?.limitReason || '已触发调用限制',
          tone: 'warning'
        } : null
      ),
      metrics: buildMetrics(
        { label: '模型调用数', value: modelUsageTraces.length ? String(modelUsageTraces.length) : '', mono: true },
        {
          label: '总 Token',
          value: modelUsageTraces.length
            ? totalTokens.toLocaleString('zh-CN')
            : '',
          mono: true
        },
        {
          label: '总成本',
          value: modelUsageTraces.length
            ? `¥ ${totalCost.toFixed(4)}`
            : ''
        }
      ),
      textBlocks: [],
      listBlocks: [],
      advancedTextBlocks: [],
      advancedListBlocks: [
        {
          label: '模型使用清单',
          ordered: false,
          items: modelUsageTraces.map((item) => {
            const tokenText = item?.totalTokens ? `，总Token ${item.totalTokens}` : ''
            const costText = item?.estimatedCost ? `，成本约 ¥${Number(item.estimatedCost).toFixed(4)}` : ''
            const durationText = item?.durationMs ? `，耗时 ${item.durationMs} ms` : ''
            return `${item?.stageName || 'unknown'} | ${item?.provider || 'unknown'} / ${item?.model || 'unknown'}${tokenText}${costText}${durationText}`
          })
        },
        trace.limitStats?.limitReason ? {
          label: '限制说明',
          ordered: false,
          items: [trace.limitStats.limitReason]
        } : null
      ].filter(Boolean)
    }
  ]

  return stages.filter((stage) => {
    return stage.chips?.length
      || stage.metrics?.length
      || stage.textBlocks?.length
      || stage.listBlocks?.length
      || stage.toolTraces?.length
      || stage.references?.length
      || stage.advancedTextBlocks?.length
      || stage.advancedListBlocks?.length
  })
}

export function stageHasAdvancedDetails(stage) {
  if (!stage) {
    return false
  }
  return Boolean(
    stage.advancedTextBlocks?.length
    || stage.advancedListBlocks?.length
    || stage.advancedToolTraces?.length
    || stage.advancedReferences?.length
  )
}

function snapshotValue(snapshot, key) {
  if (!snapshot || typeof snapshot !== 'object') {
    return ''
  }
  return snapshot[key]
}

function snapshotList(snapshot, key) {
  const value = snapshotValue(snapshot, key)
  return Array.isArray(value) ? value.filter(Boolean) : []
}

function pushPair(target, label, value, options = {}) {
  if (value == null || value === '') {
    return
  }
  target.push({
    label,
    value,
    code: Boolean(options.code)
  })
}

function deepParseJson(value, depth = 0) {
  if (depth > 8) {
    return value
  }
  if (typeof value === 'string') {
    const trimmed = value.trim()
    if ((trimmed.startsWith('{') && trimmed.endsWith('}')) || (trimmed.startsWith('[') && trimmed.endsWith(']'))) {
      try {
        return deepParseJson(JSON.parse(trimmed), depth + 1)
      } catch {
        return value
      }
    }
    return value
  }
  if (Array.isArray(value)) {
    return value.map((item) => deepParseJson(item, depth + 1))
  }
  if (value && typeof value === 'object') {
    const result = {}
    for (const [key, item] of Object.entries(value)) {
      result[key] = deepParseJson(item, depth + 1)
    }
    return result
  }
  return value
}

function safeJson(snapshot) {
  if (!snapshot || typeof snapshot !== 'object' || !Object.keys(snapshot).length) {
    return ''
  }
  return JSON.stringify(deepParseJson(snapshot), null, 2)
}

function stageUsageDetails(exchange, stageNames = []) {
  const traces = asList(exchange?.debugTrace?.modelUsageTraces)
  return traces
    .filter((item) => stageNames.includes(item?.stageName))
    .map((item) => {
      const tokens = item?.totalTokens ? `总Token ${item.totalTokens}` : ''
      const prompt = item?.promptTokens ? `输入 ${item.promptTokens}` : ''
      const completion = item?.completionTokens ? `输出 ${item.completionTokens}` : ''
      const cost = item?.estimatedCost ? `成本约 ¥${Number(item.estimatedCost).toFixed(4)}` : ''
      const duration = item?.durationMs ? `耗时 ${item.durationMs} ms` : ''
      return `${item?.stageName || 'unknown'} | ${item?.provider || 'unknown'} / ${item?.model || 'unknown'} | ${[prompt, completion, tokens, cost, duration].filter(Boolean).join('，')}`
    })
}

function formatUsageStageName(stageName) {
  const mapping = {
    intent: '意图分析',
    rewrite: '问题改写',
    summary: '会话记忆压缩',
    rag_answer: '回答生成',
    recommendation: '推荐问题',
    react_agent_turn: 'Agent 推理'
  }
  return mapping[stageName] || stageName || '未知阶段'
}

function buildReferenceDecisionRows(details = []) {
  return asList(details).map((detail) => {
    const text = String(detail || '')
    const index = text.lastIndexOf(' | ')
    if (index === -1) {
      return {
        reference: text,
        reason: ''
      }
    }
    return {
      reference: text.slice(0, index),
      reason: text.slice(index + 3)
    }
  })
}

function findStageSnapshot(stageTraces, stageCode) {
  const trace = asList(stageTraces).find((item) => item?.stageCode === stageCode)
  return trace?.snapshot && typeof trace.snapshot === 'object' ? trace.snapshot : null
}

function channelTraceKey(index, channelType) {
  return `${index || 1}:${channelType || 'unknown'}`
}

function buildChannelTraceLookup(retrieveSnapshot) {
  const lookup = new Map()
  snapshotList(retrieveSnapshot || {}, 'subQuestions').forEach((subQuestion) => {
    const index = subQuestion?.index || 1
    asList(subQuestion?.channelTraces).forEach((trace) => {
      if (!trace || typeof trace !== 'object') {
        return
      }
      lookup.set(channelTraceKey(index, trace.channelName), trace)
    })
  })
  return lookup
}

function referenceIdentityKey(reference, field) {
  if (!reference || reference[field] == null || reference[field] === '') {
    return ''
  }
  return `${reference.documentId || ''}:${field}:${reference[field]}`
}

function normalizedSourceType(reference) {
  return String(reference?.sourceType || '').trim().toUpperCase()
}

function appendFinalReferenceKeys(lookup, reference, rank) {
  const sourceType = normalizedSourceType(reference)
  const keys = []
  keys.push(referenceIdentityKey(reference, 'chunkId'))
  if (!keys.some(Boolean)) {
    keys.push(referenceIdentityKey(reference, 'parentBlockId'))
  }
  keys.filter(Boolean).forEach((key) => {
    if (!lookup.has(key)) {
      lookup.set(key, rank)
    }
  })
}

function buildFinalReferenceLookup(finalReferences = []) {
  const lookup = new Map()
  asList(finalReferences).forEach((reference, index) => {
    appendFinalReferenceKeys(lookup, reference, reference.rank || index + 1)
  })
  return lookup
}

function resolveSelectedRankFromReferences(result, finalReferenceLookup) {
  if (!result || !finalReferenceLookup || !finalReferenceLookup.size) {
    return null
  }
  const keys = [
    referenceIdentityKey(result, 'chunkId'),
    referenceIdentityKey(result, 'parentBlockId')
  ].filter(Boolean)
  for (const key of keys) {
    if (finalReferenceLookup.has(key)) {
      return finalReferenceLookup.get(key)
    }
  }
  return null
}

function resultStatus(result, selectedRank = null) {
  if (result?.isSelected || selectedRank != null) {
    return {
      label: '命中最终证据',
      tone: 'success'
    }
  }
  if (result && result.gatePassed === false) {
    return {
      label: '闸门过滤',
      tone: 'warning'
    }
  }
  return {
    label: '未选入',
    tone: 'neutral'
  }
}

function resultSortKey(result) {
  if (result?.isSelected) {
    return 0
  }
  if (result?.gatePassed === false) {
    return 2
  }
  return 1
}

function buildFusionResultRow(result, finalReferenceLookup) {
  const selectedRank = resolveSelectedRankFromReferences(result, finalReferenceLookup)
  const isSelected = Boolean(result?.isSelected) || selectedRank != null
  const finalRank = result?.finalRank || selectedRank
  const status = resultStatus(result, selectedRank)
  const scoreItems = [
    { key: 'raw', label: '原始分', value: result?.originalScore },
    { key: 'vector', label: '向量分', value: result?.vectorScore },
    { key: 'keyword', label: '关键词分', value: result?.keywordScore },
    { key: 'metadata', label: '元数据分', value: result?.metadataBoost },
    { key: 'rrf', label: '排名融合分', value: result?.rrfScore },
    { key: 'fusion', label: '加权融合分', value: result?.hybridScore },
    { key: 'rerank', label: '重排分', value: result?.rerankScore }
  ].map((item) => ({
    ...item,
    text: formatScore(item.value),
    width: scoreBarWidth(item.value)
  }))
  const stableId = [
    result?.subQuestionIndex || 1,
    result?.channelType || 'unknown',
    result?.channelRank || 0,
    finalRank || 0,
    result?.documentId || '',
    result?.chunkId || '',
    result?.parentBlockId || ''
  ].join('-')
  // O9 如实呈现：命中候选展示后端 selectionReason（finalSelectionReason），被过滤候选展示 filteredReason；
  // 不再用“该候选命中最终引用证据”这类前端文案覆盖后端真实原因（P11 观测重构）。
  const reasonText = isSelected
    ? (result?.selectionReason || 'SELECTED_BY_RANK')
    : (result?.filteredReason || result?.selectionReason || '未进入最终证据')
  const reasonLabel = formatReason(reasonText)
  return {
    id: stableId,
    status,
    candidateId: result?.candidateId || '',
    channelType: result?.channelType || 'unknown',
    channelLabel: formatChannelType(result?.channelType),
    channelRank: result?.channelRank,
    rrfRank: result?.rrfRank,
    finalRank,
    documentId: result?.documentId || '',
    documentName: result?.documentName || '未知文档',
    chunkId: result?.chunkId || '',
    chunkNo: result?.chunkNo,
    parentBlockId: result?.parentBlockId || '',
    parentBlockNo: result?.parentBlockNo,
    sectionPath: result?.sectionPath || '',
    preview: result?.chunkTextPreview || '',
    scoreItems,
    rankFeature: result?.rankFeature || '',
    selectionReason: reasonText,
    selectionReasonText: reasonLabel,
    contextIdentity: result?.contextIdentity || '',
    citationIdentity: result?.citationIdentity || '',
    citationEvidenceType: result?.citationEvidenceType || '',
    contextOnly: Boolean(result?.contextOnly),
    sourceEvidenceResolved: Boolean(result?.sourceEvidenceResolved),
    isSelected,
    gatePassed: result?.gatePassed !== false,
    isElevated: Boolean(result?.isElevated)
  }
}

function buildFusionChannelMetric(channelType, trace, execution, maxWeight) {
  const weight = numericValue(trace?.channelWeight)
  return {
    key: channelType,
    channelType,
    channelLabel: formatChannelType(channelType),
    retrievalIntent: trace?.retrievalIntent || '',
    channelWeight: weight,
    channelWeightText: weight == null ? '未记录权重' : weight.toFixed(2),
    weightWidth: weight == null || maxWeight <= 0 ? '0%' : `${Math.max(8, Math.min((weight / maxWeight) * 100, 100))}%`,
    recalledCount: firstPresent(trace?.recalledCount, execution?.recalledCount, 0),
    acceptedCount: firstPresent(trace?.acceptedCount, execution?.acceptedCount, 0),
    finalSelectedCount: firstPresent(execution?.finalSelectedCount, 0),
    durationMs: execution?.durationMs,
    executionState: execution?.executionState,
    avgScore: execution?.avgScore,
    maxScore: execution?.maxScore,
    minScore: execution?.minScore,
    errorMessage: execution?.errorMessage || ''
  }
}

export function buildFusionTraceGroups(retrievalResults, channelExecutions, stageTraces, finalReferences = []) {
  const retrieveSnapshot = findStageSnapshot(stageTraces, 'RAG_RETRIEVE')
  const channelTraceLookup = buildChannelTraceLookup(retrieveSnapshot)
  const finalReferenceLookup = buildFinalReferenceLookup(finalReferences)
  const executionLookup = new Map()
  asList(channelExecutions).forEach((execution) => {
    executionLookup.set(channelTraceKey(execution.subQuestionIndex || 1, execution.channelType), execution)
  })

  const grouped = new Map()
  asList(retrievalResults).forEach((result) => {
    const index = result.subQuestionIndex || 1
    if (!grouped.has(index)) {
      grouped.set(index, {
        index,
        question: result.subQuestion || `子问题 ${index}`,
        channelTypes: new Set(),
        resultRows: []
      })
    }
    const group = grouped.get(index)
    group.channelTypes.add(result.channelType || 'unknown')
    group.resultRows.push(buildFusionResultRow(result, finalReferenceLookup))
  })

  snapshotList(retrieveSnapshot || {}, 'subQuestions').forEach((subQuestion) => {
    const index = subQuestion?.index || 1
    if (!grouped.has(index)) {
      grouped.set(index, {
        index,
        question: subQuestion?.question || `子问题 ${index}`,
        channelTypes: new Set(),
        resultRows: []
      })
    }
    const group = grouped.get(index)
    group.fusedCandidateCount = subQuestion?.fusedCandidateCount
    group.parentCandidateCount = subQuestion?.parentCandidateCount
    group.rerankedCandidateCount = subQuestion?.rerankedCandidateCount
    group.referenceCount = subQuestion?.referenceCount
    asList(subQuestion?.channelTraces).forEach((trace) => group.channelTypes.add(trace?.channelName || 'unknown'))
  })

  asList(channelExecutions).forEach((execution) => {
    const index = execution.subQuestionIndex || 1
    if (!grouped.has(index)) {
      grouped.set(index, {
        index,
        question: execution.subQuestion || `子问题 ${index}`,
        channelTypes: new Set(),
        resultRows: []
      })
    }
    grouped.get(index).channelTypes.add(execution.channelType || 'unknown')
  })

  const groups = Array.from(grouped.values()).sort((a, b) => a.index - b.index).map((group) => {
    const maxWeight = Array.from(group.channelTypes).reduce((max, channelType) => {
      const trace = channelTraceLookup.get(channelTraceKey(group.index, channelType))
      const weight = numericValue(trace?.channelWeight)
      return Math.max(max, weight || 0)
    }, 0)
    const channelMetrics = Array.from(group.channelTypes).map((channelType) => buildFusionChannelMetric(
      channelType,
      channelTraceLookup.get(channelTraceKey(group.index, channelType)),
      executionLookup.get(channelTraceKey(group.index, channelType)),
      maxWeight
    )).sort((a, b) => (b.channelWeight || 0) - (a.channelWeight || 0) || a.channelLabel.localeCompare(b.channelLabel))
    const resultRows = group.resultRows
      .sort((a, b) => resultSortKey(a) - resultSortKey(b)
        || (a.finalRank || 9999) - (b.finalRank || 9999)
        || (a.channelRank || 9999) - (b.channelRank || 9999))
    channelMetrics.forEach((metric) => {
      const selectedFromRows = resultRows.filter((row) => row.channelType === metric.channelType && row.isSelected).length
      if (resultRows.length) {
        metric.finalSelectedCount = selectedFromRows
      }
    })
    return {
      ...group,
      channelMetrics,
      resultRows,
      selectedCount: resultRows.filter((item) => item.isSelected).length,
      filteredCount: resultRows.filter((item) => !item.gatePassed).length,
      rerankedCount: resultRows.filter((item) => item.scoreItems.some((score) => score.key === 'rerank' && score.text !== '-')).length
    }
  })

  const summary = groups.reduce((acc, group) => {
    acc.subQuestionCount += 1
    acc.channelCount += group.channelMetrics.length
    acc.candidateCount += group.resultRows.length
    acc.selectedCount += group.selectedCount
    acc.filteredCount += group.filteredCount
    acc.rerankedCount += group.rerankedCount
    return acc
  }, {
    subQuestionCount: 0,
    channelCount: 0,
    candidateCount: 0,
    selectedCount: 0,
    filteredCount: 0,
    rerankedCount: 0
  })

  return {
    hasData: groups.length > 0,
    retrievalQuestion: snapshotValue(retrieveSnapshot, 'retrievalQuestion') || '',
    usedChannels: snapshotList(retrieveSnapshot || {}, 'usedChannels').map(formatChannelName),
    summary,
    groups
  }
}

function fusionCandidateScoreText(row, key) {
  const score = asList(row?.scoreItems).find((item) => item.key === key)
  return score?.text || '-'
}

function candidateOutcome(row) {
  if (row?.isSelected) {
    return { key: 'selected', tone: 'success', label: `最终证据${row.finalRank ? ` #${row.finalRank}` : ''}` }
  }
  if (row?.gatePassed === false) {
    return { key: 'filtered', tone: 'warning', label: '闸门过滤' }
  }
  return { key: 'unselected', tone: 'neutral', label: '未选入' }
}

function fusionCandidateContent(row) {
  const sectionPath = String(row?.sectionPath || '').trim()
  if (sectionPath) return { kind: '章节路径', text: sectionPath }
  const preview = String(row?.preview || '').replace(/\s+/g, ' ').trim()
  if (preview) return { kind: '正文预览', text: preview }
  return { kind: '内容状态', text: '未记录可读内容' }
}

function fusionCandidateMeta(row) {
  return `来源：${row.channelLabel}`
}

export function buildRetrievalFlowSummary(fusionTraceView) {
  const groups = asList(fusionTraceView?.groups)
  const totals = groups.reduce((summary, group) => {
    asList(group?.channelMetrics).forEach((metric) => {
      summary.recalled += Number(metric?.recalledCount || 0)
      summary.accepted += Number(metric?.acceptedCount || 0)
    })
    return summary
  }, { recalled: 0, accepted: 0 })
  const summary = fusionTraceView?.summary || {}
  return {
    stages: [
      { key: 'channels', label: '通道执行', value: Number(summary.channelCount || 0) },
      { key: 'recalled', label: '召回候选', value: totals.recalled },
      { key: 'accepted', label: '闸门通过', value: totals.accepted },
      { key: 'reranked', label: '完成重排', value: Number(summary.rerankedCount || 0) },
      { key: 'selected', label: '最终证据', value: Number(summary.selectedCount || 0), tone: 'success' }
    ]
  }
}

export function buildFusionCandidateFlow(group, candidateLimit = 12) {
  const rows = asList(group?.resultRows)
  const limit = Math.max(4, Math.min(Number(candidateLimit) || 12, 20))
  const selectedRows = rows.filter((row) => row.isSelected)
  const remainingRows = rows.filter((row) => !row.isSelected)
  const visibleRows = selectedRows.length >= limit
    ? selectedRows.slice(0, limit)
    : [...selectedRows, ...remainingRows.slice(0, limit - selectedRows.length)]
  const maxRecalled = Math.max(0, ...asList(group?.channelMetrics).map((metric) => Number(metric?.recalledCount || 0)))
  const ratio = (value) => maxRecalled > 0 ? Math.max(0, Math.min(Number(value || 0) / maxRecalled, 1)) : 0
  const channels = asList(group?.channelMetrics).map((metric) => ({
    ...metric,
    recalledRatio: ratio(metric.recalledCount),
    acceptedRatio: ratio(metric.acceptedCount),
    selectedRatio: ratio(metric.finalSelectedCount)
  }))

  if (!visibleRows.length) {
    return { channels, candidates: [], groups: [], totalCount: rows.length, hiddenCount: rows.length }
  }

  const candidates = visibleRows.map((row) => {
    const outcome = candidateOutcome(row)
    const fusionText = fusionCandidateScoreText(row, 'fusion')
    const rerankText = fusionCandidateScoreText(row, 'rerank')
    const content = fusionCandidateContent(row)
    const contentMeta = fusionCandidateMeta(row)
    const fusionLabel = fusionText === '-' ? '未记录' : fusionText
    const rerankLabel = rerankText === '-' ? '未记录' : rerankText
    return {
      key: row.id,
      row,
      groupKey: outcome.key,
      tone: outcome.tone,
      outcomeLabel: outcome.label,
      channelLabel: row.channelLabel,
      contentKind: content.kind,
      contentLabel: content.text,
      contentText: content.text,
      contentMeta,
      fusionLabel,
      rerankLabel,
      stages: [
        { key: 'channel', label: '召回候选', value: row.channelLabel },
        { key: 'fusion', label: '融合分', value: fusionLabel },
        { key: 'rerank', label: '重排分', value: rerankLabel },
        { key: 'outcome', label: '最终结果', value: outcome.label }
      ],
      ariaLabel: `${content.kind}：${content.text}，${contentMeta}，融合分 ${fusionLabel}，重排分 ${rerankLabel}，${outcome.label}`
    }
  })
  const groups = [
    { key: 'selected', label: '最终证据', tone: 'success' },
    { key: 'unselected', label: '未选入', tone: 'neutral' },
    { key: 'filtered', label: '闸门过滤', tone: 'warning' }
  ].map((item) => ({
    ...item,
    candidates: candidates.filter((candidate) => candidate.groupKey === item.key)
  })).filter((item) => item.candidates.length)

  return {
    channels,
    candidates,
    groups,
    totalCount: rows.length,
    hiddenCount: Math.max(rows.length - candidates.length, 0)
  }
}

export function buildChannelExecutionDisplay(channelExecutions, fusionTraceView) {
  const metricMap = new Map()
  asList(fusionTraceView?.groups).forEach((group) => {
    asList(group?.channelMetrics).forEach((metric) => {
      metricMap.set(`${group.index || 1}:${metric.channelType || 'unknown'}`, metric)
    })
  })
  return asList(channelExecutions).map((execution) => ({
    ...execution,
    finalSelectedCount: metricMap.get(`${execution.subQuestionIndex || 1}:${execution.channelType || 'unknown'}`)?.finalSelectedCount
      ?? execution.finalSelectedCount
  }))
}

function normalizeExplicitReference(reference, identity, index) {
  const item = reference || {}
  return {
    ...item,
    key: identity || item.citationIdentity || item.referenceId || `explicit-reference-${index + 1}`,
    identity: identity || item.citationIdentity || item.contextIdentity || '',
    referenceId: item.referenceId || String(index + 1),
    sourceType: item.sourceType || '',
    channelLabel: item.channel ? formatChannelName(item.channel) : '未记录通道',
    documentId: item.documentId || '',
    documentName: item.documentName || item.title || '未命名来源',
    sectionPath: item.sectionPath || '',
    quoteText: item.quoteText || item.snippet || '',
    chunkId: item.chunkId || '',
    parentBlockId: item.parentBlockId || '',
    tableId: item.tableId || '',
    pageNo: item.pageNo
  }
}

export function buildExplicitCitationView(stageTraces, references) {
  const snapshot = findStageSnapshot(stageTraces, 'CITATION_BINDING')
  const explicitCitationIdentities = snapshotList(snapshot || {}, 'explicitCitationIdentities')
  const sourceSnapshotIdentities = snapshotList(snapshot || {}, 'sourceSnapshotIdentities')
  const renderedSourceIdentities = snapshotList(snapshot || {}, 'renderedSourceIdentities')
  const bindings = snapshotList(snapshot || {}, 'bindings')
  const rejectedTokens = snapshotList(snapshot || {}, 'rejectedTokens')
  const finalReferences = asList(references).map((reference, index) => normalizeExplicitReference(
    reference,
    explicitCitationIdentities[index] || sourceSnapshotIdentities[index],
    index
  ))

  return {
    hasData: Boolean(snapshot || finalReferences.length),
    hasSnapshot: Boolean(snapshot),
    authority: snapshotValue(snapshot || {}, 'authority') || '',
    conservationStatus: snapshotValue(snapshot || {}, 'conservationStatus') || '',
    explicitCitationIdentities,
    sourceSnapshotIdentities,
    renderedSourceIdentities,
    bindings,
    rejectedTokens,
    finalReferences,
    summary: {
      renderedSourceCount: renderedSourceIdentities.length,
      parsedTokenCount: firstPresent(snapshotValue(snapshot || {}, 'parsedTokenCount'), 0),
      bindingCount: firstPresent(snapshotValue(snapshot || {}, 'bindingCount'), bindings.length),
      rejectedTokenCount: firstPresent(snapshotValue(snapshot || {}, 'rejectedTokenCount'), rejectedTokens.length),
      finalReferenceCount: finalReferences.length
    }
  }
}

export function buildTraceStageInspector(stageTrace, exchange) {
  if (!stageTrace) {
    return null
  }

  const snapshot = stageTrace.snapshot || {}
  const summaryItems = []
  const listSections = []
  const tableSections = []
  const advancedItems = []

  switch (stageTrace.stageCode) {
    case 'MEMORY':
      pushPair(summaryItems, '是否命中长期摘要', snapshotValue(snapshot, 'compressionApplied') ? '是' : '否')
      pushPair(summaryItems, '摘要覆盖到的最后一轮', snapshotValue(snapshot, 'coveredExchangeId'))
      pushPair(summaryItems, '摘要覆盖轮次', snapshotValue(snapshot, 'coveredExchangeCount'))
      pushPair(summaryItems, '累计压缩次数', snapshotValue(snapshot, 'compressionCount'))
      pushPair(advancedItems, '长期摘要文本', snapshotValue(snapshot, 'longTermSummary'), { code: true })
      pushPair(advancedItems, '最近原文窗口', snapshotValue(snapshot, 'recentTranscript'), { code: true })
      pushPair(advancedItems, '回答阶段最近上下文', snapshotValue(snapshot, 'answerRecentTranscript'), { code: true })
      listSections.push({
        label: '这一阶段的模型使用',
        items: stageUsageDetails(exchange, ['summary']),
        ordered: false
      })
      break
    case 'INTENT':
      pushPair(summaryItems, '原始问题', snapshotValue(snapshot, 'originalQuestion'))
      pushPair(summaryItems, '关系判定', formatRelationType(snapshotValue(snapshot, 'relationType')))
      pushPair(summaryItems, '当前主题', snapshotValue(snapshot, 'resolvedTopic'))
      pushPair(summaryItems, '当前面向', snapshotValue(snapshot, 'resolvedFacet'))
      pushPair(summaryItems, '信息需求', snapshotValue(snapshot, 'informationNeed'))
      pushPair(summaryItems, '答案形态', formatAnswerShape(snapshotValue(snapshot, 'answerShape')))
      pushPair(summaryItems, '检索模式', formatRetrievalMode(snapshotValue(snapshot, 'retrievalMode')))
      pushPair(summaryItems, '检索查询', snapshotValue(snapshot, 'retrievalQuery'))
      pushPair(summaryItems, '置信度', formatConfidence(snapshotValue(snapshot, 'confidence')))
      pushPair(summaryItems, '判定理由', snapshotValue(snapshot, 'rationale'))
      listSections.push({
        label: '分析时参考的上轮锚点',
        items: snapshotValue(snapshot, 'previousAnchorDescription') ? [snapshotValue(snapshot, 'previousAnchorDescription')] : [],
        ordered: false
      })
      listSections.push({
        label: '规划出的检索子问题',
        items: snapshotList(snapshot, 'retrievalSubQuestions'),
        ordered: true
      })
      listSections.push({
        label: '软章节提示',
        items: snapshotList(snapshot, 'softSectionHints'),
        ordered: false
      })
      listSections.push({
        label: '上下文提示词',
        items: snapshotList(snapshot, 'queryContextHints'),
        ordered: false
      })
      listSections.push({
        label: '这一阶段的模型使用',
        items: stageUsageDetails(exchange, ['intent']),
        ordered: false
      })
      break
    case 'REWRITE':
      pushPair(summaryItems, '原始问题', exchange?.question || '')
      pushPair(summaryItems, '改写后问题', snapshotValue(snapshot, 'rewriteQuestion'))
      pushPair(summaryItems, '改写参考历史', snapshotValue(snapshot, 'historyContext'), { code: true })
      pushPair(summaryItems, '参数覆盖', snapshotValue(snapshot, 'rewriteOverrideEnabled') === true ? '已启用' : '未启用')
      pushPair(summaryItems, 'Temperature', snapshotValue(snapshot, 'rewriteTemperature'))
      pushPair(summaryItems, 'TopP', snapshotValue(snapshot, 'rewriteTopP'))
      pushPair(
        summaryItems,
        'Thinking',
        snapshotValue(snapshot, 'rewriteThinking') === true ? 'true' : snapshotValue(snapshot, 'rewriteThinking') === false ? 'false' : ''
      )
      listSections.push({
        label: '改写拆分出的子问题',
        items: snapshotList(snapshot, 'subQuestions'),
        ordered: true
      })
      listSections.push({
        label: '这一阶段的模型使用',
        items: stageUsageDetails(exchange, ['rewrite']),
        ordered: false
      })
      break
    case 'ROUTE':
      pushPair(summaryItems, '原始问题', snapshotValue(snapshot, 'originalQuestion'))
      pushPair(summaryItems, '最终执行路径', formatExecutionMode(snapshotValue(snapshot, 'executionMode')))
      pushPair(summaryItems, '最终检索问题', snapshotValue(snapshot, 'retrievalQuestion'))
      pushPair(summaryItems, '根主题', snapshotValue(snapshot, 'rootTopic'))
      pushPair(summaryItems, '根章节编码', snapshotValue(snapshot, 'rootSectionCode'))
      pushPair(summaryItems, '根章节标题', snapshotValue(snapshot, 'rootSectionTitle'))
      pushPair(summaryItems, '目标章节提示', snapshotValue(snapshot, 'targetSectionHint'))
      pushPair(summaryItems, '是否使用锚点', snapshotValue(snapshot, 'anchorApplied') ? '是' : '否')
      listSections.push({
        label: '最终检索子问题',
        items: snapshotList(snapshot, 'retrievalSubQuestions'),
        ordered: true
      })
      break
    case 'RAG_RETRIEVE':
      pushPair(summaryItems, '实际检索问题', snapshotValue(snapshot, 'retrievalQuestion'))
      pushPair(summaryItems, '最终证据条数', snapshotValue(snapshot, 'referenceCount'))
      pushPair(summaryItems, '子问题数量', snapshotValue(snapshot, 'subQuestionCount'))
      listSections.push({
        label: '使用通道',
        items: snapshotList(snapshot, 'usedChannels').map(formatChannelName),
        ordered: false
      })
      listSections.push({
        label: '检索过程说明',
        items: snapshotList(snapshot, 'retrievalNotes'),
        ordered: false
      })
      listSections.push({
        label: '子问题检索明细',
        items: snapshotList(snapshot, 'subQuestions').map((item) => {
          if (!item || typeof item !== 'object') {
            return ''
          }
          const channelTraceText = asList(item.channelTraces).map((trace) => {
            if (!trace || typeof trace !== 'object') {
              return ''
            }
            const weightText = trace.channelWeight == null ? '' : ` weight=${formatScore(trace.channelWeight)}`
            const intentText = trace.retrievalIntent ? ` intent=${trace.retrievalIntent}` : ''
            return `${formatChannelName(trace.channelName)} raw=${trace.recalledCount || 0} accepted=${trace.acceptedCount || 0}${weightText}${intentText}`
          }).filter(Boolean).join('；')
          return `${item.index}. ${item.question} | 通道 ${channelTraceText || '无'} | fused ${item.fusedCandidateCount || 0} | parent ${item.parentCandidateCount || 0} | rerank ${item.rerankedCandidateCount || 0} | 文档 ${item.documentCount || 0} | 引用 ${item.referenceCount || 0}`
        }).filter(Boolean),
        ordered: false
      })
      listSections.push({
        label: '最终证据概览',
        items: snapshotList(snapshot, 'references').map((item) => {
          if (!item || typeof item !== 'object') {
            return ''
          }
          return `[${item.referenceId || '-'}] ${item.documentName || '未命名引用'} ${item.sectionPath ? `| ${item.sectionPath}` : ''} ${item.channel ? `| ${formatChannelName(item.channel)}` : ''}`.trim()
        }).filter(Boolean),
        ordered: false
      })
      tableSections.push({
        label: '子问题检索链路',
        columns: ['子问题', '关键词', '向量', '融合', '父块', '重排', '最终引用'],
        rows: snapshotList(snapshot, 'subQuestions').map((item) => {
          if (!item || typeof item !== 'object') {
            return null
          }
          const channelTraces = Array.isArray(item.channelTraces) ? item.channelTraces : []
          const countTrace = (channelName) => {
            const trace = channelTraces.find((entry) => entry?.channelName === channelName)
            const weight = trace?.channelWeight == null ? '' : ` / w ${formatScore(trace.channelWeight)}`
            return `${trace?.recalledCount ?? 0} / ${trace?.acceptedCount ?? 0}${weight}`
          }
          return {
            cells: [
              `${item.index}. ${item.question}`,
              countTrace('keyword'),
              countTrace('vector'),
              String(item.fusedCandidateCount ?? 0),
              String(item.parentCandidateCount ?? 0),
              String(item.rerankedCandidateCount ?? 0),
              String(item.referenceCount ?? 0)
            ]
          }
        }).filter(Boolean)
      })
      tableSections.push({
        label: '最终证据表',
        columns: ['引用', '文档', '章节', '通道', '页码/位置'],
        rows: snapshotList(snapshot, 'references').map((item) => {
          if (!item || typeof item !== 'object') {
            return null
          }
          return {
            cells: [
              item.referenceId || '-',
              item.documentName || '未命名引用',
              item.sectionPath || '未识别章节',
              formatChannelName(item.channel),
              item.pageNo ? `第 ${item.pageNo} 页` : (item.pageRange || '-')
            ]
          }
        }).filter(Boolean)
      })
      tableSections.push({
        label: '表格证据定位',
        columns: ['引用', '表格', '操作', '命中行', '证据列', '单元格坐标', 'bbox'],
        rows: buildTableEvidenceItems(snapshotList(snapshot, 'references')).map((item) => ({
          cells: [
            item.referenceId,
            `${item.tableTitle}${item.tableId ? ` / ID ${item.tableId}` : ''}`,
            item.operationText,
            item.rowsText,
            item.columnsText,
            item.cellsText,
            item.bboxText
          ]
        }))
      })
      break
    case 'CITATION_BINDING': {
      const bindingView = buildExplicitCitationView([stageTrace], exchange?.references || [])
      pushPair(summaryItems, '引用权威', bindingView.authority)
      pushPair(summaryItems, '解析 token 数', bindingView.summary.parsedTokenCount)
      pushPair(summaryItems, '合法绑定数', bindingView.summary.bindingCount)
      pushPair(summaryItems, '拒绝 token 数', bindingView.summary.rejectedTokenCount)
      pushPair(summaryItems, '最终引用数', bindingView.summary.finalReferenceCount)
      pushPair(summaryItems, '守恒状态', bindingView.conservationStatus)
      listSections.push({
        label: '显式引用 identities',
        items: bindingView.explicitCitationIdentities,
        ordered: true
      })
      listSections.push({
        label: 'Source snapshot identities',
        items: bindingView.sourceSnapshotIdentities,
        ordered: true
      })
      tableSections.push({
        label: '显式 token 绑定',
        columns: ['Token', 'Reference', 'Identity', 'Disposition'],
        rows: bindingView.bindings.map((item) => ({
          cells: [
            item.token || '-',
            item.referenceId || '-',
            item.identity || '-',
            item.bindingDisposition || '-'
          ]
        }))
      })
      tableSections.push({
        label: '被拒绝 token',
        columns: ['Token', 'Reference', 'Reason'],
        rows: bindingView.rejectedTokens.map((item) => ({
          cells: [
            item.token || '-',
            item.referenceId || '-',
            item.reason || '-'
          ]
        }))
      })
      tableSections.push({
        label: '最终 source snapshot',
        columns: ['Identity', '引用', '来源', '位置'],
        rows: bindingView.finalReferences.map((item) => ({
          cells: [
            item.identity || '-',
            item.referenceId || '-',
            item.documentName,
            item.pageNo ? `第 ${item.pageNo} 页` : (item.sectionPath || '-')
          ]
        }))
      })
      break
    }
    case 'EVIDENCE_BUDGET':
      pushPair(summaryItems, '总预算', snapshotValue(snapshot, 'totalBudget'))
      pushPair(summaryItems, '单子问题预算', snapshotValue(snapshot, 'perSubQuestionBudget'))
      pushPair(summaryItems, '实际渲染引用', snapshotValue(snapshot, 'renderedReferenceCount'))
      pushPair(summaryItems, '被省略引用', snapshotValue(snapshot, 'omittedReferenceCount'))
      listSections.push({
        label: '已纳入 Prompt 的引用',
        items: snapshotList(snapshot, 'renderedReferenceDetails'),
        ordered: false
      })
      listSections.push({
        label: '因预算被省略的引用',
        items: snapshotList(snapshot, 'omittedReferenceDetails'),
        ordered: false
      })
      tableSections.push({
        label: '保留到 Prompt 的引用',
        columns: ['引用', '结果'],
        rows: buildReferenceDecisionRows(snapshotList(snapshot, 'renderedReferenceDetails')).map((item) => ({
          cells: [item.reference, item.reason || '已纳入 Prompt']
        }))
      })
      tableSections.push({
        label: '因预算被裁掉的引用',
        columns: ['引用', '原因'],
        rows: buildReferenceDecisionRows(snapshotList(snapshot, 'omittedReferenceDetails')).map((item) => ({
          cells: [item.reference, item.reason || '超出上下文预算']
        }))
      })
      pushPair(advancedItems, '系统 Prompt', snapshotValue(snapshot, 'systemPrompt'), { code: true })
      pushPair(advancedItems, '用户 Prompt', snapshotValue(snapshot, 'userPrompt'), { code: true })
      break
    case 'ANSWER_GENERATE':
      pushPair(summaryItems, '首包耗时', snapshotValue(snapshot, 'firstResponseTimeMs') ? `${snapshotValue(snapshot, 'firstResponseTimeMs')} ms` : '')
      pushPair(summaryItems, '回答长度', snapshotValue(snapshot, 'answerLength'))
      pushPair(advancedItems, '本轮回答全文', exchange?.answer || '', { code: true })
      listSections.push({
        label: '这一阶段的模型使用',
        items: stageUsageDetails(exchange, ['rag_answer', 'react_agent_turn']),
        ordered: false
      })
      break
    case 'REACT_AGENT':
      pushPair(summaryItems, '使用组件数', snapshotList(snapshot, 'usedTools').length)
      listSections.push({
        label: '使用组件',
        items: snapshotList(snapshot, 'usedTools').map(formatToolName),
        ordered: false
      })
      break
    case 'RECOMMENDATION':
      pushPair(summaryItems, '推荐问题数量', snapshotValue(snapshot, 'recommendationCount'))
      listSections.push({
        label: '推荐问题列表',
        items: snapshotList(snapshot, 'recommendations'),
        ordered: true
      })
      listSections.push({
        label: '这一阶段的模型使用',
        items: stageUsageDetails(exchange, ['recommendation']),
        ordered: false
      })
      break
    case 'FINALIZE':
      pushPair(summaryItems, '最终状态', formatStatusLabel(snapshotValue(snapshot, 'finalStatus')))
      pushPair(summaryItems, '回答长度', snapshotValue(snapshot, 'answerLength'))
      pushPair(summaryItems, '引用数', snapshotValue(snapshot, 'referenceCount'))
      pushPair(summaryItems, '推荐问题数', snapshotValue(snapshot, 'recommendationCount'))
      pushPair(summaryItems, '结束原因', snapshotValue(snapshot, 'reason') || snapshotValue(snapshot, 'errorMessage'))
      break
    default:
      pushPair(summaryItems, '阶段摘要', stageTrace.summaryText || '')
      break
  }

  const rawSnapshot = safeJson(snapshot)
  if (rawSnapshot) {
    pushPair(advancedItems, '原始阶段快照 JSON', rawSnapshot, { code: true })
  }

  const normalizedListSections = listSections
    .map((section) => ({
      ...section,
      items: asList(section.items)
    }))
    .filter((section) => section.items.length > 0)

  return {
    title: stageTrace.stageName || stageTrace.stageCode || '未知阶段',
    stageCode: stageTrace.stageCode || '',
    summary: stageTrace.summaryText || '',
    status: stageTrace.stageState,
    startTime: stageTrace.startTime,
    endTime: stageTrace.endTime,
    durationMs: stageTrace.durationMs,
    summaryItems,
    listSections: normalizedListSections,
    tableSections: tableSections.filter((section) => section.rows && section.rows.length > 0),
    advancedItems
  }
}

export function buildUsageStageInspector(exchange) {
  if (!exchange) {
    return null
  }

  const usageTraces = asList(exchange.debugTrace?.modelUsageTraces)
  const limitStats = exchange.debugTrace?.limitStats || null
  const totalPromptTokens = usageTraces.reduce((sum, item) => sum + Number(item?.promptTokens || 0), 0)
  const totalCompletionTokens = usageTraces.reduce((sum, item) => sum + Number(item?.completionTokens || 0), 0)
  const totalTokens = usageTraces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0)
  const totalCost = usageTraces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0)

  const rows = usageTraces.map((item) => ({
    cells: [
      formatUsageStageName(item.stageName),
      `${item.provider || 'unknown'} / ${item.model || 'unknown'}`,
      String(item.promptTokens ?? 0),
      String(item.completionTokens ?? 0),
      String(item.totalTokens ?? 0),
      item.estimatedCost ? `¥ ${Number(item.estimatedCost).toFixed(4)}` : '无',
      item.durationMs ? `${item.durationMs} ms` : '无',
      item.status || 'UNKNOWN'
    ]
  }))

  return {
    title: '模型使用与限制',
    summary: '这一轮里每一次模型调用都按阶段分组列在下面，便于排查到底哪个阶段最耗 token 和成本。',
    status: limitStats?.limitTriggered ? 'WARNING' : 'COMPLETED',
    startTime: exchange.createTime,
    endTime: exchange.editTime,
    durationMs: exchange.totalResponseTimeMs,
    summaryItems: [
      {
        label: '模型调用次数',
        value: String(usageTraces.length)
      },
      {
        label: '输入 Token',
        value: String(totalPromptTokens)
      },
      {
        label: '输出 Token',
        value: String(totalCompletionTokens)
      },
      {
        label: '总 Token',
        value: String(totalTokens)
      },
      {
        label: '总成本',
        value: totalCost > 0 ? `¥ ${totalCost.toFixed(4)}` : '无'
      },
      {
        label: '模型运行上限',
        value: limitStats?.modelCallsRunLimit != null ? `${limitStats.modelCallsUsed || 0}/${limitStats.modelCallsRunLimit}` : ''
      },
      {
        label: '工具运行上限',
        value: limitStats?.toolCallsRunLimit != null ? `${limitStats.toolCallsUsed || 0}/${limitStats.toolCallsRunLimit}` : ''
      },
      {
        label: '限制触发',
        value: limitStats?.limitTriggered ? (limitStats.limitReason || '已触发') : '未触发'
      }
    ].filter((item) => item.value),
    listSections: [],
    tableSections: rows.length
      ? [{
          label: '按阶段分组的模型使用明细',
          columns: ['阶段', '模型', '输入 Token', '输出 Token', '总 Token', '成本', '耗时', '状态'],
          rows
        }]
      : [],
    advancedItems: [
      limitStats?.modelCallsThreadLimit != null
        ? { label: '线程级模型上限', value: String(limitStats.modelCallsThreadLimit) }
        : null,
      limitStats?.toolCallsThreadLimit != null
        ? { label: '线程级工具上限', value: String(limitStats.toolCallsThreadLimit) }
        : null
    ].filter(Boolean)
  }
}

export function groupResultsBySubQuestion(results) {
  if (!results || !results.length) {
    return []
  }

  const grouped = new Map()

  results.forEach((result) => {
    const index = result.subQuestionIndex || 1
    if (!grouped.has(index)) {
      grouped.set(index, {
        index,
        question: result.subQuestion || `子问题 ${index}`,
        channels: new Map()
      })
    }

    const subQ = grouped.get(index)
    const channelType = result.channelType || 'unknown'

    if (!subQ.channels.has(channelType)) {
      subQ.channels.set(channelType, {
        type: channelType,
        results: []
      })
    }

    subQ.channels.get(channelType).results.push(result)
  })

  return Array.from(grouped.values()).map((subQ) => ({
    index: subQ.index,
    question: subQ.question,
    channels: Array.from(subQ.channels.values())
  }))
}
