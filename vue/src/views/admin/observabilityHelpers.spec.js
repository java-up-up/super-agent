import { describe, expect, it } from 'vitest'
import {
  buildExchangeSignalMap, buildExchangeStages, buildExplicitCitationView,
  buildFusionCandidateFlow, buildFusionTraceGroups, buildRetrievalFlowSummary
} from './observabilityHelpers'

describe('F07 explicit citation projection', () => {
  it('keeps explicit citations empty when binding recorded no legal token', () => {
    const view = buildExplicitCitationView([
      {
        stageCode: 'CITATION_BINDING',
        snapshot: {
          authority: 'EXPLICIT_REFERENCE_TOKEN',
          parsedTokenCount: 0,
          bindings: [],
          explicitCitationIdentities: [],
          sourceSnapshotIdentities: []
        }
      },
      {
        stageCode: 'CITATION_REPAIR',
        snapshot: { finalCitations: [{ referenceId: '99' }] }
      }
    ], [])

    expect(view.explicitCitationIdentities).toEqual([])
    expect(view.finalReferences).toEqual([])
    expect(view.summary.finalReferenceCount).toBe(0)
  })

  it('preserves backend binding and source-snapshot order', () => {
    const view = buildExplicitCitationView([{
      stageCode: 'CITATION_BINDING',
      snapshot: {
        authority: 'EXPLICIT_REFERENCE_TOKEN',
        parsedTokenCount: 3,
        bindings: [
          { token: '[2]', identity: 'source-b', bindingDisposition: 'BOUND_FIRST_OCCURRENCE' },
          { token: '[1]', identity: 'source-a', bindingDisposition: 'BOUND_FIRST_OCCURRENCE' }
        ],
        explicitCitationIdentities: ['source-b', 'source-a'],
        sourceSnapshotIdentities: ['source-b', 'source-a'],
        conservationStatus: 'CONSERVED'
      }
    }], [
      { referenceId: '2', citationIdentity: 'source-b', documentName: 'B' },
      { referenceId: '1', citationIdentity: 'source-a', documentName: 'A' }
    ])

    expect(view.explicitCitationIdentities).toEqual(['source-b', 'source-a'])
    expect(view.finalReferences.map((item) => item.documentName)).toEqual(['B', 'A'])
    expect(view.conservationStatus).toBe('CONSERVED')
  })
})

describe('F07 exchange key-result projection', () => {
  it('projects one concise diagnostic flow without inventing health scores', () => {
    const session = {
      conversationId: 'conversation-1',
      chatMode: 'DOCUMENT',
      selectedDocumentName: '客服平台上线与运营管理手册.md'
    }
    const exchange = {
      exchangeId: 'exchange-1',
      question: '检索命中率突然下降的可能原因有哪些？',
      answer: '回答正文',
      status: 'COMPLETED',
      firstResponseTimeMs: 20355,
      totalResponseTimeMs: 22904,
      references: [{}],
      recommendations: [{}, {}, {}],
      usedTools: ['graph-rag', 'raptor'],
      thinkingSteps: ['规划检索范围', '扩展结构锚点'],
      debugTrace: {
        chatMode: 'DOCUMENT',
        executionMode: 'STANDARD',
        originalQuestion: '检索命中率突然下降的可能原因有哪些？',
        retrievalQuestion: '检索命中率下降原因',
        usedChannels: ['vector', 'keyword'],
        limitStats: {
          modelCallsUsed: 2,
          modelCallsRunLimit: 8,
          toolCallsUsed: 5,
          toolCallsRunLimit: 6,
          limitTriggered: false
        },
        modelUsageTraces: [
          { totalTokens: 400, estimatedCost: 0.0018 },
          { totalTokens: 558, estimatedCost: 0.0025 }
        ]
      }
    }

    const signalMap = buildExchangeSignalMap(session, exchange)

    expect(signalMap.inputs).toEqual([
      expect.objectContaining({ key: 'scope', stageKey: 'request', value: '客服平台上线与运营管理手册.md' }),
      expect.objectContaining({ key: 'understanding', stageKey: 'planning', value: '检索命中率下降原因' }),
      expect.objectContaining({ key: 'retrieval', stageKey: 'execution', value: '2 个检索通道', details: ['向量检索', '关键词检索'] })
    ])
    expect(signalMap.evidence).toEqual(expect.objectContaining({ stageKey: 'outcome', count: 1, state: 'available' }))
    expect(signalMap.answer).toEqual(expect.objectContaining({ stageKey: 'generation', status: '已完成', latency: '20.4 s', preview: '回答正文' }))
    expect(signalMap.resources).toEqual([
      { label: '模型调用', value: '2 次' },
      { label: '总 Token', value: '958' },
      { label: '预估成本', value: '¥ 0.0043' },
      { label: '调用限制', value: '未触发', tone: 'neutral' }
    ])
    expect(signalMap).not.toHaveProperty('healthScore')

    const stages = buildExchangeStages(session, exchange)
    expect(stages.map((stage) => stage.key)).toEqual(['outcome', 'execution', 'planning', 'request', 'generation', 'usage'])
  })
})

