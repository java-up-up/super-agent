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
  RAG_CHAT: '文档检索问答',
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
  youcom_search: 'You.com 联网搜索',
  youcom_research: 'You.com 深度研究',
  keyword: '关键词检索通道',
  vector: '向量检索通道',
  rerank: '重排精排'
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
  return `${value} ms`
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
      return `本轮已完成，并基于 ${references.length} 条最终证据生成回答。排障时优先核对这些引用是否真的支撑了答案。`
    }
    return '本轮已完成，但没有看到最终引用，适合继续检查检索或 Prompt 组装阶段。'
  }
  return '这是一条正在执行中的轮次，建议优先关注执行过程提示和实时状态。'
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
    parts.push('这轮已经成功完成，默认先看“结果与诊断”和“执行过程”两块。')
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
  const toolTraces = asList(trace.toolTraces).map((item) => ({
    ...item,
    toolName: formatToolName(item?.toolName),
    topic: item?.topic || ''
  }))

  const requestPrimaryBlocks = []
  pushTextBlock(requestPrimaryBlocks, '用户原始问题', trace.originalQuestion || exchange.question)
  pushTextBlock(requestPrimaryBlocks, '当前日期锚点', trace.currentDateText)

  const requestAdvancedBlocks = []
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
  pushTextBlock(outcomePrimaryBlocks, '排障结论', buildOutcomeSummary(exchange, references))
  pushTextBlock(outcomePrimaryBlocks, '结束说明', exchange.errorMessage)

  const outcomeAdvancedLists = []
  pushListBlock(outcomeAdvancedLists, '推荐追问', recommendations, { ordered: true })

  const stages = [
    {
      key: 'outcome',
      eyebrow: '1. 排障结论',
      title: '结果与诊断',
      subtitle: '先看这块，快速判断这轮到底是成功、失败、停止，还是证据不足。',
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
      subtitle: trace.executionMode === 'REACT_AGENT'
        ? '如果结果不对，先看 Agent 有没有调用工具、工具回来了什么。'
        : '如果结果不对，先看检索通道、执行节点和最终证据组织是否正常。',
      tone: 'warning',
      chips: buildChips(
        asList(trace.usedChannels).map((item) => ({ label: '使用通道', value: formatChannelName(item), tone: 'success' })),
        asList(exchange.usedTools).map((item) => ({ label: '使用组件', value: formatToolName(item), tone: 'warning' })),
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
      subtitle: '当你怀疑系统“理解错问题”时，看这块最直接。',
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
      subtitle: '确认用户原始问题、模式和文档边界有没有偏掉。',
      tone: 'primary',
      chips: buildChips(
        { label: '回答模式', value: formatChatMode(trace.chatMode || session?.chatMode), tone: 'primary' },
        { label: '执行路径', value: formatExecutionMode(trace.executionMode), tone: 'success' },
        { label: '文档范围', value: session?.selectedDocumentName || (trace.selectedDocumentId ? `文档 ${trace.selectedDocumentId}` : ''), tone: 'neutral' },
        { label: '时间解释', value: trace.requiresCurrentDateAnchoring ? '按当前日期解释' : '', tone: 'warning' },
        { label: '实时核实', value: trace.requiresFreshSearch ? '优先核实最新事实' : '', tone: 'warning' }
      ),
      metrics: buildMetrics(
        { label: '会话ID', value: session?.conversationId, mono: true },
        { label: '轮次ID', value: exchange.exchangeId ? String(exchange.exchangeId) : '', mono: true }
      ),
      textBlocks: requestPrimaryBlocks,
      listBlocks: [],
      advancedTextBlocks: requestAdvancedBlocks,
      advancedListBlocks: []
    },
    {
      key: 'generation',
      eyebrow: '5. 高级生成',
      title: '生成回答',
      subtitle: '默认只看耗时和回答预览；Prompt 已折叠到高级技术细节里。',
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
      subtitle: '这一块解释这轮回答消耗了多少模型资源，以及是否触发调用限制。',
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
            ? String(modelUsageTraces.reduce((sum, item) => sum + Number(item?.totalTokens || 0), 0))
            : '',
          mono: true
        },
        {
          label: '总成本',
          value: modelUsageTraces.length
            ? `¥ ${modelUsageTraces.reduce((sum, item) => sum + Number(item?.estimatedCost || 0), 0).toFixed(4)}`
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

function safeJson(snapshot) {
  if (!snapshot || typeof snapshot !== 'object' || !Object.keys(snapshot).length) {
    return ''
  }
  return JSON.stringify(snapshot, null, 2)
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
            return `${formatChannelName(trace.channelName)} raw=${trace.recalledCount || 0} accepted=${trace.acceptedCount || 0}`
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
        columns: ['子问题', '关键词 raw/accepted', '向量 raw/accepted', '融合', '父块', '重排', '最终引用'],
        rows: snapshotList(snapshot, 'subQuestions').map((item) => {
          if (!item || typeof item !== 'object') {
            return null
          }
          const channelTraces = Array.isArray(item.channelTraces) ? item.channelTraces : []
          const keywordTrace = channelTraces.find((trace) => trace?.channelName === 'keyword')
          const vectorTrace = channelTraces.find((trace) => trace?.channelName === 'vector')
          return {
            cells: [
              `${item.index}. ${item.question}`,
              `${keywordTrace?.recalledCount ?? 0} / ${keywordTrace?.acceptedCount ?? 0}`,
              `${vectorTrace?.recalledCount ?? 0} / ${vectorTrace?.acceptedCount ?? 0}`,
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
        columns: ['引用', '文档', '章节', '通道'],
        rows: snapshotList(snapshot, 'references').map((item) => {
          if (!item || typeof item !== 'object') {
            return null
          }
          return {
            cells: [
              item.referenceId || '-',
              item.documentName || '未命名引用',
              item.sectionPath || '未识别章节',
              formatChannelName(item.channel)
            ]
          }
        }).filter(Boolean)
      })
      break
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
    title: stageTrace.stageName,
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
