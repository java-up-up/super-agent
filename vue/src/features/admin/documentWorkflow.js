import { buildPipelineStepPayload } from '@/utils/documentStrategyPipeline'

export const SUPPORTED_DOCUMENT_EXTENSIONS = Object.freeze([
  'pdf', 'doc', 'docx', 'txt', 'md', 'html', 'htm'
])

export function resolveWorkflowStepTone(kind, rawState) {
  const state = String(rawState || '').toLowerCase()
  if (state === 'failed' || state === 'error') return 'danger'
  if (state === 'done' || state === 'completed') return 'success'
  if (kind === 'build' && (state === 'ready' || state === 'current')) return 'running'
  if (kind === 'confirm' && (state === 'ready' || state === 'current')) return 'primary'
  if (state === 'blocked') return 'waiting'
  return 'default'
}

function normalizedExtension(fileName) {
  const name = String(fileName || '').trim().toLowerCase()
  const separatorIndex = name.lastIndexOf('.')
  return separatorIndex >= 0 ? name.slice(separatorIndex + 1) : ''
}

export function validateUploadDraft(draft = {}) {
  const errors = []
  if (!draft.file) {
    errors.push('请选择要上传的文档。')
  } else {
    const extension = normalizedExtension(draft.file.name)
    if (!SUPPORTED_DOCUMENT_EXTENSIONS.includes(extension)) {
      errors.push(`不支持 .${extension || '未知'} 文件，请选择 PDF、DOC、DOCX、TXT、MD 或 HTML 文件。`)
    }
  }
  return { valid: errors.length === 0, errors }
}

export function buildUploadRequest(draft = {}, operatorId = '10001') {
  const validation = validateUploadDraft(draft)
  if (!validation.valid) {
    throw new Error(validation.errors[0])
  }
  return {
    file: draft.file,
    documentName: String(draft.documentName || '').trim(),
    operatorId: String(operatorId || ''),
    knowledgeScopeCode: String(draft.knowledgeScopeCode || '').trim(),
    knowledgeScopeName: String(draft.knowledgeScopeName || '').trim(),
    businessCategory: String(draft.businessCategory || '').trim(),
    documentTags: String(draft.documentTags || '').trim()
  }
}

export function validateStrategyDraft(draft = {}) {
  const errors = []
  if (!String(draft.documentId || '').trim()) errors.push('缺少文档标识。')
  if (!String(draft.basePlanId || '').trim()) errors.push('当前还没有可确认的策略方案。')
  if (!Array.isArray(draft.parentTypes) || draft.parentTypes.length === 0) errors.push('父块流水线至少需要一个策略。')
  if (!Array.isArray(draft.childTypes) || draft.childTypes.length === 0) errors.push('子块流水线至少需要一个策略。')
  return { valid: errors.length === 0, errors }
}

export function buildStrategyConfirmRequest(draft = {}, strategyLibrary) {
  const validation = validateStrategyDraft(draft)
  if (!validation.valid) {
    throw new Error(validation.errors[0])
  }
  return {
    documentId: String(draft.documentId),
    basePlanId: String(draft.basePlanId),
    adjustNote: String(draft.adjustNote || '').trim(),
    operatorId: String(draft.operatorId || ''),
    parentSteps: buildPipelineStepPayload(draft.parentTypes, strategyLibrary),
    childSteps: buildPipelineStepPayload(draft.childTypes, strategyLibrary)
  }
}

export function buildIndexRequest(input = {}) {
  if (!input.confirmed || !String(input.currentPlanId || '').trim()) {
    throw new Error('请先确认策略方案。')
  }
  if (input.dirty) {
    throw new Error('当前流水线有未确认的改动。')
  }
  if (!String(input.documentId || '').trim()) {
    throw new Error('缺少文档标识。')
  }
  return {
    documentId: String(input.documentId),
    planId: String(input.currentPlanId),
    operatorId: String(input.operatorId || '')
  }
}

export function mergeIncrementalLogs(previousLogs = [], incomingLogs = []) {
  const byId = new Map()
  previousLogs.concat(incomingLogs).forEach((item) => {
    const id = String(item?.id ?? '').trim()
    if (id) byId.set(id, item)
  })
  return Array.from(byId.values()).sort((left, right) => {
    const leftTime = new Date(left?.createTime || 0).getTime()
    const rightTime = new Date(right?.createTime || 0).getTime()
    if (leftTime !== rightTime) return leftTime - rightTime
    return Number(left?.id || 0) - Number(right?.id || 0)
  })
}

export function latestIncrementalLogId(logs = []) {
  return logs
    .map((item) => Number(item?.id || 0))
    .filter((id) => Number.isFinite(id) && id > 0)
    .reduce((max, id) => Math.max(max, id), 0) || null
}

export function createSubmissionGuard() {
  let running = false
  return {
    pending: () => running,
    async run(task) {
      if (running) return { skipped: true }
      running = true
      try {
        return await task()
      } finally {
        running = false
      }
    }
  }
}