describe('F07 retrieval fusion workbench projection', () => {
  const results = [
    {
      candidateId: 'candidate-vector-selected', subQuestionIndex: 1, subQuestion: '如何定位检索下降原因？',
      channelType: 'vector', channelRank: 2, rrfRank: 2, hybridScore: 0.92, rerankScore: 0.96,
      documentId: 'doc-1', documentName: '运营管理手册.md', chunkId: 'chunk-1', parentBlockNo: 9, sectionPath: '9.3.2 为什么不直接把父块拿去做召回',
      isSelected: true, finalRank: 1, gatePassed: true, selectionReason: 'SELECTED_BY_RANK'
    },
    {
      candidateId: 'candidate-keyword-neutral', subQuestionIndex: 1, subQuestion: '如何定位检索下降原因？',
      channelType: 'keyword', channelRank: 1, rrfRank: 1, hybridScore: 0.84, rerankScore: 0.71,
      documentId: 'doc-1', documentName: '运营管理手册.md', chunkId: 'chunk-2', chunkTextPreview: '命中率下降时先检查召回范围和质量闸门。',
      isSelected: false, gatePassed: true
    },
    {
      candidateId: 'candidate-vector-filtered', subQuestionIndex: 1, subQuestion: '如何定位检索下降原因？',
      channelType: 'vector', channelRank: 1, rrfRank: 3, hybridScore: 0.52,
      documentId: 'doc-2', documentName: '告警处理手册.md', chunkId: 'chunk-3', chunkNo: 4, parentBlockNo: 27,
      isSelected: false, gatePassed: false, filteredReason: 'BELOW_GATE'
    }
  ]
  const executions = [
    { id: 'exec-vector', subQuestionIndex: 1, channelType: 'vector', executionState: 1, recalledCount: 6, acceptedCount: 4, durationMs: 320, avgScore: 0.78 },
    { id: 'exec-keyword', subQuestionIndex: 1, channelType: 'keyword', executionState: 1, recalledCount: 3, acceptedCount: 1, durationMs: 140, avgScore: 0.64 }
  ]
  const stageTraces = [{
    stageCode: 'RAG_RETRIEVE',
    snapshot: {
      subQuestions: [{
        index: 1,
        question: '如何定位检索下降原因？',
        fusedCandidateCount: 3,
        rerankedCandidateCount: 2,
        referenceCount: 1,
        channelTraces: [
          { channelName: 'vector', channelWeight: 0.7, recalledCount: 6, acceptedCount: 4 },
          { channelName: 'keyword', channelWeight: 0.3, recalledCount: 3, acceptedCount: 1 }
        ]
      }]
    }
  }]

  it('builds a zero-baseline channel flow and real aggregate stages', () => {
    const view = buildFusionTraceGroups(results, executions, stageTraces)
    const summary = buildRetrievalFlowSummary(view)
    const flow = buildFusionCandidateFlow(view.groups[0])

    expect(summary.stages.map((stage) => [stage.key, stage.value])).toEqual([
      ['channels', 2],
      ['recalled', 9],
      ['accepted', 5],
      ['reranked', 2],
      ['selected', 1]
    ])
    expect(flow.channels[0]).toEqual(expect.objectContaining({ channelType: 'vector', recalledRatio: 1, acceptedRatio: 4 / 6, selectedRatio: 1 / 6 }))
    expect(flow.channels[1]).toEqual(expect.objectContaining({ channelType: 'keyword', recalledRatio: 0.5, acceptedRatio: 1 / 6, selectedRatio: 0 }))
  })

  it('projects selected, neutral and filtered candidates into grouped row tracks', () => {
    const group = buildFusionTraceGroups(results, executions, stageTraces).groups[0]
    const flow = buildFusionCandidateFlow(group)

    expect(flow.candidates).toHaveLength(3)
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-selected')).toEqual(expect.objectContaining({
      tone: 'success', outcomeLabel: '最终证据 #1',
      contentKind: '章节路径',
      contentLabel: '9.3.2 为什么不直接把父块拿去做召回',
      contentMeta: '来源：向量检索'
    }))
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-keyword-neutral')).toEqual(expect.objectContaining({
      tone: 'neutral', outcomeLabel: '未选入',
      contentKind: '正文预览',
      contentText: '命中率下降时先检查召回范围和质量闸门。'
    }))
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-filtered')).toEqual(expect.objectContaining({
      tone: 'warning', outcomeLabel: '闸门过滤',
      contentKind: '内容状态',
      contentText: '未记录可读内容'
    }))
    expect(flow.groups.map((item) => [item.key, item.label, item.candidates.length])).toEqual([
      ['selected', '最终证据', 1],
      ['unselected', '未选入', 1],
      ['filtered', '闸门过滤', 1]
    ])
    expect(flow.candidates.every((item) => item.stages.map((stage) => stage.key).join('|') === 'channel|fusion|rerank|outcome')).toBe(true)
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-selected').stages).toEqual([
      { key: 'channel', label: '召回候选', value: '向量检索' },
      { key: 'fusion', label: '融合分', value: '0.9200' },
      { key: 'rerank', label: '重排分', value: '0.9600' },
      { key: 'outcome', label: '最终结果', value: '最终证据 #1' }
    ])
    expect(flow.candidates.every((item) => !('path' in item) && !('points' in item) && !('channelRow' in item))).toBe(true)
    expect(flow).not.toHaveProperty('height')
    expect(flow.candidates.every((item) => item.ariaLabel.includes(item.outcomeLabel))).toBe(true)
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-selected').ariaLabel).toContain('9.3.2 为什么不直接把父块拿去做召回')
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-selected').ariaLabel).not.toContain('运营管理手册.md')
    expect(flow.candidates.find((item) => item.row.candidateId === 'candidate-vector-filtered').ariaLabel).not.toMatch(/P#|C#|父块 27|文档块 4/)
  })

  it('keeps selected evidence inside a bounded candidate window', () => {
    const extraRows = Array.from({ length: 8 }, (_, index) => ({
      ...results[1],
      candidateId: `candidate-extra-${index}`,
      chunkId: `chunk-extra-${index}`,
      channelRank: index + 2,
      hybridScore: 0.6 - index * 0.02
    }))
    const group = buildFusionTraceGroups([...extraRows, ...results], executions, stageTraces).groups[0]
    const flow = buildFusionCandidateFlow(group, 4)

    expect(flow.candidates).toHaveLength(4)
    expect(flow.candidates.some((item) => item.row.candidateId === 'candidate-vector-selected')).toBe(true)
    expect(flow.hiddenCount).toBe(group.resultRows.length - 4)
    expect(flow.totalCount).toBe(group.resultRows.length)
  })
})
