import { mount, flushPromises } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BusinessChatView from './BusinessChatView.vue'

const apiMocks = vi.hoisted(() => ({
  listSessions: vi.fn(),
  listKnowledgeDocumentOptions: vi.fn(),
  getSession: vi.fn(),
  deleteSession: vi.fn(),
  stopSession: vi.fn(),
  openStream: vi.fn(),
  queryKnowledgeRouteTracePage: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ resolve: () => ({ href: '/admin/login?redirect=/admin/dashboard' }) })
}))

vi.mock('../api/api', () => ({
  APIError: class APIError extends Error {},
  createConversationId: () => 'conversation-test',
  chatApi: {
    listSessions: apiMocks.listSessions,
    listKnowledgeDocumentOptions: apiMocks.listKnowledgeDocumentOptions,
    getSession: apiMocks.getSession,
    deleteSession: apiMocks.deleteSession,
    stopSession: apiMocks.stopSession,
    openStream: apiMocks.openStream
  },
  manageApi: { queryKnowledgeRouteTracePage: apiMocks.queryKnowledgeRouteTracePage }
}))

let wrapper

beforeEach(() => {
  Object.values(apiMocks).forEach((mock) => mock.mockReset())
  apiMocks.listSessions.mockResolvedValue([])
  apiMocks.listKnowledgeDocumentOptions.mockResolvedValue([])
  apiMocks.stopSession.mockResolvedValue({ message: '已停止生成' })
  apiMocks.queryKnowledgeRouteTracePage.mockResolvedValue({ records: [] })
})

afterEach(() => {
  wrapper?.unmount()
  wrapper = null
  document.body.innerHTML = ''
})

describe('BusinessChatView stream ownership', () => {
  it('scrolls to the latest answer after the hydrated conversation is rendered', async () => {
    let resolveConversation
    apiMocks.listSessions.mockResolvedValue([{
      conversationId: 'conversation-existing',
      updatedAt: '2026-07-28T14:18:00Z'
    }])
    apiMocks.getSession.mockImplementation(() => new Promise((resolve) => {
      resolveConversation = resolve
    }))

    wrapper = mount(BusinessChatView, { attachTo: document.body })
    await flushPromises()

    const messagesPanel = wrapper.get('[aria-label="对话消息"]')
    expect(wrapper.find('[aria-label="正在加载会话内容"]').exists()).toBe(true)
    Object.defineProperty(messagesPanel.element, 'scrollHeight', {
      configurable: true,
      get: () => wrapper.find('[aria-label="正在加载会话内容"]').exists() ? 180 : 1200
    })
    messagesPanel.element.scrollTo = vi.fn(({ top }) => {
      messagesPanel.element.scrollTop = top
    })

    resolveConversation({
      conversationId: 'conversation-existing',
      chatMode: 'OPEN_CHAT',
      exchanges: [{
        exchangeId: 'exchange-existing',
        question: '测试问题',
        answer: '一段足够长的最终回答'
      }]
    })
    await flushPromises()

    expect(wrapper.find('[aria-label="正在加载会话内容"]').exists()).toBe(false)
    expect(messagesPanel.element.scrollTop).toBe(1200)
    expect(messagesPanel.element.scrollTo).toHaveBeenLastCalledWith({ top: 1200, behavior: 'instant' })
  })

  it('keeps the ordinary chat scope without knowledge-base selection controls', async () => {
    wrapper = mount(BusinessChatView, { attachTo: document.body })
    await flushPromises()

    expect(wrapper.find('legend').text()).not.toContain('知识库范围')
    expect(wrapper.text()).not.toContain('暂无可用知识库，可在管理端创建。')
    expect(wrapper.find('input[type="checkbox"]').exists()).toBe(false)
    expect(wrapper.find('[role="checkbox"]').exists()).toBe(false)
  })

  it('guards IME Enter, preserves payload, and rejects chunks after stop', async () => {
    let streamHandlers
    let rejectStream
    let resolveStop
    const controller = {
      abort: vi.fn(() => {
        const error = new Error('aborted')
        error.name = 'AbortError'
        rejectStream(error)
      })
    }
    apiMocks.openStream.mockImplementation((_payload, handlers) => {
      streamHandlers = handlers
      return {
        controller,
        done: new Promise((_resolve, reject) => { rejectStream = reject })
      }
    })
    apiMocks.stopSession.mockImplementation(() => new Promise((resolve) => { resolveStop = resolve }))

    wrapper = mount(BusinessChatView, { attachTo: document.body })
    await flushPromises()

    const textarea = wrapper.get('textarea[aria-label="输入问题"]')
    await textarea.setValue('中文输入法问题')
    await textarea.trigger('keydown', { key: 'Enter', keyCode: 229, isComposing: true })
    expect(apiMocks.openStream).not.toHaveBeenCalled()

    await textarea.trigger('keydown', { key: 'Enter', keyCode: 13, isComposing: false })
    await flushPromises()
    expect(apiMocks.openStream).toHaveBeenCalledTimes(1)
    const payload = apiMocks.openStream.mock.calls[0][0]
    expect(payload).toMatchObject({
      question: '中文输入法问题',
      conversationId: 'conversation-test',
      chatMode: 'OPEN_CHAT',
      selectedDocumentId: null
    })
    expect(payload).not.toHaveProperty('knowledgeBaseSelectionMode')
    expect(payload).not.toHaveProperty('selectedKnowledgeBaseIds')

    streamHandlers.onEvent({ type: 'text', content: '第一段' })
    await flushPromises()
    expect(wrapper.text()).toContain('第一段')

    await wrapper.get('button[aria-label="停止生成"]').trigger('click')
    streamHandlers.onEvent({ type: 'text', content: '停止后旧 chunk' })
    await flushPromises()

    expect(controller.abort).toHaveBeenCalledTimes(1)
    expect(apiMocks.stopSession).toHaveBeenCalledWith('conversation-test')
    expect(wrapper.text()).not.toContain('停止后旧 chunk')
    expect(wrapper.get('button[aria-label="停止生成"]').attributes('aria-busy')).toBe('true')

    resolveStop({ message: '已停止生成' })
    await flushPromises()
    expect(wrapper.find('button[aria-label="停止生成"]').exists()).toBe(false)
  })
})
