import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

function source(path) {
  return readFileSync(resolve(process.cwd(), path), 'utf8')
}

const routeFiles = {
  documents: 'src/views/admin/AdminDocumentListView.vue',
  observability: 'src/views/admin/AdminObservabilityListView.vue'
}

describe('F05 admin shell contracts', () => {
  it('uses a skip link, grouped navigation, and a left mobile Drawer', () => {
    const layout = source('src/views/admin/AdminLayoutView.vue')
    expect(layout).toContain('href="#admin-main-content"')
    expect(layout).toContain('id="admin-main-content"')
    expect(layout).toContain('direction="left"')
    expect(layout).toContain('知识资产')
    expect(layout).toContain('路由与观测')
    expect(layout).not.toContain('sidebar-mask')
  })

  it('keeps the login compact, empty by default, and exposes password visibility', () => {
    const login = source('src/views/AdminLoginView.vue')
    expect(login).toContain('showPassword')
    expect(login).toContain(':loading="submitting"')
    expect(login).not.toContain("username: 'admin'")
    expect(login).not.toContain("password: 'admin123456'")
  })
})

describe('F05 dashboard and list template contracts', () => {
  it('uses the shared dashboard shell for the ordinary document overview', () => {
    const dashboard = source('src/views/admin/AdminDashboardView.vue')
    expect(dashboard).toContain('PageHeader')
    expect(dashboard).toContain('AsyncState')
    expect(dashboard).toContain('建议演示路径')
    expect(dashboard).toContain('最近接入文档')
    expect(dashboard).not.toContain('queryKnowledgeRouteTracePage')
    expect(dashboard).not.toContain('SCRIPT_PLACEHOLDER')
  })

  it.each(Object.entries(routeFiles))('%s uses the shared list shell and a mobile priority view', (name, file) => {
    const content = source(file)
    expect(content).toContain('PageHeader')
    expect(content).toContain('FilterToolbar')
    expect(content).toContain('DataTableShell')
    expect(content).toContain('AsyncState')
    expect(content).toContain('StatusBadge')
    expect(content).toContain('md:hidden')
  })

  it('removes observability status side stripes and keeps details out of Sheet', () => {
    const observability = source(routeFiles.observability)
    const allF05 = Object.values(routeFiles).map(source).join('\n')
    expect(observability).not.toContain('border-l-4')
    expect(allF05).not.toMatch(/<Sheet|SheetContent|slide-in-from-right/)
  })
})
