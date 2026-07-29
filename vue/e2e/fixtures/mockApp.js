const ADMIN_TOKEN_KEY = 'nexus-agent-admin-token'
const ADMIN_USER_KEY = 'nexus-agent-admin-user'

const fixtureExchange = {
  exchangeId: '90071992547409931234',
  question: '这个极长的问题标题用于验证中文内容在窄屏和百分之二百缩放下仍然能够完整换行吗？',
  answer: '这是隔离 fixture 的回答。[1]',
  status: 'COMPLETED',
  firstResponseTimeMs: 740,
  totalResponseTimeMs: 1000,
  references: [{ referenceId: '1' }],
  recommendations: [{ question: '如何继续检查？' }, { question: '如何核对引用？' }, { question: '如何查看执行过程？' }],
  usedTools: ['vector', 'keyword', 'graph-rag'],
  thinkingSteps: ['规划知识检索范围。', '根据结构锚点扩展正文。'],
  debugTrace: {
    chatMode: 'DOCUMENT',
    executionMode: 'STANDARD',
    originalQuestion: '这个极长的问题标题用于验证中文内容在窄屏和百分之二百缩放下仍然能够完整换行吗？',
    retrievalQuestion: '验证中文长问题在窄屏和百分之二百缩放下能否完整换行',
    usedChannels: ['vector', 'keyword', 'graph-rag'],
    limitStats: {
      modelCallsUsed: 1,
      modelCallsRunLimit: 8,
      toolCallsUsed: 3,
      toolCallsRunLimit: 6,
      limitTriggered: false
    },
    modelUsageTraces: [{ stageName: 'rag_answer', totalTokens: 958, estimatedCost: 0.0043 }]
  },
  createTime: '2026-07-21T08:00:00Z',
  editTime: '2026-07-21T08:00:01Z'
}

const fixtureStageTraces = [{
  stageId: 'stage-route-90071992547409931234',
  stageName: '请求入口',
  stageCode: 'ROUTE',
  stageState: 'COMPLETED',
  summaryText: '确认本轮请求范围并记录执行路径。',
  durationMs: 128,
  startTime: '2026-07-21T08:00:00Z',
  endTime: '2026-07-21T08:00:00.128Z',
  snapshot: { executionMode: 'STANDARD', retrievalQuestion: fixtureExchange.question }
}, {
  stageId: 'stage-answer-90071992547409931234',
  stageName: '生成回答',
  stageCode: 'ANSWER_GENERATE',
  stageState: 'COMPLETED',
  summaryText: '根据本轮证据生成最终回答。',
  durationMs: 872,
  startTime: '2026-07-21T08:00:00.128Z',
  endTime: '2026-07-21T08:00:01Z',
  snapshot: { answerLength: fixtureExchange.answer.length }
}]

