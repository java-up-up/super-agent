import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminObservabilitySessionView from './AdminObservabilitySessionView.vue'

const mocks = vi.hoisted(() => ({
  route: {
    params: { conversationId: 'conversation-1' },
    query: { listKeyword: '年假', listPage: '3', listPageSize: '12' }
  },
  getSession: vi.fn(),
  rebuildSummary: vi.fn()
}))

vi.mock('vue-router', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    useRoute: () => mocks.route,
    RouterLink: defineComponent({
      name: 'RouterLink',
      props: { to: { type: [String, Object], default: '' } },
      setup(props, { attrs, slots }) {
        return () => h('a', { ...attrs, href: '#', 'data-route': JSON.stringify(props.to) }, slots.default?.())
      }
    })
  }
})

vi.mock('../../api/api', () => ({
  APIError: class APIError extends Error {},
  chatApi: {
    getSession: mocks.getSession,
    rebuildConversationSummary: mocks.rebuildSummary
  }
}))

function sessionWithTurns(count) {
  return {
    conversationId: 'conversation-1',
    chatMode: 'DOCUMENT',
    messageCount: count * 2,
    checkpointCount: count,
    running: false,
    exchanges: Array.from({ length: count }, (_, index) => ({
      exchangeId: String(index + 1),
      question: `问题 ${index + 1}`,
      answer: `回答 ${index + 1}`,
      status: 'COMPLETED',
      references: [],
      recommendations: []
    }))
  }
}

beforeEach(() => {
  mocks.route.query = { listKeyword: '年假', listPage: '3', listPageSize: '12' }
  mocks.getSession.mockReset()
  mocks.rebuildSummary.mockReset()
  mocks.getSession.mockResolvedValue(sessionWithTurns(45))
})

