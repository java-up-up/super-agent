import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { afterEach, describe, expect, it } from 'vitest'
import Chat from './Chat.vue'

const mountedWrappers = []

afterEach(() => {
  mountedWrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  document.body.innerHTML = ''
})

function mountChat(message, props = {}) {
  const wrapper = mount(Chat, {
    attachTo: document.body,
    props: { message, ...props }
  })
  mountedWrappers.push(wrapper)
  return wrapper
}

describe('Chat answer rendering', () => {
  it('sanitizes markdown and links only citation tokens backed by stable references', async () => {
    const wrapper = mountChat({
      id: 'assistant-1',
      role: 'assistant',
      content: '<img src="x" onerror="window.__unsafe = true">结论见 [1]，越界 token [2] 保持普通文本。',
      references: [{
        citationIdentity: 'CHUNK:10:20',
        citationEvidenceType: 'CHUNK',
        documentName: '培训手册',
        quoteText: '这是后端绑定的来源片段。'
      }],
      recommendations: []
    })

    expect(wrapper.html()).not.toContain('onerror')
    expect(wrapper.findAll('.citation-token')).toHaveLength(1)
    expect(wrapper.find('.citation-token').text()).toBe('[1]')
    expect(wrapper.text()).toContain('[2]')

    await wrapper.find('.citation-token').trigger('click')
    await nextTick()
    expect(document.body.querySelector('[data-slot="dialog-content"]')).not.toBeNull()
    expect(document.body.textContent).toContain('来源 [1] · 培训手册')
  })

  it('keeps failure recovery in the answer context', async () => {
    const wrapper = mountChat({
      id: 'assistant-2',
      role: 'assistant',
      question: '重试这个问题',
      content: '',
      errorMessage: '网络中断',
      references: [],
      recommendations: []
    })

    const retryButton = wrapper.findAll('button').find((button) => button.text().includes('重新发送'))
    await retryButton.trigger('click')
    expect(wrapper.emitted('retry')).toEqual([['重试这个问题']])
  })
})