const fixtureRetrievalResults = [{
  candidateId: 'candidate-vector-selected',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'vector',
  channelRank: 2,
  rrfRank: 2,
  originalScore: 0.81,
  vectorScore: 0.93,
  rrfScore: 0.88,
  hybridScore: 0.91,
  rerankScore: 0.97,
  documentId: 'doc-90071992547409931234',
  documentName: '星联智服全渠道客服平台上线与运营管理手册_响应式与可访问性验证长文件名.md',
  chunkId: 'chunk-vector-selected',
  chunkNo: 18,
  parentBlockNo: 64,
  sectionPath: '第六章 检索观测 / 6.4 候选融合与最终证据确认流程',
  chunkTextPreview: '候选需要经过通道召回、融合、重排和最终证据选择。',
  isSelected: true,
  finalRank: 1,
  gatePassed: true,
  selectionReason: 'SELECTED_BY_RANK'
}, {
  candidateId: 'candidate-keyword-neutral',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'keyword',
  channelRank: 1,
  rrfRank: 1,
  keywordScore: 0.89,
  rrfScore: 0.85,
  hybridScore: 0.86,
  rerankScore: 0.74,
  documentId: 'doc-90071992547409931234',
  documentName: '客服平台故障排查与指标定义补充说明.md',
  chunkId: 'chunk-keyword-neutral',
  chunkNo: 7,
  parentBlockNo: 71,
  sectionPath: '',
  chunkTextPreview: '该候选通过质量闸门，但没有进入本轮最终证据窗口。',
  isSelected: false,
  gatePassed: true,
  selectionReason: 'FILTERED_BY_FINAL_TOP_K'
}, {
  candidateId: 'candidate-graph-filtered',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'graph-rag',
  channelRank: 1,
  rrfRank: 4,
  originalScore: 0.48,
  hybridScore: 0.51,
  documentId: 'doc-graph-fixture',
  documentName: '客服平台关系图谱维护指南.md',
  chunkId: 'chunk-graph-filtered',
  chunkNo: 4,
  parentBlockNo: 14,
  sectionPath: '关系图谱 / 无效关联处理',
  chunkTextPreview: '该候选没有通过当前通道的质量闸门。',
  isSelected: false,
  gatePassed: false,
  filteredReason: 'FILTERED_BY_CHANNEL_GATE'
}, {
  candidateId: 'candidate-vector-neutral',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'vector',
  channelRank: 1,
  rrfRank: 3,
  vectorScore: 0.88,
  hybridScore: 0.79,
  rerankScore: 0.69,
  documentId: 'doc-90071992547409931234',
  documentName: '星联智服全渠道客服平台上线与运营管理手册.md',
  chunkId: 'chunk-vector-neutral',
  chunkNo: 17,
  parentBlockNo: 63,
  sectionPath: '第六章 检索观测 / 6.3 通道质量检查',
  isSelected: false,
  gatePassed: true
}, {
  candidateId: 'candidate-table-selected',
  subQuestionIndex: 2,
  subQuestion: '哪些真实指标可以判断本轮检索执行是否正常？',
  channelType: 'table',
  channelRank: 1,
  rrfRank: 1,
  originalScore: 0.9,
  hybridScore: 0.94,
  rerankScore: 0.95,
  documentId: 'doc-90071992547409931234',
  documentName: '星联智服全渠道客服平台上线与运营管理手册.md',
  chunkId: 'chunk-table-selected',
  chunkNo: 21,
  parentBlockNo: 72,
  sectionPath: '第七章 指标口径 / 表 7-2 检索执行健康指标',
  isSelected: true,
  finalRank: 2,
  gatePassed: true,
  selectionReason: 'SELECTED_TOP_RANK'
}, {
  candidateId: 'candidate-keyword-second-neutral',
  subQuestionIndex: 2,
  subQuestion: '哪些真实指标可以判断本轮检索执行是否正常？',
  channelType: 'keyword',
  channelRank: 1,
  rrfRank: 2,
  keywordScore: 0.82,
  hybridScore: 0.76,
  rerankScore: 0.7,
  documentId: 'doc-metrics-fixture',
  documentName: '运营指标核对手册.md',
  chunkId: 'chunk-keyword-second-neutral',
  chunkNo: 9,
  parentBlockNo: 75,
  sectionPath: '检索指标 / 召回与闸门通过率',
  isSelected: false,
  gatePassed: true
}]

const fixtureChannelExecutions = [{
  id: 'execution-vector-1',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'vector',
  executionState: 1,
  recalledCount: 12,
  acceptedCount: 8,
  finalSelectedCount: 1,
  durationMs: 318,
  avgScore: 0.81,
  minScore: 0.44,
  maxScore: 0.93
}, {
  id: 'execution-keyword-1',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'keyword',
  executionState: 1,
  recalledCount: 7,
  acceptedCount: 4,
  finalSelectedCount: 0,
  durationMs: 142,
  avgScore: 0.69,
  minScore: 0.32,
  maxScore: 0.89
}, {
  id: 'execution-graph-1',
  subQuestionIndex: 1,
  subQuestion: '如何验证窄屏下的检索结果仍然保持完整且可追踪？',
  channelType: 'graph-rag',
  executionState: 1,
  recalledCount: 4,
  acceptedCount: 0,
  finalSelectedCount: 0,
  durationMs: 506,
  avgScore: 0.48,
  minScore: 0.41,
  maxScore: 0.55
}, {
  id: 'execution-table-2',
  subQuestionIndex: 2,
  subQuestion: '哪些真实指标可以判断本轮检索执行是否正常？',
  channelType: 'table',
  executionState: 1,
  recalledCount: 5,
  acceptedCount: 3,
  finalSelectedCount: 1,
  durationMs: 226,
  avgScore: 0.84,
  minScore: 0.62,
  maxScore: 0.9
}, {
  id: 'execution-keyword-2',
  subQuestionIndex: 2,
  subQuestion: '哪些真实指标可以判断本轮检索执行是否正常？',
  channelType: 'keyword',
  executionState: 1,
  recalledCount: 8,
  acceptedCount: 5,
  finalSelectedCount: 0,
  durationMs: 156,
  avgScore: 0.71,
  minScore: 0.39,
  maxScore: 0.86
}]

