import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DocumentTaskHistoryDialog from './DocumentTaskHistoryDialog.vue'

describe('DocumentTaskHistoryDialog', () => {
  it('renders task status and long log detail inside the child-page dialog contract', () => {
    const wrapper = mount(DocumentTaskHistoryDialog, {
      props: {
        open: true,
        documentDetail: { latestTaskId: 'task-1', latestTaskTypeName: '解析', latestTaskStatusName: '执行中', latestTaskStatus: '2', indexStatusName: '待构建', indexStatus: '1' },
        logs: [{ id: '1', stageTypeName: '解析', eventTypeName: '完成', createTime: '2026-07-21T08:00:00Z', content: '内容解析完成', detailJson: '{"pages":2}' }]
      },
      global: {
        stubs: {
          ChildPageDialog: { template: '<div data-dialog><slot /></div>' },
          AdminStatusBadge: { template: '<span>{{ label }}</span>', props: ['label'] }
        }
      }
    })

    expect(wrapper.get('[data-dialog]').text()).toContain('内容解析完成')
    expect(wrapper.get('pre').text()).toBe('{\n  "pages": 2\n}')
    expect(wrapper.text()).toContain('执行中')
  })

  it('falls back to the raw string when detail json is not parseable', () => {
    const wrapper = mount(DocumentTaskHistoryDialog, {
      props: {
        open: true,
        documentDetail: {},
        logs: [{ id: '1', stageTypeName: '解析', eventTypeName: '完成', content: '内容解析完成', detailJson: 'not-json' }]
      },
      global: {
        stubs: {
          ChildPageDialog: { template: '<div data-dialog><slot /></div>' },
          AdminStatusBadge: { template: '<span>{{ label }}</span>', props: ['label'] }
        }
      }
    })

    expect(wrapper.get('pre').text()).toBe('not-json')
  })
})

