import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDocumentListView from './AdminDocumentListView.vue'
import AdminObservabilityListView from './AdminObservabilityListView.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  queryDocumentPage: vi.fn(),
  deleteDocument: vi.fn(),
  uploadDocument: vi.fn(),
  listSessionsPage: vi.fn()
}))

vi.mock('vue-router', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    useRouter: () => ({ push: mocks.push }),
    useRoute: () => ({ query: {} }),
    RouterLink: defineComponent({
      name: 'RouterLink',
      props: { to: { type: [String, Object], default: '' } },
      setup(_props, { slots }) { return () => h('a', { href: '#' }, slots.default?.()) }
    })
  }
})

vi.mock('../../api/api', () => ({
  APIError: class APIError extends Error {},
  manageApi: {
    queryDocumentPage: mocks.queryDocumentPage,
    deleteDocument: mocks.deleteDocument,
    uploadDocument: mocks.uploadDocument
  },
  chatApi: { listSessionsPage: mocks.listSessionsPage }
}))

const documentRecord = {
  documentId: 'doc-1',
  documentName: '人事制度',
  originalFileName: 'policy.pdf',
  knowledgeScopeCode: 'hr',
  knowledgeScopeName: '人事域',
  fileTypeName: 'PDF',
  fileSize: 1024,
  parseStatus: '3',
  strategyStatus: '3',
  indexStatus: '3',
  parseStatusName: '解析成功',
  strategyStatusName: '策略确认',
  indexStatusName: '索引完成',
  editTime: '2026-07-21T08:00:00Z'
}

beforeEach(() => {
  Object.values(mocks).forEach((mock) => mock.mockReset())
  mocks.queryDocumentPage.mockResolvedValue({ records: [documentRecord], pageNo: 1, pageSize: 12, total: 25 })
  mocks.listSessionsPage.mockResolvedValue({
    sessions: [{
      conversationId: 'conversation-1',
      latestQuestion: '年假怎么申请？',
      latestAnswer: '在系统中提交年假申请。',
      latestExchangeId: 'exchange-9',
      latestTurnStatus: 'COMPLETED',
      chatMode: 'DOCUMENT',
      messageCount: 4,
      updatedAt: '2026-07-21T08:00:00Z'
    }],
    pageNo: '1',
    pageSize: '12',
    totalSize: '13',
    totalPages: '2'
  })
})

describe('F05 document list behavior', () => {
  it('opens document details only from an explicit view button', async () => {
    const wrapper = mount(AdminDocumentListView)
    await flushPromises()
    expect(wrapper.text()).not.toContain('按文档身份、主处理状态和更新时间扫描列表')
    expect(wrapper.text()).not.toContain('搜索文档')
    expect(wrapper.get('#document-search').attributes('aria-label')).toBe('搜索文档')

    const summaries = wrapper.findAll('[data-document-summary]')
    expect(summaries).toHaveLength(2)
    expect(wrapper.findAll('[data-document-summary] a')).toHaveLength(0)

    await summaries[0].trigger('click')
    expect(mocks.push).not.toHaveBeenCalled()

    const viewButton = wrapper.findAll('button').find((button) => button.text() === '查看')
    await viewButton.trigger('click')
    expect(mocks.push).toHaveBeenCalledWith({
      name: 'AdminDocumentDetail',
      params: { documentId: 'doc-1' },
      query: {}
    })
  })

  it('uses server search and pagination parameters', async () => {
    const wrapper = mount(AdminDocumentListView)
    await flushPromises()
    expect(mocks.queryDocumentPage).toHaveBeenLastCalledWith({ pageNo: 1, pageSize: 12, keyword: '' })

    await wrapper.get('#document-search').setValue(' policy ')
    await wrapper.get('#document-search').trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(mocks.queryDocumentPage).toHaveBeenLastCalledWith({ pageNo: 1, pageSize: 12, keyword: 'policy' })

    const next = wrapper.findAll('button').find((button) => button.text() === '下一页')
    await next.trigger('click')
    await flushPromises()
    expect(mocks.queryDocumentPage).toHaveBeenLastCalledWith({ pageNo: 2, pageSize: 12, keyword: 'policy' })
  })
})

describe('F05 observability list behavior', () => {
  it('opens observability details only from explicit, full-size action buttons', async () => {
    const wrapper = mount(AdminObservabilityListView)
    await flushPromises()
    expect(wrapper.text()).not.toContain('搜索会话')
    expect(wrapper.get('#session-search').attributes('aria-label')).toBe('搜索会话')

    const summaries = wrapper.findAll('[data-session-summary]')
    expect(summaries).toHaveLength(2)
    expect(wrapper.findAll('[data-session-summary] a')).toHaveLength(0)

    await summaries[0].trigger('click')
    expect(mocks.push).not.toHaveBeenCalled()

    const desktopActions = wrapper.get('[data-session-actions]')
    const viewSessionButton = desktopActions.findAll('button').find((button) => button.text() === '查看会话')
    const latestExchangeButton = desktopActions.findAll('button').find((button) => button.text() === '最近轮次')
    expect(viewSessionButton.classes()).toContain('h-8')
    expect(latestExchangeButton.classes()).toContain('h-8')

    await viewSessionButton.trigger('click')
    expect(mocks.push).toHaveBeenLastCalledWith({
      name: 'AdminObservabilitySession',
      params: { conversationId: 'conversation-1' },
      query: { listKeyword: undefined, listMode: undefined, listStatus: undefined, listPage: '1', listPageSize: '12' }
    })

    await latestExchangeButton.trigger('click')
    expect(mocks.push).toHaveBeenLastCalledWith({
      name: 'AdminObservabilityExchangeDetail',
      params: { conversationId: 'conversation-1', exchangeId: 'exchange-9' },
      query: { listKeyword: undefined, listMode: undefined, listStatus: undefined, listPage: '1', listPageSize: '12' }
    })
  })

  it('preserves explicit string pagination and filter parameters', async () => {
    const wrapper = mount(AdminObservabilityListView)
    await flushPromises()
    expect(mocks.listSessionsPage).toHaveBeenLastCalledWith({ keyword: '', chatMode: 'ALL', turnStatus: 'ALL', pageNo: '1', pageSize: '12' })

    await wrapper.get('#session-search').setValue('年假')
    await wrapper.get('#session-search').trigger('keydown', { key: 'Enter' })
    await flushPromises()
    expect(mocks.listSessionsPage).toHaveBeenLastCalledWith({ keyword: '年假', chatMode: 'ALL', turnStatus: 'ALL', pageNo: '1', pageSize: '12' })

    expect(wrapper.text()).toContain('已完成')
    expect(wrapper.text()).not.toContain('border-l-4')
  })
})
