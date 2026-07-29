import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

const workflowFiles = [
  'src/views/admin/AdminDocumentListView.vue',
  'src/views/admin/AdminDocumentDetailView.vue',
  'src/views/admin/AdminKnowledgeRouteView.vue',
  'src/components/admin/DocumentTaskHistoryDialog.vue'
]

describe('F06 workflow UI contracts', () => {
  it('uses centered child-page dialogs for every workflow detail layer', () => {
    const detail = source(workflowFiles[1])
    const route = source(workflowFiles[2])
    const taskHistory = source(workflowFiles[3])
    expect(detail.match(/<ChildPageDialog/g)?.length).toBeGreaterThanOrEqual(2)
    expect(route).toContain('<ChildPageDialog')
    expect(taskHistory).toContain('<ChildPageDialog')
  })

  it('contains no Sheet or right-slide detail pattern in the F06 family', () => {
    const content = workflowFiles.map(source).join('\n')
    expect(content).not.toMatch(/<Sheet|SheetContent|slide-in-from-right|translate-x-full/)
    expect(content).not.toContain('bg-[rgba(')
  })

  it('keeps the F06 route views behind tested workflow boundaries', () => {
    expect(source(workflowFiles[0])).toContain("from '@/features/admin/documentWorkflow'")
    expect(source(workflowFiles[1])).toContain("from '@/features/admin/documentWorkflow'")
    expect(source(workflowFiles[2])).toContain("from '@/features/admin/knowledgeRouteWorkflow'")
    expect(source(workflowFiles[1])).toContain('DocumentTaskHistoryDialog')
  })

  it('keeps chunk browsing server-paged and detail on demand', () => {
    const detail = source(workflowFiles[1])
    expect(detail).toContain('queryDocumentChunks')
    expect(detail).toContain('pageSize: chunkPageSize.value')
    expect(detail).toContain('queryDocumentChunkDetail')
  })

  it('keeps build progress polling connected to the task log endpoint', () => {
    const detail = source(workflowFiles[1])
    expect(detail).toContain('async function loadBuildTaskLogs()')
    expect(detail).toContain('taskId: buildTaskId')
    expect(detail).toContain('buildPollTimer.value = window.setInterval')
    expect(detail).toContain('await loadAll()')
    expect(detail).not.toMatch(/async function loadBuildTaskLogs\(\)\s*\{\s*return null\s*\}/)
    expect(detail).not.toMatch(/function startBuildPolling\(\)\s*\{\s*clearBuildPolling\(\)\s*\}/)
  })

  it('keeps text buttons on the admin radius contract', () => {
    const route = source(workflowFiles[2])
    expect(route).not.toMatch(/<Button[^>]*rounded-full/)
  })

  it('keeps execution colors semantic without changing parent and child category colors', () => {
    const detail = source(workflowFiles[1])
    const tokens = source('src/assets/tailwind.css')
    const executionSection = detail.match(/data-workbench-section="execution"([\s\S]*?)data-workbench-section="chunk"/)?.[1] || ''

    expect(executionSection).not.toContain('blue-500')
    expect(executionSection).toContain('confirmStepVisual')
    expect(executionSection).toContain('buildStepVisual')
    // Parent/child classification is centralised: consumed via scoped tone classes + --pipeline-*
    // backing tokens, never branched as raw blue-500/amber-600 utilities in the template.
    expect(detail).toContain("pipeline.key === 'parent' ? 'pipeline-tone-parent' : 'pipeline-tone-child'")
    expect(detail).not.toContain('blue-500')
    expect(detail).not.toContain('amber-600')
    expect(tokens).toContain('--pipeline-parent-solid')
    expect(tokens).toContain('--pipeline-child-solid')
  })
})
