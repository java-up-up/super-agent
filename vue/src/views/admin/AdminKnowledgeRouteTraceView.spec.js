import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminKnowledgeRouteTraceView from './AdminKnowledgeRouteTraceView.vue'

const mocks = vi.hoisted(() => ({
  queryPage: vi.fn()
}))

vi.mock('../../api/api', () => ({
  manageApi: {
    queryKnowledgeRouteTracePage: mocks.queryPage
  }
}))

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

function pageWithQuestion(question) {
  return {
    pageNo: '1',
    pageSize: '20',
    totalSize: '1',
    totalPages: '1',
    records: [{
      id: question,
      question,
      mode: 'auto',
      routeStatus: '1',
      confidence: '0.82',
      topScopesJson: '[]',
      topTopicsJson: '[]',
      topDocumentsJson: '[]'
    }]
  }
}

beforeEach(() => {
  mocks.queryPage.mockReset()
})

describe('F07 knowledge route trace requests', () => {
  it('renders auto and shadow modes with distinct semantic badge roles', async () => {
    mocks.queryPage.mockResolvedValueOnce({
      pageNo: '1',
      pageSize: '20',
      totalSize: '2',
      totalPages: '1',
      records: [
        { ...pageWithQuestion('自动路由记录').records[0], conversationId: 'conversation-hidden', exchangeId: 'exchange-hidden' },
        { ...pageWithQuestion('影子路由记录').records[0], id: 'shadow', mode: 'shadow' }
      ]
    })

    const wrapper = mount(AdminKnowledgeRouteTraceView)
    await flushPromises()

    expect(wrapper.get('[data-route-mode="auto"]').classes()).toContain('bg-[var(--route-mode-auto-bg)]')
    expect(wrapper.get('[data-route-mode="shadow"]').classes()).toContain('bg-[var(--route-mode-shadow-bg)]')
    expect(wrapper.text()).toContain('自动知识路由')
    expect(wrapper.text()).toContain('影子路由对比')
    expect(wrapper.text()).not.toContain('conversation-hidden')
    expect(wrapper.text()).not.toContain('exchange-hidden')
  })

  it('does not let an older response replace a newer page result', async () => {
    const first = deferred()
    const second = deferred()
    mocks.queryPage.mockReturnValueOnce(first.promise).mockReturnValueOnce(second.promise)

    const wrapper = mount(AdminKnowledgeRouteTraceView)
    await flushPromises()
    await wrapper.find('input').trigger('keydown', { key: 'Enter' })

    second.resolve(pageWithQuestion('较新的追踪记录'))
    await flushPromises()
    first.resolve(pageWithQuestion('过期的追踪记录'))
    await flushPromises()

    expect(wrapper.text()).toContain('较新的追踪记录')
    expect(wrapper.text()).not.toContain('过期的追踪记录')
  })

  it('keeps the last successful page when refresh fails', async () => {
    mocks.queryPage
      .mockResolvedValueOnce(pageWithQuestion('保留的追踪记录'))
      .mockRejectedValueOnce(new Error('追踪刷新失败'))

    const wrapper = mount(AdminKnowledgeRouteTraceView)
    await flushPromises()
    const refresh = wrapper.findAll('button').find((button) => button.text().includes('刷新追踪'))
    await refresh.trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('追踪刷新失败')
    expect(wrapper.text()).toContain('保留的追踪记录')
  })
})
