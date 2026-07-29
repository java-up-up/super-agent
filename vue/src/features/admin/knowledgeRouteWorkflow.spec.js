import { describe, expect, it } from 'vitest'
import {
  buildRelationRequest,
  buildScopeRequest,
  buildTopicRequest,
  createRouteTraceTarget
} from './knowledgeRouteWorkflow'

describe('F06 knowledge-route workflow contracts', () => {
  it('builds only the backend scope, topic, and relation contract fields', () => {
    expect(buildScopeRequest({
      id: 'scope-1', scopeCode: ' hr ', scopeName: '  HR ', parentScopeCode: '__none__', description: ' d ', aliases: 'a,b', examples: '["q"]', sortOrder: '2', operatorId: '10001', uiLabel: 'ignore'
    })).toEqual({
      id: 'scope-1', scopeCode: 'hr', scopeName: 'HR', parentScopeCode: '', description: 'd', aliases: 'a,b', examples: '["q"]', sortOrder: '2', operatorId: '10001'
    })

    expect(buildTopicRequest({
      id: '', topicCode: ' leave ', topicName: ' Leave ', scopeCode: 'hr', description: '', aliases: '', examples: '', answerShape: 'procedure', executionPreference: 'stable', sortOrder: 0, operatorId: '10001', inferredDocumentId: 'must-not-leak'
    })).not.toHaveProperty('inferredDocumentId')

    expect(buildRelationRequest({
      topicCode: ' leave ', documentId: 'doc-1', relationScore: '0.9000', relationSource: 'manual', reason: ' confirmed ', operatorId: '10001', routeStatus: 'SUCCESS'
    })).toEqual({
      topicCode: 'leave', documentId: 'doc-1', relationScore: '0.9000', relationSource: 'manual', reason: 'confirmed', operatorId: '10001'
    })
  })

  it('preserves route/context on trace navigation without recomputing a route', () => {
    expect(createRouteTraceTarget({ exchangeId: 'ex-1', conversationId: 'c-1', topicCode: ' leave ' })).toEqual({
      name: 'AdminObservabilityDetail',
      params: { conversationId: 'c-1' },
      query: { exchangeId: 'ex-1', topicCode: 'leave', source: 'knowledge-route' }
    })
  })
})
