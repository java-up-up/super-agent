import { describe, expect, it } from 'vitest'
import { buildChatRouteExplain, normalizeRouteTrace } from './knowledgeRoute'

describe('knowledge route observation projection', () => {
  it('does not infer a widened route from confidence and candidate count', () => {
    const trace = normalizeRouteTrace({
      mode: 'auto',
      confidence: '0.42',
      routeStatus: '2',
      topDocumentsJson: JSON.stringify([
        { documentId: '1' },
        { documentId: '2' },
        { documentId: '3' },
        { documentId: '4' },
        { documentId: '5' }
      ])
    })

    expect(trace.lowConfidenceWidened).toBe(false)
    expect(trace.confidenceBand).toEqual({ label: '路由置信度', tone: 'neutral' })
  })

  it('keeps explicit backend widening facts when provided', () => {
    const trace = normalizeRouteTrace({
      mode: 'auto',
      confidence: '0.42',
      routeStatus: '2',
      lowConfidenceWidened: true
    })

    expect(trace.lowConfidenceWidened).toBe(true)
  })

  it('projects route modes to stable visual tones without reinterpreting routing', () => {
    expect(normalizeRouteTrace({ mode: 'auto' }).modeTone).toBe('auto')
    expect(normalizeRouteTrace({ mode: 'shadow' }).modeTone).toBe('shadow')
    expect(normalizeRouteTrace({ mode: 'manual' }).modeTone).toBe('neutral')
  })

  it('describes shadow hits as response facts without inventing a cause', () => {
    const explanation = buildChatRouteExplain({
      mode: 'shadow',
      routeStatus: '1',
      selectedDocumentId: 'doc-1',
      hitSelectedDocument: '0',
      topDocumentsJson: JSON.stringify([{ documentId: 'doc-2', documentName: '候选文档.md' }])
    })

    expect(explanation.notes).toContain('接口记录：影子路由 Top3 不包含当前所选文档。')
    expect(explanation.notes.join('')).not.toContain('元数据仍需补强')
  })
})