const fixtureSession = {
  conversationId: 'conversation_01HZYX0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ',
  selectedDocumentId: 'doc-90071992547409931234',
  selectedDocumentName: '超长知识库文档标题用于响应式与辅助技术验证',
  selectedKnowledgeBaseIds: ['kb-90071992547409931234'],
  knowledgeBaseSelectionMode: 'SELECTED',
  latestQuestion: fixtureExchange.question,
  latestUserMessage: fixtureExchange.question,
  latestAnswer: fixtureExchange.answer,
  chatMode: 'DOCUMENT',
  latestTurnStatus: 'COMPLETED',
  latestExchangeId: fixtureExchange.exchangeId,
  messageCount: 2,
  checkpointCount: 1,
  running: false,
  memorySummary: {
    compressionApplied: true,
    coveredExchangeCount: 1,
    summaryVersion: 1,
    compressionCount: 1,
    summaryText: '【会话目标】\n核对响应式与辅助技术表现\n\n【已确认信息】\n- 摘要正文默认不占用页面高度\n- 完整内容通过居中弹窗查看'
  },
  exchanges: [fixtureExchange],
  updatedAt: '2026-07-21T08:00:01Z'
}

const fixtureKnowledgeBase = {
  id: 'kb-90071992547409931234',
  baseName: '产品制度与超长英文标识 KnowledgeBaseWithoutWhitespace0123456789',
  description: '用于 F09 隔离浏览器验证，不写入真实业务库。',
  documentCount: '1',
  retrievableDocumentCount: '1',
  status: '1',
  sortOrder: '0',
  editTime: '2026-07-21T08:00:00Z'
}

const fixtureDocument = {
  documentId: 'doc-90071992547409931234',
  documentName: '响应式验证文档_with_a_very_long_identifier_0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ',
  originalFileName: 'responsive-accessibility-performance-fixture-document.pdf',
  knowledgeBaseId: fixtureKnowledgeBase.id,
  knowledgeBaseName: fixtureKnowledgeBase.baseName,
  fileTypeName: 'PDF',
  fileSize: '999999999999',
  parseStatus: '3',
  strategyStatus: '3',
  indexStatus: '3',
  parseStatusName: '解析成功',
  strategyStatusName: '策略已确认',
  indexStatusName: '索引完成',
  editTime: '2026-07-21T08:00:00Z'
}

const routeTracePage = {
  pageNo: '1',
  pageSize: '20',
  totalSize: '1',
  totalPages: '1',
  records: [{
    id: 'trace-1',
    conversationId: fixtureSession.conversationId,
    question: fixtureExchange.question,
    mode: 'auto',
    routeStatus: '1',
    confidence: '0.82',
    topScopesJson: '[]',
    topTopicsJson: '[]',
    topDocumentsJson: '[]',
    createTime: '2026-07-21T08:00:00Z'
  }]
}

const systemConfigGroupMeta = {
  retrieval: {
    groupKey: 'retrieval',
    groupLabel: '检索与排序',
    groupDescription: '控制召回、通道、阈值、融合和执行边界。'
  },
  conversation: {
    groupKey: 'conversation',
    groupLabel: '对话与回答',
    groupDescription: '控制对话助手、RAG 编排和回答上下文。'
  },
  documentBuild: {
    groupKey: 'documentBuild',
    groupLabel: '文档与索引',
    groupDescription: '控制切块、内容增强、索引和 RAPTOR 构建。'
  },
  graphRag: {
    groupKey: 'graphRag',
    groupLabel: 'GraphRAG',
    groupDescription: '控制知识图谱构建和 LLM 受控增强。'
  }
}

