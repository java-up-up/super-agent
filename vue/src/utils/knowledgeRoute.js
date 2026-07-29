const ROUTE_MODE_LABELS = {
  auto: '自动知识路由',
  shadow: '影子路由对比'
}

const ROUTE_STATUS_ALIAS = {
  '1': 'SUCCESS',
  '2': 'LOW_CONFIDENCE',
  '3': 'FAILED',
  SUCCESS: 'SUCCESS',
  LOW_CONFIDENCE: 'LOW_CONFIDENCE',
  FAILED: 'FAILED'
}

const ROUTE_STATUS_META = {
  SUCCESS: {
    key: 'SUCCESS',
    label: '成功',
    tone: 'success'
  },
  LOW_CONFIDENCE: {
    key: 'LOW_CONFIDENCE',
    label: '低置信',
    tone: 'warning'
  },
  FAILED: {
    key: 'FAILED',
    label: '失败',
    tone: 'danger'
  }
}

function asString(value) {
  if (value == null) {
    return ''
  }
  return String(value).trim()
}

function toNumber(value) {
  const num = Number(value)
  return Number.isFinite(num) ? num : null
}

function parseCandidateList(rawValue) {
  const normalized = asString(rawValue)
  if (!normalized) {
    return []
  }

  try {
    const parsed = JSON.parse(normalized)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function resolveRouteStatusMeta(value) {
  const alias = ROUTE_STATUS_ALIAS[asString(value)] || 'FAILED'
  return ROUTE_STATUS_META[alias] || ROUTE_STATUS_META.FAILED
}

function resolveConfidenceBand(value) {
  const numeric = toNumber(value)
  if (numeric == null) {
    return {
      label: '置信度未记录',
      tone: 'neutral'
    }
  }
  return {
    label: '路由置信度',
    tone: 'neutral'
  }
}

function normalizeCandidate(item = {}) {
  const scoreNumber = toNumber(item.score)
  return {
    ...item,
    scoreNumber,
    scoreText: scoreNumber == null ? '-' : scoreNumber.toFixed(4)
  }
}

function average(numbers) {
  if (!numbers.length) {
    return null
  }
  const total = numbers.reduce((sum, value) => sum + value, 0)
  return total / numbers.length
}

export function formatRouteMode(value) {
  return ROUTE_MODE_LABELS[asString(value)] || asString(value) || '未知路由模式'
}

function resolveRouteModeTone(value) {
  const mode = asString(value)
  if (mode === 'auto' || mode === 'shadow') {
    return mode
  }
  return 'neutral'
}

export function normalizeRouteTrace(record = {}) {
  const scopes = parseCandidateList(record.topScopesJson).map(normalizeCandidate)
  const topics = parseCandidateList(record.topTopicsJson).map(normalizeCandidate)
  const documents = parseCandidateList(record.topDocumentsJson).map(normalizeCandidate)
  const confidenceNumber = toNumber(record.confidence)
  const statusMeta = resolveRouteStatusMeta(record.routeStatus)
  const selectedDocumentId = asString(record.selectedDocumentId)
  const selectedDocument = selectedDocumentId
    ? documents.find((item) => asString(item.documentId) === selectedDocumentId) || null
    : null
  const topDocument = documents[0] || null
  const createTimeNumber = toNumber(record.createTime) || 0
  const hitSelectedDocument = asString(record.hitSelectedDocument)
  const mode = asString(record.mode)

  return {
    ...record,
    mode,
    modeLabel: formatRouteMode(mode),
    modeTone: resolveRouteModeTone(mode),
    scopes,
    topics,
    documents,
    topDocument,
    selectedDocumentId,
    selectedDocument,
    confidenceNumber,
    confidenceText: confidenceNumber == null ? '-' : confidenceNumber.toFixed(4),
    confidenceBand: resolveConfidenceBand(confidenceNumber),
    statusKey: statusMeta.key,
    statusLabel: statusMeta.label,
    statusTone: statusMeta.tone,
    reasonText: asString(record.errorMsg) || asString(topDocument?.reason),
    createTimeNumber,
    hitTop3: hitSelectedDocument === '1',
    missedTop3: hitSelectedDocument === '0',
    candidateDocumentCount: documents.length,
    candidateTopicCount: topics.length,
    candidateScopeCount: scopes.length,
    // Widening is a backend observation. Do not infer it from score/count.
    lowConfidenceWidened: record.lowConfidenceWidened === true || asString(record.lowConfidenceWidened) === '1'
  }
}

export function buildRouteTraceLookup(records = []) {
  return records.reduce((lookup, item) => {
    const normalized = normalizeRouteTrace(item)
    const exchangeId = asString(normalized.exchangeId)
    if (!exchangeId) {
      return lookup
    }
    const existing = lookup[exchangeId]
    if (!existing) {
      lookup[exchangeId] = normalized
      return lookup
    }
    if (normalized.mode === 'auto' && existing.mode !== 'auto') {
      lookup[exchangeId] = normalized
      return lookup
    }
    if (normalized.createTimeNumber >= existing.createTimeNumber) {
      lookup[exchangeId] = normalized
    }
    return lookup
  }, {})
}

export function buildChatRouteExplain(record) {
  if (!record) {
    return null
  }

  const trace = normalizeRouteTrace(record)
  const topDocuments = trace.documents.slice(0, 5)
  const scopePreview = trace.scopes.slice(0, 3)
  const topicPreview = trace.topics.slice(0, 3)
  const notes = []
  let summary = ''

  if (trace.mode === 'auto') {
    summary = trace.topDocument
      ? `路由响应记录了 ${trace.candidateDocumentCount} 份候选文档，当前 Top1 是「${trace.topDocument.documentName || trace.topDocument.documentId}」。`
      : '路由响应没有记录显式候选文档。'

    if (trace.lowConfidenceWidened) {
      notes.push('接口显式记录本轮已扩展候选范围。')
    }
    if (!trace.documents.length) {
      notes.push('接口未返回显式候选文档。')
    }
    if (trace.reasonText) {
      notes.push(`路由依据：${trace.reasonText}`)
    }
  } else if (trace.mode === 'shadow') {
    summary = trace.topDocument
      ? `影子路由响应的 Top1 是「${trace.topDocument.documentName || trace.topDocument.documentId}」。`
      : '影子路由响应没有记录显式候选文档。'

    if (trace.hitTop3) {
      notes.push('接口记录：影子路由 Top3 包含当前所选文档。')
    }
    if (trace.missedTop3) {
      notes.push('接口记录：影子路由 Top3 不包含当前所选文档。')
    }
    if (trace.reasonText) {
      notes.push(`影子路由依据：${trace.reasonText}`)
    }
  } else {
    return null
  }

  return {
    ...trace,
    summary,
    notes,
    topDocuments,
    scopePreview,
    topicPreview
  }
}

export function summarizeRouteTraceRecords(records = []) {
  const normalized = records.map(normalizeRouteTrace)
  const autoCount = normalized.filter((item) => item.mode === 'auto').length
  const shadowCount = normalized.filter((item) => item.mode === 'shadow').length
  const successCount = normalized.filter((item) => item.statusKey === 'SUCCESS').length
  const lowConfidenceCount = normalized.filter((item) => item.statusKey === 'LOW_CONFIDENCE').length
  const failedCount = normalized.filter((item) => item.statusKey === 'FAILED').length
  const highConfidenceCount = normalized.filter((item) => (item.confidenceNumber || 0) >= 0.8).length
  const confidenceValues = normalized
    .map((item) => item.confidenceNumber)
    .filter((item) => item != null)
  const averageConfidence = average(confidenceValues)
  const shadowSamples = normalized.filter((item) => item.mode === 'shadow' && (item.hitTop3 || item.missedTop3))
  const shadowHitCount = shadowSamples.filter((item) => item.hitTop3).length
  const shadowHitRate = shadowSamples.length ? (shadowHitCount / shadowSamples.length) * 100 : null
  const widenedCount = normalized.filter((item) => item.lowConfidenceWidened).length
  const avgDocumentCount = average(normalized.map((item) => item.candidateDocumentCount))
  const avgTopicCount = average(normalized.map((item) => item.candidateTopicCount))
  const avgScopeCount = average(normalized.map((item) => item.candidateScopeCount))
  const uniqueTopDocuments = new Set(
    normalized
      .map((item) => item.topDocument?.documentId || item.topDocument?.documentName || '')
      .filter(Boolean)
  )
  const successRate = normalized.length ? (successCount / normalized.length) * 100 : null
  const lowConfidenceRate = normalized.length ? ((lowConfidenceCount + failedCount) / normalized.length) * 100 : null

  return {
    total: normalized.length,
    autoCount,
    shadowCount,
    successCount,
    lowConfidenceCount,
    failedCount,
    highConfidenceCount,
    widenedCount,
    uniqueTopDocumentCount: uniqueTopDocuments.size,
    averageConfidenceText: averageConfidence == null ? '-' : averageConfidence.toFixed(4),
    averageDocumentCountText: avgDocumentCount == null ? '-' : avgDocumentCount.toFixed(1),
    averageTopicCountText: avgTopicCount == null ? '-' : avgTopicCount.toFixed(1),
    averageScopeCountText: avgScopeCount == null ? '-' : avgScopeCount.toFixed(1),
    successRateText: successRate == null ? '-' : `${successRate.toFixed(1)}%`,
    lowConfidenceRateText: lowConfidenceRate == null ? '-' : `${lowConfidenceRate.toFixed(1)}%`,
    shadowHitRateText: shadowHitRate == null ? '-' : `${shadowHitRate.toFixed(1)}%`
  }
}

export function buildTopDocumentDistribution(records = []) {
  const rows = records
    .map(normalizeRouteTrace)
    .filter((item) => item.topDocument)
    .reduce((map, item) => {
      const documentId = item.topDocument.documentId || item.topDocument.documentName || 'unknown'
      const existing = map.get(documentId) || {
        documentId,
        documentName: item.topDocument.documentName || item.topDocument.documentId || '未知文档',
        count: 0,
        confidenceTotal: 0,
        confidenceCount: 0,
        lowConfidenceCount: 0
      }
      existing.count += 1
      if (item.confidenceNumber != null) {
        existing.confidenceTotal += item.confidenceNumber
        existing.confidenceCount += 1
      }
      if (item.statusKey !== 'SUCCESS') {
        existing.lowConfidenceCount += 1
      }
      map.set(documentId, existing)
      return map
    }, new Map())

  return [...rows.values()]
    .map((item) => ({
      ...item,
      averageConfidenceText: item.confidenceCount ? (item.confidenceTotal / item.confidenceCount).toFixed(4) : '-'
    }))
    .sort((left, right) => right.count - left.count)
    .slice(0, 6)
}
