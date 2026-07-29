import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

function openingTags(content, tagName) {
  return content.match(new RegExp(`<${tagName}\\b[^>]*>`, 'gs')) || []
}

describe('F08 responsive and accessibility contracts', () => {
  it('gives the fullscreen chat shell a keyboard skip target', () => {
    const chat = source('src/views/BusinessChatView.vue')
    expect(chat).toContain('href="#chat-main-content"')
    expect(chat).toMatch(/<main\b[^>]*id="chat-main-content"[^>]*tabindex="-1"/s)
  })

  it('does not leave mouse-only divs or articles in knowledge workflows', () => {
    const content = [
      source('src/views/admin/AdminKnowledgeRouteView.vue'),
      source('src/views/admin/AdminKnowledgeRouteTraceView.vue')
    ].join('\n')
    const clickTargets = [
      ...openingTags(content, 'div'),
      ...openingTags(content, 'article')
    ].filter((tag) => tag.includes('@click='))

    clickTargets.forEach((tag) => {
      expect(tag).toContain('role="button"')
      expect(tag).toContain('tabindex="0"')
      expect(tag).toContain('@keydown.enter')
      expect(tag).toContain('@keydown.space')
    })
  })

  it('gives knowledge route filters and form controls explicit accessible names', () => {
    const content = [
      source('src/views/admin/AdminKnowledgeRouteView.vue'),
      source('src/views/admin/AdminKnowledgeRouteTraceView.vue')
    ].join('\n')

    ;['Input', 'Textarea', 'SelectTrigger'].forEach((tagName) => {
      openingTags(content, tagName).forEach((tag) => {
        expect(tag, tag).toMatch(/aria-label="[^"]+"/)
      })
    })
  })

  it('uses explicit static reduced-motion end states for historical risks', () => {
    const expectations = [
      ['src/views/admin/AdminDocumentDetailView.vue', '.stage-spinner'],
      ['src/views/admin/AdminKnowledgeRouteView.vue', '.tab-content']
    ]

    expectations.forEach(([path, selector]) => {
      const content = source(path)
      expect(content).toContain('@media (prefers-reduced-motion: reduce)')
      expect(content).toContain(selector)
      expect(content).toMatch(/animation:\s*none/)
    })

    expect(source('src/components/ui/select/SelectContent.vue')).toContain('motion-reduce:animate-none')
  })

  it('keeps direct data tables named and column headers scoped', () => {
    const directTableFiles = [
      'src/views/admin/AdminDocumentDetailView.vue',
      'src/views/admin/AdminObservabilityDetailView.vue'
    ]
    const tableShellFiles = [
      'src/views/admin/AdminDocumentListView.vue',
      'src/views/admin/AdminObservabilityListView.vue'
    ]

    directTableFiles.forEach((path) => {
      const content = source(path)
      expect(openingTags(content, 'caption')).toHaveLength(openingTags(content, 'table').length)
    })

    ;[...directTableFiles, ...tableShellFiles].forEach((path) => {
      openingTags(source(path), 'th').forEach((tag) => {
        expect(tag, `${path}: ${tag}`).toContain('scope="col"')
      })
    })
  })
})