const fixtureSystemConfig = {
  configVersion: 6,
  sourceType: 'DATABASE',
  sourceTypeLabel: '数据库配置',
  lastModifiedAt: '2026-07-23T11:55:00Z',
  categories: [{
    ...systemConfigGroupMeta.retrieval,
    categoryKey: 'retrievalWindow',
    categoryLabel: '召回与窗口',
    description: '控制各通道召回规模和候选裁剪窗口。',
    items: [{
      configKey: 'ragRuntime.vectorTopK',
      label: '向量召回数量',
      description: '每个子问题从向量通道最多召回的候选数量。',
      value: 10,
      valueType: 'INTEGER',
      controlType: 'NUMBER',
      minValue: 1,
      maxValue: 200,
      step: 1,
      displayScale: 1,
      unit: '条',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.retrieval,
    categoryKey: 'execution',
    categoryLabel: '执行与超时',
    description: '控制检索执行开关和等待时间。',
    items: [{
      configKey: 'ragRuntime.channelTimeoutMs',
      label: '单通道超时',
      description: '单个检索通道超过该时长后按超时结果收口。',
      value: 30000,
      valueType: 'LONG',
      controlType: 'DURATION',
      minValue: 100,
      maxValue: 120000,
      step: 100,
      displayScale: 1,
      unit: '毫秒',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.retrieval,
    categoryKey: 'relevance',
    categoryLabel: '相关性阈值',
    description: '控制低相关候选进入后续阶段的最低要求。',
    items: [{
      configKey: 'ragRuntime.minVectorSimilarity',
      label: '向量最低相似度',
      description: '低于该比例的向量候选不会进入融合。',
      value: 0.45,
      valueType: 'DECIMAL',
      controlType: 'PERCENTAGE',
      minValue: 0,
      maxValue: 1,
      step: 0.01,
      displayScale: 100,
      unit: '%',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }, {
      configKey: 'ragRuntime.rerankEnabled',
      label: '启用重排',
      description: '决定是否运行重排阶段。',
      value: true,
      valueType: 'BOOLEAN',
      controlType: 'CHECKBOX',
      displayScale: 1,
      unit: '',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.retrieval,
    categoryKey: 'channels',
    categoryLabel: '检索通道',
    description: '控制新会话启用的检索通道。',
    items: [{
      configKey: 'ragRuntime.keywordChannelEnabled',
      label: '启用关键词通道',
      description: '决定新会话是否并行执行关键词检索。',
      value: true,
      valueType: 'BOOLEAN',
      controlType: 'CHECKBOX',
      displayScale: 1,
      unit: '',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.retrieval,
    categoryKey: 'fusion',
    categoryLabel: '融合与排序',
    description: '控制候选融合和排序阶段的权重。',
    items: [{
      configKey: 'ragRuntime.hybrid.vectorWeight',
      label: '向量通道权重',
      description: '融合排序时向量通道分数的权重。',
      value: 1,
      valueType: 'DECIMAL',
      controlType: 'NUMBER',
      minValue: 0,
      maxValue: 5,
      step: 0.05,
      displayScale: 1,
      unit: '倍',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.conversation,
    categoryKey: 'chatAgent',
    categoryLabel: '对话助手',
    description: '控制推荐追问、调用保护和对话提示词。',
    items: [{
      configKey: 'chat.recommendationEnabled',
      label: '启用推荐追问',
      description: '主回答完成后生成可继续追问的问题。',
      value: true,
      valueType: 'BOOLEAN',
      controlType: 'CHECKBOX',
      displayScale: 1,
      unit: '',
      effectiveMode: 'NEW_CONVERSATION',
      effectiveModeLabel: '新会话生效'
    }]
  }, {
    ...systemConfigGroupMeta.documentBuild,
    categoryKey: 'chunking',
    categoryLabel: '切块策略',
    description: '控制子块、父块和智能切块的全局默认值。',
    items: [{
      configKey: 'chunk.recursiveMaxChars',
      label: '递归子块最大长度',
      description: '递归分块时单个子块允许的最大字符数。',
      value: 800,
      valueType: 'INTEGER',
      controlType: 'NUMBER',
      minValue: 100,
      maxValue: 8000,
      step: 1,
      displayScale: 1,
      unit: '字符',
      effectiveMode: 'NEW_BUILD_TASK',
      effectiveModeLabel: '新构建任务生效'
    }]
  }, {
    ...systemConfigGroupMeta.graphRag,
    categoryKey: 'graphRagBuild',
    categoryLabel: 'GraphRAG 构建',
    description: '控制构建租约、尝试次数和重试等待。',
    items: [{
      configKey: 'graphRag.build.leaseEnabled',
      label: '启用 GraphRAG 构建租约',
      description: '控制 GraphRAG 构建任务是否持有租约。',
      value: true,
      valueType: 'BOOLEAN',
      controlType: 'CHECKBOX',
      displayScale: 1,
      unit: '',
      effectiveMode: 'NEW_BUILD_TASK',
      effectiveModeLabel: '新构建任务生效'
    }]
  }]
}

const fixtureUpdatedSystemConfig = {
  ...fixtureSystemConfig,
  configVersion: 7,
  lastModifiedAt: '2026-07-23T12:00:00Z',
  categories: fixtureSystemConfig.categories.map((category) => ({
    ...category,
    items: category.items.map((item) => item.configKey === 'ragRuntime.minVectorSimilarity'
      ? { ...item, value: 0.52 }
      : item)
  }))
}

const fixtureSystemConfigHistory = {
  historyId: '90071992547409931235',
  beforeVersion: 6,
  afterVersion: 7,
  sourceType: 'MANUAL',
  sourceTypeLabel: '手动修改',
  changeNote: '提高低相关候选过滤强度',
  operatorName: 'f09-reviewer',
  changedAt: '2026-07-23T12:00:00Z',
  changeCount: 1,
  changes: [{
    configKey: 'ragRuntime.minVectorSimilarity',
    label: '向量最低相似度',
    beforeValue: 0.45,
    afterValue: 0.52,
    valueType: 'DECIMAL',
    controlType: 'PERCENTAGE',
    displayScale: 100,
    unit: '%'
  }],
  restoreFromHistoryId: ''
}

const fixtureQualityOverview = {
  windowDays: '30',
  windowLabel: '近 30 天',
  comparable: '1',
  truncated: '0',
  metrics: [
    {
      key: 'route-total', label: '知识路由总量', value: '372', valueType: 'count',
      previousValue: '68', delta: '304', higherIsBetter: '1',
      hint: 'auto 与 shadow 两种模式的路由次数合计',
      sparkline: [
        { date: '2026-07-18', value: '22' },
        { date: '2026-07-19', value: '4' },
        { date: '2026-07-20', value: '100' },
        { date: '2026-07-21', value: '109' },
        { date: '2026-07-22', value: '6' }
      ]
    },
    {
      key: 'route-success-rate', label: '路由成功率', value: '59.1', valueType: 'rate',
      previousValue: '63.4', delta: '-4.3', higherIsBetter: '1',
      hint: 'route_status 为成功的次数占路由总量之比',
      sparkline: [
        { date: '2026-07-18', value: '4.5' },
        { date: '2026-07-19', value: '100.0' },
        { date: '2026-07-20', value: '57.0' },
        { date: '2026-07-21', value: '57.8' },
        { date: '2026-07-22', value: '66.7' }
      ]
    },
    {
      key: 'route-confidence', label: '平均置信度', value: '0.568', valueType: 'score',
      previousValue: '0.556', delta: '0.012', higherIsBetter: '1',
      hint: '路由整体置信度按路由次数加权平均，取值 0 到 1',
      sparkline: [
        { date: '2026-07-18', value: '0.480' },
        { date: '2026-07-19', value: '0.598' },
        { date: '2026-07-20', value: '0.567' },
        { date: '2026-07-21', value: '0.565' },
        { date: '2026-07-22', value: '0.575' }
      ]
    },
    {
      key: 'retrieval-hit-rate', label: '检索选入率', value: '28.4', valueType: 'rate',
      previousValue: '25.1', delta: '3.3', higherIsBetter: '1',
      hint: '检索候选中最终选入 Prompt 的比例',
      sparkline: []
    }
  ],
  routeTrend: [
    {
      date: '2026-07-18', dateLabel: '07-18',
      autoCount: '21', autoSuccessRate: '0.0', autoConfidence: '0.471',
      shadowCount: '1', shadowSuccessRate: '100.0', shadowConfidence: '0.669'
    },
    {
      date: '2026-07-19', dateLabel: '07-19',
      autoCount: '4', autoSuccessRate: '100.0', autoConfidence: '0.598',
      shadowCount: '', shadowSuccessRate: '', shadowConfidence: ''
    },
    {
      date: '2026-07-20', dateLabel: '07-20',
      autoCount: '43', autoSuccessRate: '48.8', autoConfidence: '0.544',
      shadowCount: '57', shadowSuccessRate: '63.2', shadowConfidence: '0.587'
    },
    {
      date: '2026-07-21', dateLabel: '07-21',
      autoCount: '48', autoSuccessRate: '50.0', autoConfidence: '0.541',
      shadowCount: '61', shadowSuccessRate: '63.9', shadowConfidence: '0.585'
    },
    {
      date: '2026-07-22', dateLabel: '07-22',
      autoCount: '', autoSuccessRate: '', autoConfidence: '',
      shadowCount: '6', shadowSuccessRate: '66.7', shadowConfidence: '0.575'
    }
  ],
  channelProfiles: [
    { channelType: 'vector', channelName: '向量检索', recalledCount: '70', selectedCount: '22', selectionRate: '31.4', resolutionRate: '64.3', snapshotCount: '70', executionCount: '8' },
    { channelType: 'keyword', channelName: '关键词检索', recalledCount: '70', selectedCount: '19', selectionRate: '27.1', resolutionRate: '32.9', snapshotCount: '70', executionCount: '8' },
    { channelType: 'raptor', channelName: '层级结构树', recalledCount: '48', selectedCount: '11', selectionRate: '22.9', resolutionRate: '100.0', snapshotCount: '48', executionCount: '8' },
    { channelType: 'graph-rag', channelName: '知识图谱', recalledCount: '22', selectedCount: '8', selectionRate: '36.4', resolutionRate: '100.0', snapshotCount: '22', executionCount: '8' },
    { channelType: 'table', channelName: '表格检索', recalledCount: '1', selectedCount: '0', selectionRate: '0.0', resolutionRate: '0.0', snapshotCount: '1', executionCount: '8' }
  ],
  routeDecision: {
    total: '372', success: '220', lowConfidence: '152', failed: '0', successRate: '59.1'
  },
  docProcessing: {
    parseCount: '31', indexCount: '31',
    averageParseMs: '5867', averageIndexMs: '153864', indexToParseRatio: '26.2',
    items: [
      { date: '2026-07-08', dateLabel: '07-08', parseCount: '5', averageParseMs: '17158', indexCount: '5', averageIndexMs: '162622' },
      { date: '2026-07-16', dateLabel: '07-16', parseCount: '4', averageParseMs: '1389', indexCount: '4', averageIndexMs: '302644' },
      { date: '2026-07-18', dateLabel: '07-18', parseCount: '22', averageParseMs: '4115', indexCount: '22', averageIndexMs: '124822' }
    ]
  }
}

const endpointData = new Map([
  ['/api/chat/document/options', [fixtureDocument]],
  ['/api/chat/knowledge-base/options', [fixtureKnowledgeBase]],
  ['/api/chat/session/list', {
    pageNo: '1', pageSize: '20', totalSize: '1', totalPages: '1', sessions: [fixtureSession]
  }],
  ['/api/chat/session/detail', fixtureSession],
  ['/api/chat/exchange/detail', { exchange: fixtureExchange, stageTraces: fixtureStageTraces }],
  ['/api/chat/exchange/retrieval/results', fixtureRetrievalResults],
  ['/api/chat/exchange/channel/executions', fixtureChannelExecutions],
  ['/api/chat/stage/benchmarks', []],
  ['/manage/observability/quality/overview/query', fixtureQualityOverview],
  ['/manage/knowledge/base/list', [fixtureKnowledgeBase]],
  ['/manage/knowledge/base/detail', fixtureKnowledgeBase],
  ['/manage/document/page/query', {
    records: [fixtureDocument], pageNo: '1', pageSize: '20', total: '1'
  }],
  ['/manage/document/detail/query', fixtureDocument],
  ['/manage/document/strategy/plan/query', {
    planReady: true,
    parseStatus: '4',
    plan: { parentStrategies: [], childStrategies: [] }
  }],
  ['/manage/document/index/build/progress/query', null],
  ['/manage/document/parse-route/progress/query', null],
  ['/manage/document/parse-artifact/query', { records: [] }],
  ['/manage/document/chunk/query', {
    records: [], pageNo: '1', pageSize: '20', total: '0', totalPages: '1'
  }],
  ['/manage/document/rag/snapshot/query', null],
  ['/manage/document/rag/parser-diagnostic/query', null],
  ['/manage/document/rag/page-overlay/index/query', { records: [] }],
  ['/manage/document/rag/artifact/node/page/query', {
    records: [], pageNo: '1', pageSize: '10', total: '0'
  }],
  ['/manage/document/rag/artifact/node/detail/query', null],
  ['/manage/document/rag/artifact/relation/page/query', {
    records: [], pageNo: '1', pageSize: '5', total: '0'
  }],
  ['/manage/document/rag/artifact/table/window/query', {
    records: [], columns: [], pageNo: '1', pageSize: '20', totalRows: '0', totalColumns: '0'
  }],
  ['/manage/document/task/log/query', { logs: [] }],
  ['/manage/knowledge/scope/list', [{
    scopeId: 'scope-1',
    scopeName: '制度范围',
    knowledgeBaseId: fixtureKnowledgeBase.id,
    description: '范围描述'
  }]],
  ['/manage/knowledge/topic/list', [{
    topicId: 'topic-1',
    scopeId: 'scope-1',
    topicName: '请假制度',
    description: '主题描述',
    answerShape: 'FACT',
    executionPreference: 'AUTO'
  }]],
  ['/manage/knowledge/topic/document/list', []],
  ['/manage/knowledge/document/profile/detail', null],
  ['/manage/knowledge/route/trace/page/query', routeTracePage],
  ['/manage/config/current/query', fixtureSystemConfig],
  ['/manage/config/item/update', fixtureUpdatedSystemConfig],
  ['/manage/config/history/page/query', {
    pageNo: 1, pageSize: 10, total: 1, totalPages: 1, records: [fixtureSystemConfigHistory]
  }],
  ['/manage/config/history/detail/query', fixtureSystemConfigHistory],
  ['/manage/config/history/restore', { ...fixtureUpdatedSystemConfig, configVersion: 8 }]
])

function validToken() {
  const header = btoa(JSON.stringify({ alg: 'none', typ: 'JWT' }))
  const payload = btoa(JSON.stringify({ exp: 4102444800 }))
  return `${header}.${payload}.fixture`
}

export async function installMockApp(page) {
  await page.route('**/*', async (route) => {
    const url = new URL(route.request().url())
    const isApi = url.pathname.startsWith('/api/') || url.pathname.startsWith('/manage/') || url.pathname.startsWith('/admin/auth/')
    if (!isApi) {
      await route.continue()
      return
    }

    const data = endpointData.has(url.pathname) ? endpointData.get(url.pathname) : null
    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ code: '0', message: 'ok', data })
    })
  })
}

export async function setAdminAuthenticated(page, authenticated) {
  await page.evaluate(({ tokenKey, userKey, token, enabled }) => {
    if (enabled) {
      localStorage.setItem(tokenKey, token)
      localStorage.setItem(userKey, 'f09-reviewer')
    } else {
      localStorage.removeItem(tokenKey)
      localStorage.removeItem(userKey)
    }
  }, { tokenKey: ADMIN_TOKEN_KEY, userKey: ADMIN_USER_KEY, token: validToken(), enabled: authenticated })
}
