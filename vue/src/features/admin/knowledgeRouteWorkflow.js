function text(value) {
  return String(value ?? '').trim()
}

function pickText(input, fields) {
  return Object.fromEntries(fields.map((field) => [field, text(input?.[field])]))
}

export function buildScopeRequest(input = {}) {
  const request = pickText(input, [
    'id', 'scopeCode', 'scopeName', 'parentScopeCode', 'description', 'aliases', 'examples', 'sortOrder', 'operatorId'
  ])
  if (request.parentScopeCode === '__none__') request.parentScopeCode = ''
  if (!request.scopeCode) throw new Error('知识范围编码不能为空。')
  if (!request.scopeName) throw new Error('知识范围名称不能为空。')
  return request
}

export function buildTopicRequest(input = {}) {
  const request = pickText(input, [
    'id', 'topicCode', 'topicName', 'scopeCode', 'description', 'aliases', 'examples', 'answerShape', 'executionPreference', 'sortOrder', 'operatorId'
  ])
  if (!request.topicCode) throw new Error('知识主题编码不能为空。')
  if (!request.topicName) throw new Error('知识主题名称不能为空。')
  if (!request.scopeCode) throw new Error('请选择知识主题所属范围。')
  return request
}

export function buildRelationRequest(input = {}) {
  const request = pickText(input, [
    'topicCode', 'documentId', 'relationScore', 'relationSource', 'reason', 'operatorId'
  ])
  if (!request.topicCode) throw new Error('请选择知识主题。')
  if (!request.documentId) throw new Error('请选择关联文档。')
  const score = Number(request.relationScore)
  if (!Number.isFinite(score) || score < 0 || score > 1) throw new Error('关联分数必须在 0 到 1 之间。')
  return request
}

export function createRouteTraceTarget(context = {}) {
  const query = {
    exchangeId: text(context.exchangeId),
    source: 'knowledge-route'
  }
  if (text(context.topicCode)) query.topicCode = text(context.topicCode)
  return {
    name: 'AdminObservabilityDetail',
    params: { conversationId: text(context.conversationId) },
    query
  }
}
