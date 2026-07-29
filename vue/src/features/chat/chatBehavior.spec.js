import { describe, expect, it } from 'vitest'
import {
  isNearScrollBottom,
  mergeAssistantStreamEvent,
  projectSourceReferences,
  shouldApplyStreamEvent,
  shouldSubmitComposerEvent
} from './chatBehavior'

describe('chat behavior contracts', () => {
  it('submits Enter only after IME composition has ended', () => {
    expect(shouldSubmitComposerEvent({ key: 'Enter', shiftKey: false, isComposing: false, keyCode: 13 })).toBe(true)
    expect(shouldSubmitComposerEvent({ key: 'Enter', shiftKey: false, isComposing: true, keyCode: 13 })).toBe(false)
    expect(shouldSubmitComposerEvent({ key: 'Enter', shiftKey: false, isComposing: false, keyCode: 229 })).toBe(false)
    expect(shouldSubmitComposerEvent({ key: 'Enter', shiftKey: true, isComposing: false, keyCode: 13 })).toBe(false)
  })

  it('follows output only while the reader remains near the bottom', () => {
    expect(isNearScrollBottom({ scrollHeight: 1200, scrollTop: 620, clientHeight: 500 })).toBe(true)
    expect(isNearScrollBottom({ scrollHeight: 1200, scrollTop: 300, clientHeight: 500 })).toBe(false)
  })

  it('rejects chunks from a stream token that is no longer active', () => {
    expect(shouldApplyStreamEvent('stream-2', 'stream-2')).toBe(true)
    expect(shouldApplyStreamEvent(null, 'stream-2')).toBe(false)
    expect(shouldApplyStreamEvent('stream-3', 'stream-2')).toBe(false)
  })

  it('merges SSE events without mutating the previous assistant message', () => {
    const original = { content: 'A', thinkingSteps: ['plan'], references: [], recommendations: [], status: 'RUNNING' }
    const withText = mergeAssistantStreamEvent(original, { type: 'text', content: 'B', timestamp: '2026-07-21T10:00:00Z' })
    const withDuplicateThinking = mergeAssistantStreamEvent(withText, { type: 'thinking', content: 'plan' })
    const withReferences = mergeAssistantStreamEvent(withDuplicateThinking, {
      type: 'reference',
      content: [{ citationIdentity: 'CHUNK:1:2', documentName: '培训手册' }]
    })
    const failed = mergeAssistantStreamEvent(withReferences, { type: 'error', content: '网络中断' })

    expect(original.content).toBe('A')
    expect(withText.content).toBe('AB')
    expect(withDuplicateThinking.thinkingSteps).toEqual(['plan'])
    expect(withReferences.references).toHaveLength(1)
    expect(failed).toMatchObject({ status: 'FAILED', errorMessage: '网络中断' })
  })

  it('projects only backend references with a stable identity and preserves backend order', () => {
    const projected = projectSourceReferences([
      { citationIdentity: 'CHUNK:8:12', documentName: '员工手册', pageNo: 3, quoteText: '试用期为三个月。' },
      { documentName: '缺少身份的候选' },
      { referenceId: 'web-2', title: '公开资料', url: 'https://example.com/source' },
      { citationIdentity: 'CTX:1', citationEvidenceType: 'CONTEXT_ONLY', contextOnly: true }
    ])

    expect(projected).toHaveLength(2)
    expect(projected.map((item) => item.index)).toEqual([1, 3])
    expect(projected.map((item) => item.identity)).toEqual(['CHUNK:8:12', 'web-2'])
    expect(projected[0]).toMatchObject({ title: '员工手册', pageNo: '3', snippet: '试用期为三个月。' })
  })
})
