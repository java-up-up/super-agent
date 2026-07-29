import { describe, expect, it } from 'vitest'
import {
  buildIndexRequest,
  buildUploadRequest,
  buildStrategyConfirmRequest,
  createSubmissionGuard,
  mergeIncrementalLogs,
  resolveWorkflowStepTone,
  validateStrategyDraft,
  validateUploadDraft
} from './documentWorkflow'

describe('F06 document workflow contracts', () => {
  it('separates confirmation, build execution, and failure visual roles', () => {
    expect(resolveWorkflowStepTone('confirm', 'completed')).toBe('success')
    expect(resolveWorkflowStepTone('confirm', 'ready')).toBe('primary')
    expect(resolveWorkflowStepTone('build', 'ready')).toBe('running')
    expect(resolveWorkflowStepTone('build', 'current')).toBe('running')
    expect(resolveWorkflowStepTone('build', 'completed')).toBe('success')
    expect(resolveWorkflowStepTone('build', 'failed')).toBe('danger')
    expect(resolveWorkflowStepTone('build', 'locked')).toBe('default')
  })

  it('validates supported uploads without discarding the selected draft', () => {
    const supported = new File(['policy'], 'policy.pdf', { type: 'application/pdf' })
    expect(validateUploadDraft({ file: supported })).toEqual({ valid: true, errors: [] })

    const unsupported = new File(['archive'], 'policy.zip', { type: 'application/zip' })
    expect(validateUploadDraft({ file: unsupported })).toEqual({
      valid: false,
      errors: ['不支持 .zip 文件，请选择 PDF、DOC、DOCX、TXT、MD 或 HTML 文件。']
    })

    const upgradedOnly = new File(['sheet'], 'knowledge.xlsx', { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    expect(validateUploadDraft({ file: upgradedOnly })).toEqual({
      valid: false,
      errors: ['不支持 .xlsx 文件，请选择 PDF、DOC、DOCX、TXT、MD 或 HTML 文件。']
    })

    expect(buildUploadRequest({
      file: supported,
      documentName: '  人事制度  ',
      knowledgeScopeCode: ' hr ',
      knowledgeScopeName: ' 人事域 ',
      businessCategory: ' 规则 ',
      documentTags: ' 年假, 请假 '
    }, '10001')).toMatchObject({
      file: supported,
      documentName: '人事制度',
      knowledgeScopeCode: 'hr',
      knowledgeScopeName: '人事域',
      businessCategory: '规则',
      documentTags: '年假, 请假',
      operatorId: '10001'
    })
  })

  it('keeps draft validation separate from strategy commit payload construction', () => {
    const draft = {
      documentId: 'doc-1',
      basePlanId: 'plan-1',
      adjustNote: '  keep headings  ',
      parentTypes: ['1', '2'],
      childTypes: ['3', '2'],
      operatorId: '10001'
    }
    expect(validateStrategyDraft(draft)).toEqual({ valid: true, errors: [] })
    expect(buildStrategyConfirmRequest(draft)).toEqual({
      documentId: 'doc-1',
      basePlanId: 'plan-1',
      adjustNote: 'keep headings',
      operatorId: '10001',
      parentSteps: [
        { stepNo: '1', strategyType: '1' },
        { stepNo: '2', strategyType: '2' }
      ],
      childSteps: [
        { stepNo: '1', strategyType: '3' },
        { stepNo: '2', strategyType: '2' }
      ]
    })
    expect(validateStrategyDraft({ ...draft, childTypes: [] })).toEqual({
      valid: false,
      errors: ['子块流水线至少需要一个策略。']
    })
  })

  it('refuses to build from an unconfirmed or changed strategy draft', () => {
    expect(() => buildIndexRequest({ documentId: 'doc-1', currentPlanId: '', confirmed: false })).toThrow('请先确认策略方案。')
    expect(() => buildIndexRequest({ documentId: 'doc-1', currentPlanId: 'plan-1', confirmed: true, dirty: true })).toThrow('当前流水线有未确认的改动。')
    expect(buildIndexRequest({ documentId: 'doc-1', currentPlanId: 'plan-1', confirmed: true, dirty: false, operatorId: '10001' })).toEqual({
      documentId: 'doc-1',
      planId: 'plan-1',
      operatorId: '10001'
    })
  })

  it('merges incremental poll logs by stable id and chronological order', () => {
    const previous = [
      { id: '2', createTime: '2026-07-21T08:00:02Z', content: 'old' },
      { id: '1', createTime: '2026-07-21T08:00:01Z', content: 'first' }
    ]
    const incoming = [
      { id: '2', createTime: '2026-07-21T08:00:02Z', content: 'updated' },
      { id: '3', createTime: '2026-07-21T08:00:03Z', content: 'last' }
    ]
    expect(mergeIncrementalLogs(previous, incoming).map((item) => [item.id, item.content])).toEqual([
      ['1', 'first'], ['2', 'updated'], ['3', 'last']
    ])
  })

  it('guards duplicate commits and releases after both success and failure', async () => {
    const guard = createSubmissionGuard()
    let release
    const pending = guard.run(() => new Promise((resolve) => { release = resolve }))
    expect(guard.pending()).toBe(true)
    await expect(guard.run(() => Promise.resolve('duplicate'))).resolves.toEqual({ skipped: true })
    release('done')
    await expect(pending).resolves.toBe('done')
    expect(guard.pending()).toBe(false)
    await expect(guard.run(() => Promise.reject(new Error('failed')))).rejects.toThrow('failed')
    expect(guard.pending()).toBe(false)
  })
})
