import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDashboardView from './AdminDashboardView.vue'

const mocks = vi.hoisted(() => ({
  queryDocumentPage: vi.fn(),
  push: vi.fn()
}))

vi.mock('vue-router', () => ({ useRouter: () => ({ push: mocks.push }) }))

vi.mock('../../api/api', () => ({
  manageApi: {
    queryDocumentPage: mocks.queryDocumentPage
  }
}))

describe('ordinary dashboard business scope', () => {
  beforeEach(() => Object.values(mocks).forEach((mock) => mock.mockReset()))

  it('shows ordinary document metrics, demo path, and recent documents', async () => {
    mocks.queryDocumentPage.mockResolvedValue({
      total: 1,
      records: [{
        documentId: 'doc-1',
        documentName: '员工手册',
        originalFileName: 'employee-guide.pdf',
        parseStatus: '3',
        parseStatusName: '解析成功',
        strategyStatus: '3',
        indexStatus: '3',
        indexStatusName: '索引完成'
      }]
    })

    const wrapper = mount(AdminDashboardView)
    await flushPromises()

    expect(mocks.queryDocumentPage).toHaveBeenCalledWith({ pageNo: 1, pageSize: 50, keyword: '' })
    expect(wrapper.text()).toContain('文档总数')
    expect(wrapper.text()).toContain('建议演示路径')
    expect(wrapper.text()).toContain('最近接入文档')
    expect(wrapper.text()).toContain('员工手册')
    expect(wrapper.text()).not.toContain('知识路由健康度')
    expect(wrapper.text()).not.toContain('shadow 命中率')
  })

  it('exposes a retry state when the document overview fails', async () => {
    mocks.queryDocumentPage.mockRejectedValue(new Error('文档概览不可用'))

    const wrapper = mount(AdminDashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('运营数据加载失败')
    expect(wrapper.text()).toContain('文档概览不可用')
    expect(wrapper.findAll('button').some((button) => button.text().includes('重新加载'))).toBe(true)
  })
})
