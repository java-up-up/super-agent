const DEFAULT_SCROLL_THRESHOLD = 96

function asText(value) {
  return value == null ? '' : String(value).trim()
}

export function shouldSubmitComposerEvent(event) {
  if (!event || event.key !== 'Enter' || event.shiftKey) return false
  return !event.isComposing && event.keyCode !== 229
}

export function isNearScrollBottom(element, threshold = DEFAULT_SCROLL_THRESHOLD) {
  if (!element) return true
  const distance = Number(element.scrollHeight) - Number(element.scrollTop) - Number(element.clientHeight)
  return distance <= threshold
}

export function shouldApplyStreamEvent(activeToken, eventToken) {
  return activeToken != null && eventToken != null && activeToken === eventToken
}

export function mergeAssistantStreamEvent(message, event) {
  const next = {
    ...message,
    thinkingSteps: Array.isArray(message?.thinkingSteps) ? [...message.thinkingSteps] : [],
    references: Array.isArray(message?.references) ? [...message.references] : [],
    recommendations: Array.isArray(message?.recommendations) ? [...message.recommendations] : []
  }

  if (event?.type === 'text') next.content = `${next.content || ''}${event.content || ''}`
  if (event?.type === 'thinking' && event.content && !next.thinkingSteps.includes(event.content)) {
    next.thinkingSteps.push(event.content)
  }
  if (event?.type === 'reference') next.references = Array.isArray(event.content) ? [...event.content] : []
  if (event?.type === 'recommend') next.recommendations = Array.isArray(event.content) ? [...event.content] : []
  if (event?.type === 'status') next.statusText = event.content || ''
  if (event?.type === 'error') {
    next.errorMessage = event.content || '对话执行失败'
    next.status = 'FAILED'
  }
  next.updatedAt = event?.timestamp || new Date().toISOString()
  return next
}

export function sourceReferenceIdentity(reference) {
  return asText(reference?.citationIdentity) || asText(reference?.referenceId)
}

export function projectSourceReferences(references) {
  if (!Array.isArray(references)) return []

  return references.flatMap((reference, index) => {
    const identity = sourceReferenceIdentity(reference)
    const evidenceType = asText(reference?.citationEvidenceType)
    if (!identity || reference?.contextOnly || evidenceType === 'CONTEXT_ONLY') return []

    return [{
      index: index + 1,
      identity,
      evidenceType,
      title: asText(reference?.documentName) || asText(reference?.title) || `来源 ${index + 1}`,
      snippet: asText(reference?.quoteText) || asText(reference?.snippet),
      url: asText(reference?.url),
      documentId: asText(reference?.documentId),
      knowledgeScopeCode: asText(reference?.knowledgeScopeCode),
      knowledgeScopeName: asText(reference?.knowledgeScopeName),
      sectionPath: asText(reference?.sectionPath) || asText(reference?.canonicalPath),
      pageNo: asText(reference?.pageNo),
      pageRange: asText(reference?.pageRange),
      chunkId: asText(reference?.chunkId),
      chunkNo: asText(reference?.chunkNo),
      channel: asText(reference?.channel),
      sourceType: asText(reference?.sourceType),
      score: Number.isFinite(Number(reference?.score)) ? Number(reference.score) : null
    }]
  })
}