describe('F07 observability session navigation', () => {
  it('keeps the document title, moves the summary behind an explicit dialog, and removes redundant overview content', async () => {
    mocks.getSession.mockResolvedValue({
      ...sessionWithTurns(2),
      selectedDocumentName: '星联智服全渠道客服平台上线与运营管理手册.md',
      latestUserMessage: '最近用户问题不应重复展示',
      memorySummary: {
        compressionApplied: true,
        coveredExchangeCount: 2,
        summaryVersion: 2,
        compressionCount: 2,
        summaryText: '【会话目标】\n保留的长期会话摘要\n\n【已确认信息】\n- 已完成文档接入\n- 继续核对最近轮次'
      }
    })

    const wrapper = mount(AdminObservabilitySessionView)
    await flushPromises()

    expect(wrapper.text()).toContain('星联智服全渠道客服平台上线与运营管理手册.md')
    expect(wrapper.text()).toContain('查看长期摘要')
    expect(wrapper.findAll('button').some((button) => button.text().includes('刷新会话详情'))).toBe(false)
    expect(wrapper.text()).not.toContain('长期摘要快照')
    expect(wrapper.text()).not.toContain('保留的长期会话摘要')
    expect(wrapper.text()).toContain('此会话的每次问答')
    expect(wrapper.text()).not.toContain('整条会话的轮次总览，点击某一轮后查看具体的轮次详情。')

    const summaryTrigger = wrapper.findAll('button').find((button) => button.text() === '查看长期摘要')
    await summaryTrigger.trigger('click')
    await flushPromises()

    const dialog = document.body.querySelector('[role="dialog"]')
    expect(dialog).not.toBeNull()
    expect(dialog.textContent).toContain('长期摘要快照')
    expect(dialog.textContent).toContain('覆盖 2 轮 · 第 2 版 · 已压缩 2 次')
    expect(dialog.textContent).toContain('保留的长期会话摘要')
    expect([...dialog.querySelectorAll('h3')].map((heading) => heading.textContent)).toContain('会话目标')
    expect([...dialog.querySelectorAll('li')].map((item) => item.textContent)).toEqual(['已完成文档接入', '继续核对最近轮次'])

    expect(wrapper.text()).not.toContain('显示整条会话里的每次问答')
    expect(wrapper.text()).not.toContain('当前文档问答')
    expect(wrapper.text()).not.toContain('最近一轮已完成')
    expect(wrapper.text()).not.toContain('会话ID')
    expect(wrapper.text()).not.toContain('助手轮次')
    expect(wrapper.text()).not.toContain('会话消息数')
    expect(wrapper.text()).not.toContain('会话级背景')
    expect(wrapper.text()).not.toContain('最近用户问题')
    expect(wrapper.text()).not.toContain('最近助手回答')
    expect(wrapper.text()).not.toContain('Checkpoint / 消息数')
  })

  it('shows a concise unavailable state until rebuilding creates a summary', async () => {
    mocks.getSession.mockResolvedValue(sessionWithTurns(2))
    mocks.rebuildSummary.mockResolvedValue({
      compressionApplied: true,
      coveredExchangeCount: 2,
      summaryVersion: 1,
      compressionCount: 1,
      summaryText: '重建后的长期摘要'
    })

    const wrapper = mount(AdminObservabilitySessionView)
    await flushPromises()

    expect(wrapper.text()).toContain('尚未形成长期摘要')
    expect(wrapper.findAll('button').some((button) => button.text() === '查看长期摘要')).toBe(false)

    const rebuild = wrapper.findAll('button').find((button) => button.text() === '重建长期摘要')
    await rebuild.trigger('click')
    await flushPromises()

    expect(wrapper.text()).not.toContain('尚未形成长期摘要')
    expect(wrapper.findAll('button').some((button) => button.text() === '查看长期摘要')).toBe(true)
    expect(document.body.querySelector('[role="dialog"]')).toBeNull()
  })

  it('renders each turn as a scan-friendly audit record with one explicit detail link', async () => {
    const session = sessionWithTurns(2)
    mocks.getSession.mockResolvedValue({
      ...session,
      exchanges: [{
        exchangeId: '1',
        question: '怎样核对这一轮的检索结果？',
        answer: '先核对引用来源，再检查最终回答中的引用绑定。这个结尾用于确认回答正文由样式控制预览，而不是在数据层提前截断。',
        status: 'COMPLETED',
        totalResponseTimeMs: 15920,
        references: [{}],
        recommendations: [{}, {}, {}],
        debugTrace: {
          executionMode: 'STANDARD',
          modelUsageTraces: [{ totalTokens: 3377, estimatedCost: 0.0146 }]
        }
      }, session.exchanges[1]]
    })

    const wrapper = mount(AdminObservabilitySessionView)
    await flushPromises()

    const exchangeRows = wrapper.findAll('[data-exchange-row]')
    const exchangeRow = exchangeRows[0]
    const detailLinks = exchangeRow.findAll('a')
    expect(exchangeRows).toHaveLength(2)
    // 轮次行是后台内容卡，走 L2 玻璃面（rounded-glass + glass-card），不再是 bg-card 纯白
    expect(exchangeRows[0].classes()).toEqual(expect.arrayContaining(['rounded-glass', 'border', 'glass-card', 'py-4']))
    expect(exchangeRows[0].classes()).not.toContain('border-b-2')
    expect(exchangeRows[1].classes()).toEqual(expect.arrayContaining(['rounded-glass', 'border', 'glass-card', 'py-4']))
    expect(exchangeRow.element.tagName).toBe('DIV')
    expect(exchangeRow.classes()).toContain('hover:bg-muted/60')
    expect(exchangeRow.findAll('[data-exchange-header]')).toHaveLength(0)
    expect(detailLinks).toHaveLength(1)
    expect(detailLinks[0].attributes('aria-label')).toBe('查看第 1 轮详情')
    expect(detailLinks[0].text()).toContain('查看轮次详情')
    expect(detailLinks[0].text()).not.toContain('怎样核对这一轮的检索结果？')

    expect(exchangeRow.findAll('[data-exchange-dialogue] dt').map((item) => item.text())).toEqual(['问', '答'])
    expect(exchangeRow.text()).toContain('这个结尾用于确认回答正文由样式控制预览')
    expect(exchangeRow.findAll('[data-exchange-metrics] dt').map((item) => item.text())).toEqual(['耗时', '引用', '推荐', 'Token', '成本'])
    expect(exchangeRow.findAll('[data-exchange-metrics] dd').map((item) => item.text())).toEqual(['15.9 s', '1', '3', '3,377', '¥ 0.0146'])
  })

  it('segments long sessions and preserves list context in exchange links', async () => {
    const wrapper = mount(AdminObservabilitySessionView)
    await flushPromises()

    expect(wrapper.text()).toContain('问题 1')
    expect(wrapper.text()).toContain('问题 20')
    expect(wrapper.text()).not.toContain('问题 21')

    const next = wrapper.findAll('button').find((button) => button.text() === '下一页')
    await next.trigger('click')
    expect(wrapper.text()).toContain('问题 21')
    expect(wrapper.text()).not.toContain('问题 1')

    const exchangeLink = wrapper.findAll('a').find((link) => link.attributes('aria-label') === '查看第 21 轮详情')
    expect(exchangeLink.text()).not.toContain('问题 21')
    const target = JSON.parse(exchangeLink.attributes('data-route'))
    expect(target.query).toEqual(expect.objectContaining({ listKeyword: '年假', listPage: '3', turnPage: '2' }))
  })

})
